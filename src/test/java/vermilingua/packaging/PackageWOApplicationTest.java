package vermilingua.packaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import vermilingua.packaging.SourceProject.Type;

/**
 * Tests on {@link PackageWOApplication}, in particular the order of jars in classpath.txt
 */
public class PackageWOApplicationTest {

	private static final List<String> MAVEN_ORDER = List.of(
			"APPROOT/Resources/Java/",
			"APPROOT/Resources/Java/testapp.jar",
			"APPROOT/Resources/Java/wonder/core/ERFoundation/1.0/ERFoundation-1.0.jar",
			"APPROOT/Resources/Java/com/webobjects/JavaFoundation/5.4.3/JavaFoundation-5.4.3.jar",
			"APPROOT/Resources/Java/com/google/guava/guava/33.5.0-jre/guava-33.5.0-jre.jar",
			"APPROOT/Resources/Java/com/webobjects/JavaWebObjects/5.4.3/JavaWebObjects-5.4.3.jar",
			"APPROOT/Resources/Java/wonder/core/ERWebObjects/1.0/ERWebObjects-1.0.jar",
			"APPROOT/Resources/Java/com/webobjects/other/NotAnAppleFramework/1.0/NotAnAppleFramework-1.0.jar" );

	private static final List<String> PATCHED_ORDER = List.of(
			"APPROOT/Resources/Java/",
			"APPROOT/Resources/Java/testapp.jar",
			"APPROOT/Resources/Java/wonder/core/ERFoundation/1.0/ERFoundation-1.0.jar",
			"APPROOT/Resources/Java/com/google/guava/guava/33.5.0-jre/guava-33.5.0-jre.jar",
			"APPROOT/Resources/Java/wonder/core/ERWebObjects/1.0/ERWebObjects-1.0.jar",
			"APPROOT/Resources/Java/com/webobjects/other/NotAnAppleFramework/1.0/NotAnAppleFramework-1.0.jar",
			"APPROOT/Resources/Java/com/webobjects/JavaFoundation/5.4.3/JavaFoundation-5.4.3.jar",
			"APPROOT/Resources/Java/com/webobjects/JavaWebObjects/5.4.3/JavaWebObjects-5.4.3.jar" );

	/**
	 * JavaFoundation and JavaWebObjects sit in the middle of Maven's order, ahead of a later Wonder jar.
	 * By default, classpath.txt keeps Maven's order.
	 */
	@Test
	public void appleFrameworksStayInMavenOrderByDefault( @TempDir final Path directory ) throws IOException {
		assertEquals( MAVEN_ORDER, classpath( directory, false ) );
	}

	/**
	 * With patching, JavaFoundation and JavaWebObjects move to the end and keep their order relative to each other.
	 * A group id that only starts with com.webobjects stays where Maven put it.
	 */
	@Test
	public void appleFrameworksMoveToTheEndWhenPatchingIsAllowed( @TempDir final Path directory ) throws IOException {
		assertEquals( PATCHED_ORDER, classpath( directory, true ) );
	}

	/**
	 * A component removed from the sources since the last build doesn't stay in the bundle, even without mvn clean
	 */
	@Test
	public void rebuildStartsFromAnEmptyBundle( @TempDir final Path directory ) throws IOException {
		classpath( directory, false );

		final Path woa = directory.resolve( "target/TestApp.woa" );
		final Path stale = woa.resolve( "Contents/Resources/Removed.wo/Removed.html" );
		TestJars.file( stale, "removed" );

		classpath( directory, false );

		assertFalse( Files.exists( stale.getParent() ) );
		assertTrue( Files.isRegularFile( woa.resolve( "classpath.txt" ) ) );
	}

	/**
	 * The bundle is deleted before every build, so a finalName that points it out of the target folder is refused
	 */
	@Test
	public void bundleOutsideTheTargetFolderIsRefused( @TempDir final Path directory ) throws IOException {
		final Path outside = directory.resolve( "Outside.woa" );
		TestJars.file( outside.resolve( "keep.txt" ), "keep" );

		final SourceProject project = project( directory );
		assertThrows( IllegalArgumentException.class, () -> new PackageWOApplication().execute( project, "../Outside", directory.resolve( "target" ), false ) );
		assertTrue( Files.isRegularFile( outside.resolve( "keep.txt" ) ) );
	}

	/**
	 * @return The lines of classpath.txt in a WOA built from a test project
	 */
	private static List<String> classpath( final Path directory, final boolean allowWebObjectsPatching ) throws IOException {
		final PackageWOApplication.WOA woa = new PackageWOApplication().execute( project( directory ), "TestApp", directory.resolve( "target" ), allowWebObjectsPatching );
		return Files.readAllLines( woa.woaPath().resolve( "classpath.txt" ) );
	}

	/**
	 * @return A test application project in [directory], with dependencies in the order the classpath tests expect from Maven
	 */
	private static SourceProject project( final Path directory ) throws IOException {
		Files.createDirectories( directory );
		TestJars.file( directory.resolve( "build.properties" ), "principalClass=test.Application\n" );

		final Path templateDir = directory.resolve( "template" );
		Files.createDirectories( templateDir );
		final Path template = TestJars.jarWithClassesOnly( templateDir );
		final Path appJar = copyJar( directory, template, "app" );

		return new SourceProject(
				Type.Application,
				"TestApp",
				"1.0",
				directory.resolve( "src/main/woresources" ),
				directory.resolve( "src/main/components" ),
				directory.resolve( "src/main/webserver-resources" ),
				appJar,
				"test.Application",
				List.of(
						dependency( directory, template, "wonder.core", "ERFoundation", "1.0" ),
						dependency( directory, template, "com.webobjects", "JavaFoundation", "5.4.3" ),
						dependency( directory, template, "com.google.guava", "guava", "33.5.0-jre" ),
						dependency( directory, template, "com.webobjects", "JavaWebObjects", "5.4.3" ),
						dependency( directory, template, "wonder.core", "ERWebObjects", "1.0" ),
						dependency( directory, template, "com.webobjects.other", "NotAnAppleFramework", "1.0" ) ),
				BuildProperties.of( directory ) );
	}

	private static Dependency dependency( final Path directory, final Path template, final String groupId, final String artifactId, final String version ) throws IOException {
		final Path jar = copyJar( directory, template, artifactId + "-" + version );
		return new Dependency( groupId, artifactId, version, jar.toFile() );
	}

	private static Path copyJar( final Path directory, final Path template, final String filename ) throws IOException {
		final Path jar = directory.resolve( "jars/" + filename + ".jar" );
		Files.createDirectories( jar.getParent() );
		Files.copy( template, jar, StandardCopyOption.REPLACE_EXISTING );
		return jar;
	}
}
