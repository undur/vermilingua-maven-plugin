package ng.packaging;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

	private static final Logger logger = LoggerFactory.getLogger( Util.class );

	/**
	 * Copy the file at [sourcePath] to a new file specified by [destinationPath]
	 * This method exists solely to wrap the checked IOException in an UncheckedIOException
	 */
	public static void copyFile( final Path sourcePath, final Path destinationPath, final CopyOption... options ) {
		Objects.requireNonNull( sourcePath );
		Objects.requireNonNull( destinationPath );

		try {
			Files.copy( sourcePath, destinationPath, options );
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * Copy the contents of the directory specified by [sourceDirectory] into the directory specified by [destinationDirectory], maintaining the directory tree/hierarchy.
	 */
	public static void copyContentsOfDirectoryToDirectory( final Path sourceDirectory, final Path destinationDirectory ) {
		Objects.requireNonNull( sourceDirectory );
		Objects.requireNonNull( destinationDirectory );

		try {
			Files.walk( sourceDirectory )
					.forEach( sourcePath -> {
						final Path relativePath = sourceDirectory.relativize( sourcePath );
						final Path targetPath = destinationDirectory.resolve( relativePath );

						// If the target file exists, we don't copy and log a warning.
						// FIXME: We should change this to an error condition, it's just asking for trouble // Hugi 2025-09-27
						if( !Files.exists( targetPath ) ) {
							copyFile( sourcePath, targetPath );
						}
						else {
							logger.warn( "File {} already exists at {}, not copying", sourcePath, targetPath );
						}
					} );
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * Copy files in the directory specified by [sourceDirectory] into the directory specified by [destinationDirectory], not maintaining the hierarchy, i.e. "flattening" the structure.
	 *
	 * Directories with names ending with [directorySuffixesToNotFlatten] are considered "bundles", i.e. they're essentially treated like files and copied in their entirety.
	 */
	public static void copyContentsOfDirectoryToDirectoryFlatten( final Path sourceDirectory, final Path destinationDirectory, final Collection<String> directorySuffixesToNotFlatten ) {
		Objects.requireNonNull( sourceDirectory );
		Objects.requireNonNull( destinationDirectory );
		Objects.requireNonNull( directorySuffixesToNotFlatten );

		final FileVisitor<? super Path> visitor = new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes arg1 ) throws IOException {

				// If this is a bundle/folder we should not flatten, copy it in it's entirety (with all it's contents) and don't walk deeper into it
				if( hasAnyOfSuffixes( dir.getFileName().toString(), directorySuffixesToNotFlatten ) ) {
					copyContentsOfDirectoryToDirectory( dir, destinationDirectory.resolve( dir.getFileName() ) );
					return FileVisitResult.SKIP_SUBTREE;
				}

				// In this case, this is a regular directory so we just keep on walking
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile( Path file, BasicFileAttributes arg1 ) throws IOException {
				// Regular standalone files just get copied. preVisitDirectory() will already have excluded and copied files inside bundles
				copyFile( file, destinationDirectory.resolve( file.getFileName() ), StandardCopyOption.REPLACE_EXISTING );
				return FileVisitResult.CONTINUE;
			}
		};

		try {
			Files.walkFileTree( sourceDirectory, visitor );
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * @return true if the given [string] ends with any of the strings in [endings]
	 */
	private static boolean hasAnyOfSuffixes( String string, Collection<String> suffixes ) {
		Objects.requireNonNull( string );
		Objects.requireNonNull( suffixes );

		for( final String suffix : suffixes ) {
			if( string.endsWith( "." + suffix ) ) {
				return true;
			}
		}

		return false;
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

	/**
	 * @return The value of the named string template resource (stored under src/main/resources/templates
	 */
	public static String readTemplate( final String name ) {
		Objects.requireNonNull( name );

		try( InputStream stream = PackageWOApplication.class.getResourceAsStream( "/templates/" + name + ".template.txt" )) {
			return new String( stream.readAllBytes(), StandardCharsets.UTF_8 );
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * Make file residing at path user executable (basically the equivalent of doing 'chmod u+x' on the shell)
	 */
	public static void makeUserExecutable( final Path path ) {
		Objects.requireNonNull( path );

		try {
			final Set<PosixFilePermission> existingPermissions = Files.getPosixFilePermissions( path );
			existingPermissions.add( PosixFilePermission.OWNER_EXECUTE );
			Files.setPosixFilePermissions( path, existingPermissions );
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
							Files.copy( inStream, targetPath, StandardCopyOption.REPLACE_EXISTING );
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
			final URI uri = URI.create( "jar:" + destinationJarPath.toUri().toString() );

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
	 * Writes [string] as a file at [destinationFilePathInsideJar] to the jar file at [destinationJarPath]
	 */
	public static void writeStringToPathInJar( final String string, final String destinationFilePathInsideJar, final Path destinationJarPath ) {
		Objects.requireNonNull( string );
		Objects.requireNonNull( destinationFilePathInsideJar );
		Objects.requireNonNull( destinationJarPath );

		final URI uri = URI.create( "jar:" + destinationJarPath.toUri().toString() );

		try( FileSystem zipfs = FileSystems.newFileSystem( uri, Collections.emptyMap() )) {
			final Path pathInZipFile = zipfs.getPath( destinationFilePathInsideJar );
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