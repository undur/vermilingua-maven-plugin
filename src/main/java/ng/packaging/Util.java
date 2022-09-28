package ng.packaging;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Util {

	/**
	 * Copy the file at [sourcePath] to a new file specified by [destinationPath]
	 * This method exists solely to wrap the checked IOException in an UncheckedIOException
	 */
	public static void copyFile( final Path sourcePath, final Path destinationPath ) {
		Objects.requireNonNull( sourcePath );
		Objects.requireNonNull( destinationPath );

		try {
			Files.copy( sourcePath, destinationPath );
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * Copy the contents of the directory specified by [sourceDirectory] into the directory specified by [destinationDirectory]
	 *
	 * FIXME: Specify replace/overwrite/failure conditions // Hugi 2021-07-16
	 */
	public static void copyContentsOfDirectoryToDirectory( final Path sourceDirectory, final Path destinationDirectory ) {
		Objects.requireNonNull( sourceDirectory );
		Objects.requireNonNull( destinationDirectory );

		// FIXME: Remove that infernal string munging // Hugi 2021-07-08
		final String sourceDirectoryLocationString = sourceDirectory.toString();
		final String destinationDirectoryLocationString = destinationDirectory.toString();

		try {
			Files.walk( Path.of( sourceDirectoryLocationString ) )
					.forEach( sourcePath -> {
						final Path destinationPath = Path.of( destinationDirectoryLocationString, sourcePath.toString().substring( sourceDirectoryLocationString.length() ) );

						if( !Files.exists( destinationPath ) ) { // FIXME: This is just a hackyhack // Hugi 2021-07-08
							copyFile( sourcePath, destinationPath );
						}
					} );
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * Writes [string] to a file specified by [path]
	 */
	public static void writeStringToPath( final String string, final Path path ) {
		Objects.requireNonNull( string );
		Objects.requireNonNull( path );

		try {
			Files.write( path, string.getBytes( StandardCharsets.UTF_8 ) );
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	public static String readTemplate( final String name ) {
		Objects.requireNonNull( name );

		try( InputStream stream = PackageWOApplication.class.getResourceAsStream( "/templates/" + name + ".template.txt" )) {
			return new String( stream.readAllBytes(), StandardCharsets.UTF_8 );
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	public static void makeUserExecutable( final Path path ) {
		Objects.requireNonNull( path );

		try {
			Files.setPosixFilePermissions( path, PosixFilePermissions.fromString( "rwxr--r--" ) );
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * @return true if [sourceJarFile] contains a non-empty WebServerResources-directory in it's root
	 */
	public static boolean jarContainsNonEmptyWebServerResourcesDirectoryInRoot( final File sourceJarFile ) {
		Objects.requireNonNull( sourceJarFile );

		try( final JarFile jarFile = new JarFile( sourceJarFile )) {
			final Enumeration<JarEntry> entries = jarFile.entries();

			int i = 0;

			while( entries.hasMoreElements() ) {
				final JarEntry entry = entries.nextElement();

				if( entry.getName().startsWith( "WebServerResources/" ) ) {
					i++;

					if( i > 1 ) {
						return true;
					}
				}
			}

			return false;
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * Copies the folder specified by folderName to destinationPath.
	 * Copies the entire folder, not just the content (as in, you will end up with [destinationPath]/[folderName]/...
	 *
	 * FIXME: Don't copy the folder if it's empty // Hugi 2021-08-07
	 */
	public static void copyFolderFromJarToPath( final String folderName, final Path sourceJarPath, final Path destinationPath ) {
		Objects.requireNonNull( folderName );
		Objects.requireNonNull( sourceJarPath );
		Objects.requireNonNull( destinationPath );

		try( final JarFile jarFile = new JarFile( sourceJarPath.toFile() )) {
			final Enumeration<JarEntry> entries = jarFile.entries();

			while( entries.hasMoreElements() ) {
				final JarEntry entry = entries.nextElement();

				if( entry.getName().startsWith( folderName + "/" ) ) {
					final Path targetPath = destinationPath.resolve( entry.getName() );

					if( entry.isDirectory() ) {
						Files.createDirectories( targetPath );
					}
					else {
						try( final InputStream inStream = jarFile.getInputStream( entry )) {
							Files.copy( inStream, targetPath );
						}
					}
				}
			}
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * Writes the contents of the folder specified by [sourcePath] into a folder named [folder] in the root of  [destinationJarPath]
	 *
	 * - Does nothing if the folder specified by sourcePath does not exist
	 * - Creates the destination folder if missing.
	 *
	 * FIXME: This method currently silently overwrites existing files in the jar. We probably want to be more intelligent about that // Hugi 2022-09-28
	 */
	public static void copyContentsOfFolderAtPathToFolderInJar( final Path sourcePath, final String folderName, final Path destinationJarPath ) {
		Objects.requireNonNull( sourcePath );
		Objects.requireNonNull( folderName );
		Objects.requireNonNull( destinationJarPath );

		if( Files.exists( sourcePath ) ) {
			final URI uri = URI.create( "jar:file:" + destinationJarPath.toString() );

			try( FileSystem zipfs = FileSystems.newFileSystem( uri, Collections.emptyMap() )) {
				Files.walk( sourcePath ).forEach( folderEntry -> {

					try {
						if( !Files.isDirectory( folderEntry ) ) {
							final Path relativePath = sourcePath.relativize( folderEntry );
							final Path pathInZipFile = zipfs.getPath( folderName + "/" + relativePath.toString() ); // FIXME: This is what I hate, all this string munging // Hugi 2021-07-10
							Files.createDirectories( pathInZipFile );
							Files.copy( folderEntry, pathInZipFile, StandardCopyOption.REPLACE_EXISTING );
						}
					}
					catch( final IOException e ) {
						throw new UncheckedIOException( e );
					}
				} );
			}
			catch( final IOException e ) {
				throw new UncheckedIOException( e );
			}
		}
	}

	/**
	 * Writes the contents of the folder specified by [sourcePath] into a folder named [folder] in the root of  [destinationJarPath]
	 * Creates the folder in question if missing.
	 *
	 * FIXME: Should this overwrite existing files silently or fail on overwrite? // Hugi 2021-07-14
	 * FIXME: Don't accept a string for the destination parameter, use a path if possible. // Hugi 2021-07-14
	 */
	public static void writeStringToPathInJar( final String string, final String destinationFilePathInsideJar, final Path destinationJarPath ) {
		Objects.requireNonNull( string );
		Objects.requireNonNull( destinationFilePathInsideJar );
		Objects.requireNonNull( destinationJarPath );

		final URI uri = URI.create( "jar:file:" + destinationJarPath.toString() );

		try( FileSystem zipfs = FileSystems.newFileSystem( uri, Collections.emptyMap() )) {
			final Path pathInZipFile = zipfs.getPath( destinationFilePathInsideJar );
			//			Files.createDirectories( pathInZipFile ); // FIXME: We probably need to keep this, in case there are no resources copied beforehand
			Files.writeString( pathInZipFile, string, StandardCharsets.UTF_8 );
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * @return The folder at the given path. Creates the folder if missing, throws an exception if the path exists but is not a folder.
	 */
	public static Path folder( final Path path ) {
		Objects.requireNonNull( path );

		if( Files.exists( path ) ) {
			if( !Files.isDirectory( path ) ) {
				throw new IllegalArgumentException( "Given folder path exists but is not a folder" );
			}
		}
		else {
			try {
				Files.createDirectories( path );
			}
			catch( final IOException e ) {
				throw new UncheckedIOException( e );
			}
		}

		return path;
	}
}