package vermilingua.packaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests on {@link Util} class.
 */
public class UtilTest {

	@Test
	public void writeStringToPathInJarCreatesMissingParentFolder( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );

		Util.writeStringToPathInJar( "plist", "Resources/Info.plist", jarPath );

		assertEquals( "plist", TestJars.stringFromJar( jarPath, "Resources/Info.plist" ) );
	}

	@Test
	public void writeStringToPathInJarCreatesMissingNestedParentFolders( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );

		Util.writeStringToPathInJar( "deep", "a/b/c/deep.txt", jarPath );

		assertEquals( "deep", TestJars.stringFromJar( jarPath, "a/b/c/deep.txt" ) );
	}

	@Test
	public void writeStringToPathInJarWritesToJarRoot( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );

		Util.writeStringToPathInJar( "hello", "hello.txt", jarPath );

		assertEquals( "hello", TestJars.stringFromJar( jarPath, "hello.txt" ) );
	}

	@Test
	public void writeStringToPathInJarOverwritesExistingEntry( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );

		Util.writeStringToPathInJar( "first", "Resources/Info.plist", jarPath );
		Util.writeStringToPathInJar( "second", "Resources/Info.plist", jarPath );

		assertEquals( "second", TestJars.stringFromJar( jarPath, "Resources/Info.plist" ) );
	}

	@Test
	public void writeStringToPathInJarLeavesExistingEntriesAlone( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );

		Util.writeStringToPathInJar( "plist", "Resources/Info.plist", jarPath );

		assertTrue( TestJars.entryNames( jarPath ).contains( TestJars.CLASS_ENTRY ) );
	}

	@Test
	public void copyFolderToJarKeepsHierarchy( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );

		final Path sourceFolder = directory.resolve( "source" );
		TestJars.file( sourceFolder.resolve( "top.txt" ), "top" );
		TestJars.file( sourceFolder.resolve( "nested/deep.txt" ), "deep" );
		TestJars.file( sourceFolder.resolve( "nested/deeper/deepest.txt" ), "deepest" );

		Util.copyContentsOfFolderAtPathToFolderInJar( sourceFolder, "Resources", jarPath );

		assertEquals( "top", TestJars.stringFromJar( jarPath, "Resources/top.txt" ) );
		assertEquals( "deep", TestJars.stringFromJar( jarPath, "Resources/nested/deep.txt" ) );
		assertEquals( "deepest", TestJars.stringFromJar( jarPath, "Resources/nested/deeper/deepest.txt" ) );
	}

	/**
	 * Every copied file must end up as a file entry. Guards against leaving directory entries named like files behind
	 */
	@Test
	public void copyFolderToJarProducesOnlyFileEntriesForFiles( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );

		final Path sourceFolder = directory.resolve( "source" );
		TestJars.file( sourceFolder.resolve( "top.txt" ), "top" );
		TestJars.file( sourceFolder.resolve( "nested/deep.txt" ), "deep" );

		Util.copyContentsOfFolderAtPathToFolderInJar( sourceFolder, "Resources", jarPath );

		final var names = TestJars.entryNames( jarPath );
		assertFalse( names.contains( "Resources/top.txt/" ), names.toString() );
		assertFalse( names.contains( "Resources/nested/deep.txt/" ), names.toString() );
	}

	@Test
	public void copyFolderToJarOverwritesExistingEntries( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );

		final Path sourceFolder = directory.resolve( "source" );
		TestJars.file( sourceFolder.resolve( "nested/file.txt" ), "first" );
		Util.copyContentsOfFolderAtPathToFolderInJar( sourceFolder, "Resources", jarPath );

		TestJars.file( sourceFolder.resolve( "nested/file.txt" ), "second" );
		Util.copyContentsOfFolderAtPathToFolderInJar( sourceFolder, "Resources", jarPath );

		assertEquals( "second", TestJars.stringFromJar( jarPath, "Resources/nested/file.txt" ) );
	}

	@Test
	public void copyFolderToJarHandlesSpacesAndNonAsciiNames( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );

		final Path sourceFolder = directory.resolve( "source" );
		TestJars.file( sourceFolder.resolve( "Þjónusta skrá.txt" ), "íslenska" );
		TestJars.file( sourceFolder.resolve( "Möppur með bili/skrá.txt" ), "nested" );

		Util.copyContentsOfFolderAtPathToFolderInJar( sourceFolder, "Resources", jarPath );

		assertEquals( "íslenska", TestJars.stringFromJar( jarPath, "Resources/Þjónusta skrá.txt" ) );
		assertEquals( "nested", TestJars.stringFromJar( jarPath, "Resources/Möppur með bili/skrá.txt" ) );
	}

	@Test
	public void copyFolderToJarDoesNothingIfSourceIsMissing( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );
		final var namesBefore = TestJars.entryNames( jarPath );

		Util.copyContentsOfFolderAtPathToFolderInJar( directory.resolve( "does-not-exist" ), "Resources", jarPath );

		assertEquals( namesBefore, TestJars.entryNames( jarPath ) );
	}

	/**
	 * An empty source folder creates nothing in the jar, not even the destination folder.
	 * This is what a framework without resources looks like, so nothing may depend on the destination folder existing afterwards.
	 */
	@Test
	public void copyFolderToJarCreatesNothingForEmptySource( @TempDir final Path directory ) throws IOException {
		final Path jarPath = TestJars.jarWithClassesOnly( directory );
		final var namesBefore = TestJars.entryNames( jarPath );

		Util.copyContentsOfFolderAtPathToFolderInJar( Files.createDirectories( directory.resolve( "empty" ) ), "Resources", jarPath );

		assertEquals( namesBefore, TestJars.entryNames( jarPath ) );
	}

	private static void flatten( final Path source, final Path destination ) {
		Util.copyContentsOfDirectoryToDirectoryFlatten( source, Util.folder( destination ), List.of( "wo" ), List.of( "lproj" ) );
	}

	@Test
	public void flattenCopiesBundlesWholeAndFlattensLooseFiles( @TempDir final Path directory ) throws IOException {
		final Path source = directory.resolve( "components" );
		TestJars.file( source.resolve( "a/Foo.wo/Foo.html" ), "foo" );
		TestJars.file( source.resolve( "b/c/Bar.api" ), "bar" );
		TestJars.file( source.resolve( "d/en.lproj/e/Baz.wo/Baz.html" ), "baz" );

		final Path destination = directory.resolve( "out" );
		flatten( source, destination );

		assertEquals( "foo", Files.readString( destination.resolve( "Foo.wo/Foo.html" ) ) );
		assertEquals( "bar", Files.readString( destination.resolve( "Bar.api" ) ) );
		assertEquals( "baz", Files.readString( destination.resolve( "en.lproj/Baz.wo/Baz.html" ) ) );
	}

	@Test
	public void flattenFailsOnDuplicateBundles( @TempDir final Path directory ) throws IOException {
		final Path source = directory.resolve( "components" );
		TestJars.file( source.resolve( "a/Foo.wo/Foo.html" ), "first" );
		TestJars.file( source.resolve( "b/Foo.wo/Foo.html" ), "second" );

		final var e = assertThrows( IllegalStateException.class, () -> flatten( source, directory.resolve( "out" ) ) );
		assertTrue( e.getMessage().contains( "Foo.wo" ), e.getMessage() );
	}

	@Test
	public void flattenFailsOnDuplicateLooseFiles( @TempDir final Path directory ) throws IOException {
		final Path source = directory.resolve( "components" );
		TestJars.file( source.resolve( "Foo.api" ), "first" );
		TestJars.file( source.resolve( "a/Foo.api" ), "second" );

		assertThrows( IllegalStateException.class, () -> flatten( source, directory.resolve( "out" ) ) );
	}

	/**
	 * Two `en.lproj` folders in different subfolders flatten into the same destination, so their contents can collide too
	 */
	@Test
	public void flattenFailsOnDuplicatesInSeparateLocalizationFolders( @TempDir final Path directory ) throws IOException {
		final Path source = directory.resolve( "components" );
		TestJars.file( source.resolve( "a/en.lproj/Foo.wo/Foo.html" ), "first" );
		TestJars.file( source.resolve( "b/en.lproj/Foo.wo/Foo.html" ), "second" );

		assertThrows( IllegalStateException.class, () -> flatten( source, directory.resolve( "out" ) ) );
	}

	@Test
	public void flattenMergesSeparateLocalizationFolders( @TempDir final Path directory ) throws IOException {
		final Path source = directory.resolve( "components" );
		TestJars.file( source.resolve( "a/en.lproj/Foo.wo/Foo.html" ), "foo" );
		TestJars.file( source.resolve( "b/en.lproj/Bar.wo/Bar.html" ), "bar" );

		final Path destination = directory.resolve( "out" );
		flatten( source, destination );

		assertEquals( "foo", Files.readString( destination.resolve( "en.lproj/Foo.wo/Foo.html" ) ) );
		assertEquals( "bar", Files.readString( destination.resolve( "en.lproj/Bar.wo/Bar.html" ) ) );
	}

	@Test
	public void flattenSkipsHiddenFilesAndFolders( @TempDir final Path directory ) throws IOException {
		final Path source = directory.resolve( "components" );
		TestJars.file( source.resolve( ".DS_Store" ), "junk" );
		TestJars.file( source.resolve( "a/.DS_Store" ), "junk" );
		TestJars.file( source.resolve( "a/.gitignore" ), "junk" );
		TestJars.file( source.resolve( "a/.svn/entries" ), "junk" );
		TestJars.file( source.resolve( "b/.svn/entries" ), "junk" );
		TestJars.file( source.resolve( "a/Foo.wo/Foo.html" ), "foo" );

		final Path destination = directory.resolve( "out" );
		flatten( source, destination );

		try( var files = Files.list( destination ) ) {
			assertEquals( List.of( "Foo.wo" ), files.map( p -> p.getFileName().toString() ).toList() );
		}
	}

	/**
	 * Files left at the destination by a previous build aren't duplicates
	 */
	@Test
	public void flattenOverwritesPreviousBuild( @TempDir final Path directory ) throws IOException {
		final Path source = directory.resolve( "components" );
		final Path destination = directory.resolve( "out" );

		TestJars.file( source.resolve( "a/Foo.wo/Foo.html" ), "first" );
		TestJars.file( source.resolve( "a/Foo.api" ), "first" );
		flatten( source, destination );

		TestJars.file( source.resolve( "a/Foo.wo/Foo.html" ), "second" );
		TestJars.file( source.resolve( "a/Foo.api" ), "second" );
		flatten( source, destination );

		assertEquals( "second", Files.readString( destination.resolve( "Foo.wo/Foo.html" ) ) );
		assertEquals( "second", Files.readString( destination.resolve( "Foo.api" ) ) );
	}
}
