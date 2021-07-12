package ng.packaging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Util {

	/**
	 * Copy the file at [sourcePath] to a new file specified by [destinationPath]
	 */
	public static void copyFile( final Path sourcePath, final Path destinationPath ) {
		Objects.requireNonNull( sourcePath );
		Objects.requireNonNull( destinationPath );

		try {
			Files.copy( sourcePath, destinationPath );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * FIXME: Change to accept Paths as parameters // Hugi 2021-07-08
	 */
	public static void copyContentsOfDirectoryToDirectory( String sourceDirectoryLocation, String destinationDirectoryLocation ) {
		Objects.requireNonNull( sourceDirectoryLocation );
		Objects.requireNonNull( destinationDirectoryLocation );

		try {
			Files.walk( Path.of( sourceDirectoryLocation ) )
					.forEach( source -> {
						final Path destination = Path.of( destinationDirectoryLocation, source.toString().substring( sourceDirectoryLocation.length() ) );
						try {
							if( !Files.exists( destination ) ) { // FIXME: This is just a hackyhack // Hugi 2021-07-08
								Files.copy( source, destination );
							}
						}
						catch( final IOException e ) {
							throw new RuntimeException( e );
						}
					} );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
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
			throw new RuntimeException( e );
		}
	}

	public static String readTemplate( final String name ) {
		Objects.requireNonNull( name );

		try( InputStream stream = PackageWOApplication.class.getResourceAsStream( "/templates/" + name + ".template.txt" )) {
			return new String( stream.readAllBytes(), StandardCharsets.UTF_8 );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}
	}

	public static void makeUserExecutable( final Path path ) {
		Objects.requireNonNull( path );

		try {
			final Set<PosixFilePermission> perms = new HashSet<>();
			perms.add( PosixFilePermission.OWNER_READ );
			perms.add( PosixFilePermission.OWNER_WRITE );
			perms.add( PosixFilePermission.OWNER_EXECUTE );
			perms.add( PosixFilePermission.GROUP_READ );
			perms.add( PosixFilePermission.OTHERS_READ );
			Files.setPosixFilePermissions( path, perms );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}
	}

	public static boolean containsWebServerResources( final File sourceJarFile ) {
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
			throw new RuntimeException( e );
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
					final File targetFile = destinationPath.resolve( entry.getName() ).toFile();

					if( entry.isDirectory() ) {
						targetFile.mkdirs();
					}
					else {
						final InputStream inStream = jarFile.getInputStream( entry );
						final OutputStream outStream = new FileOutputStream( targetFile );
						copy( inStream, outStream );
					}
				}
			}
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Writes the contents of the folder specified by [sourcePath] into a folder named [folder] in the root of  [destinationJarPath]
	 * Creates the folder in question if missing.
	 *
	 * FIXME: First implementation attempt. This is actually pretty horrid // Hugi 2021-07-10
	 * FIXME: Should this overwrite existing files silently or fail on overwrite?
	 */
	public static void copyContentsOfFolderAtPathToFolderInJar( final Path sourcePath, final String folderName, final Path destinationJarPath ) {
		Objects.requireNonNull( sourcePath );
		Objects.requireNonNull( folderName );
		Objects.requireNonNull( destinationJarPath );

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
				catch( final Exception e ) {
					throw new RuntimeException( e );
				}
			} );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
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
				throw new RuntimeException( e );
			}
		}

		return path;
	}

	private static void copy( final InputStream source, final OutputStream target ) throws IOException {
		Objects.requireNonNull( source );
		Objects.requireNonNull( target );

		final byte[] buf = new byte[8192];
		int length;
		while( (length = source.read( buf )) > 0 ) {
			target.write( buf, 0, length );
		}
	}
}