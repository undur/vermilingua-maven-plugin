package vermilingua.packaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import vermilingua.packaging.SourceProject.Type;

/**
 * Tests on {@link PackageWOFramework}, packaging frameworks into a jar the way maven-jar-plugin leaves it (classes only)
 */
public class PackageWOFrameworkTest {

	private static final String PRINCIPAL_CLASS = "some.pkg.Principal";

	private static SourceProject framework( final Path projectDirectory, final Path jarPath, final String principalClassName ) {
		return new SourceProject(
				Type.Framework,
				"TestFramework",
				"1.2.3",
				projectDirectory.resolve( "src/main/woresources" ),
				projectDirectory.resolve( "src/main/components" ),
				projectDirectory.resolve( "src/main/webserver-resources" ),
				jarPath,
				principalClassName,
				List.of(),
				null );
	}

	@Test
	public void frameworkWithAllKindsOfResources( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );
		final Path main = directory.resolve( "src/main" );

		TestJars.file( main.resolve( "woresources/Properties" ), "properties" );
		TestJars.file( main.resolve( "woresources/nested/data.txt" ), "data" );
		TestJars.file( main.resolve( "woresources/English.lproj/Localizable.strings" ), "strings" );

		TestJars.file( main.resolve( "components/Main.wo/Main.html" ), "main-html" );
		TestJars.file( main.resolve( "components/Main.wo/Main.wod" ), "main-wod" );
		TestJars.file( main.resolve( "components/Main.api" ), "main-api" );
		TestJars.file( main.resolve( "components/subfolder/Nested.wo/Nested.html" ), "nested-html" );
		TestJars.file( main.resolve( "components/English.lproj/subfolder/Localized.wo/Localized.html" ), "localized-html" );

		TestJars.file( main.resolve( "webserver-resources/style.css" ), "css" );
		TestJars.file( main.resolve( "webserver-resources/img/logo.png" ), "png" );

		new PackageWOFramework().execute( framework( directory, jarPath, PRINCIPAL_CLASS ) );

		// woresources keep their hierarchy
		assertEquals( "properties", TestJars.stringFromJar( jarPath, "Resources/Properties" ) );
		assertEquals( "data", TestJars.stringFromJar( jarPath, "Resources/nested/data.txt" ) );
		assertEquals( "strings", TestJars.stringFromJar( jarPath, "Resources/English.lproj/Localizable.strings" ) );

		// Components get flattened, with .wo bundles kept intact and .lproj folders preserved (and flattened into)
		assertEquals( "main-html", TestJars.stringFromJar( jarPath, "Resources/Main.wo/Main.html" ) );
		assertEquals( "main-wod", TestJars.stringFromJar( jarPath, "Resources/Main.wo/Main.wod" ) );
		assertEquals( "main-api", TestJars.stringFromJar( jarPath, "Resources/Main.api" ) );
		assertEquals( "nested-html", TestJars.stringFromJar( jarPath, "Resources/Nested.wo/Nested.html" ) );
		assertEquals( "localized-html", TestJars.stringFromJar( jarPath, "Resources/English.lproj/Localized.wo/Localized.html" ) );

		// Webserver resources keep their hierarchy
		assertEquals( "css", TestJars.stringFromJar( jarPath, "WebServerResources/style.css" ) );
		assertEquals( "png", TestJars.stringFromJar( jarPath, "WebServerResources/img/logo.png" ) );

		final String infoPlist = TestJars.stringFromJar( jarPath, "Resources/Info.plist" );
		assertTrue( infoPlist.contains( "<string>TestFramework</string>" ), infoPlist );
		assertTrue( infoPlist.contains( "<string>" + PRINCIPAL_CLASS + "</string>" ), infoPlist );

		// The classes maven-jar-plugin put in the jar are still there
		assertTrue( TestJars.entryNames( jarPath ).contains( TestJars.CLASS_ENTRY ) );
	}

	/**
	 * A framework consisting of java code only (no woresources, components or webserver-resources folders at all) must still get it's Info.plist
	 */
	@Test
	public void frameworkWithoutAnyResourceFolders( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );

		new PackageWOFramework().execute( framework( directory, jarPath, PRINCIPAL_CLASS ) );

		final String infoPlist = TestJars.stringFromJar( jarPath, "Resources/Info.plist" );
		assertTrue( infoPlist.contains( "<string>" + PRINCIPAL_CLASS + "</string>" ), infoPlist );

		final List<String> names = TestJars.entryNames( jarPath );
		assertTrue( names.contains( TestJars.CLASS_ENTRY ), names.toString() );
		assertFalse( names.stream().anyMatch( name -> name.startsWith( "WebServerResources" ) ), names.toString() );
	}

	@Test
	public void frameworkWithEmptyResourceFolders( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );
		final Path main = directory.resolve( "src/main" );

		java.nio.file.Files.createDirectories( main.resolve( "woresources" ) );
		java.nio.file.Files.createDirectories( main.resolve( "components" ) );
		java.nio.file.Files.createDirectories( main.resolve( "webserver-resources" ) );

		new PackageWOFramework().execute( framework( directory, jarPath, null ) );

		final String infoPlist = TestJars.stringFromJar( jarPath, "Resources/Info.plist" );
		assertFalse( infoPlist.contains( "NSPrincipalClass" ), infoPlist );
	}

	@Test
	public void frameworkWithWebserverResourcesOnly( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );

		TestJars.file( directory.resolve( "src/main/webserver-resources/style.css" ), "css" );

		new PackageWOFramework().execute( framework( directory, jarPath, null ) );

		assertEquals( "css", TestJars.stringFromJar( jarPath, "WebServerResources/style.css" ) );
		assertTrue( TestJars.entryNames( jarPath ).contains( "Resources/Info.plist" ) );
	}

	/**
	 * Packaging over a jar that's already been packaged happens on builds without 'clean', if maven-jar-plugin considers the jar up to date and leaves it alone
	 */
	@Test
	public void packagingTwiceIntoTheSameJar( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );
		final Path main = directory.resolve( "src/main" );

		TestJars.file( main.resolve( "woresources/nested/data.txt" ), "first" );
		TestJars.file( main.resolve( "components/Main.wo/Main.html" ), "first-html" );
		TestJars.file( main.resolve( "webserver-resources/img/logo.png" ), "first-png" );

		new PackageWOFramework().execute( framework( directory, jarPath, PRINCIPAL_CLASS ) );

		TestJars.file( main.resolve( "woresources/nested/data.txt" ), "second" );
		TestJars.file( main.resolve( "components/Main.wo/Main.html" ), "second-html" );
		TestJars.file( main.resolve( "webserver-resources/img/logo.png" ), "second-png" );

		new PackageWOFramework().execute( framework( directory, jarPath, PRINCIPAL_CLASS ) );

		assertEquals( "second", TestJars.stringFromJar( jarPath, "Resources/nested/data.txt" ) );
		assertEquals( "second-html", TestJars.stringFromJar( jarPath, "Resources/Main.wo/Main.html" ) );
		assertEquals( "second-png", TestJars.stringFromJar( jarPath, "WebServerResources/img/logo.png" ) );
	}
}
