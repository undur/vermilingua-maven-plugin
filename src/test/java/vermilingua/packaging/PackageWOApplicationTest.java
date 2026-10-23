package vermilingua.packaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

	@Test
	public void appleFrameworksStayInMavenOrderByDefault( @TempDir final Path directory ) throws IOException {
		assertEquals( MAVEN_ORDER, classpath( directory, false ) );
	}

	@Test
	public void appleFrameworksMoveToTheEndWhenPatchingIsAllowed( @TempDir final Path directory ) throws IOException {
		assertEquals( PATCHED_ORDER, classpath( directory, true ) );
	}

	/**
	 * JavaFoundation and JavaWebObjects sit in the middle of Maven's order, ahead of a later Wonder jar.
	 * With patching they move to the end and keep that order relative to each other. A group id that only
	 * starts with com.webobjects stays where Maven put it.
	 */
	private static List<String> classpath( final Path directory, final boolean allowWebObjectsPatching ) throws IOException {
		Files.createDirectories( directory );
		TestJars.file( directory.resolve( "build.properties" ), "principalClass=test.Application\n" );

		final Path templateDir = directory.resolve( "template" );
		Files.createDirectories( templateDir );
		final Path template = TestJars.jarWithClassesOnly( templateDir );
		final Path appJar = copyJar( directory, template, "app" );

		final SourceProject project = new SourceProject(
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

		final PackageWOApplication packager = new PackageWOApplication();
		final PackageWOApplication.WOA woa = allowWebObjectsPatching
				? packager.execute( project, "TestApp", directory.resolve( "target" ), true )
				: packager.execute( project, "TestApp", directory.resolve( "target" ) );

		return Files.readAllLines( woa.woaPath().resolve( "classpath.txt" ) );
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
