package vermilingua.packaging;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Helpers for tests that read and write jar files
 */
class TestJars {

	static final String CLASS_ENTRY = "some/pkg/SomeClass.class";

	/**
	 * @return Path to a new jar containing classes only, like the one maven-jar-plugin builds for a framework that has no resources or components
	 */
	static Path jarWithClassesOnly( final Path directory ) throws IOException {
		final Path jarPath = directory.resolve( "framework.jar" );

		try( OutputStream out = Files.newOutputStream( jarPath ); JarOutputStream jar = new JarOutputStream( out )) {
			jar.putNextEntry( new JarEntry( CLASS_ENTRY ) );
			jar.write( new byte[] { 1, 2, 3 } );
			jar.closeEntry();
		}

		return jarPath;
	}

	/**
	 * Writes [content] to a file at [path], creating parent folders as needed
	 */
	static void file( final Path path, final String content ) throws IOException {
		Files.createDirectories( path.getParent() );
		Files.writeString( path, content, StandardCharsets.UTF_8 );
	}

	static String stringFromJar( final Path jarPath, final String entryName ) throws IOException {
		try( JarFile jar = new JarFile( jarPath.toFile() )) {
			final JarEntry entry = jar.getJarEntry( entryName );

			if( entry == null ) {
				throw new AssertionError( "No entry named '%s' in jar. Entries: %s".formatted( entryName, entryNames( jarPath ) ) );
			}

			return new String( jar.getInputStream( entry ).readAllBytes(), StandardCharsets.UTF_8 );
		}
	}

	/**
	 * @return Sorted names of every entry in the jar, folders included (folder names end with a slash)
	 */
	static List<String> entryNames( final Path jarPath ) throws IOException {
		try( JarFile jar = new JarFile( jarPath.toFile() )) {
			final List<String> names = Collections.list( jar.entries() ).stream().map( JarEntry::getName ).sorted().toList();
			return names;
		}
	}
}
