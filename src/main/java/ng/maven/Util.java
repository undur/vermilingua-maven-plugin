package ng.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Util {

	public static void copyFile( final Path sourcePath, final Path destinationPath ) {
		try {
			Files.copy( sourcePath, destinationPath );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * FIXME: Change to accept Paths as parameters
	 */
	public static void copyContentsOfDirectoryToDirectory( String sourceDirectoryLocation, String destinationDirectoryLocation ) {
		try {
			Files.walk( Paths.get( sourceDirectoryLocation ) )
					.forEach( source -> {
						final Path destination = Paths.get( destinationDirectoryLocation, source.toString().substring( sourceDirectoryLocation.length() ) );
						try {
							if( !Files.exists( destination ) ) { // FIXME: This is just a hackyhack
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

	public static void writeStringToPath( final String string, final Path path ) {
		try {
			Files.write( path, string.getBytes( StandardCharsets.UTF_8 ) );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}
	}

	public static void makeExecutable( final Path path ) {
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
			e.printStackTrace();
		}
	}

	public static byte[] byteArrayFromInputStream( InputStream is ) {
		try {
			final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			final byte[] data = new byte[16384];

			while( (nRead = is.read( data, 0, data.length )) != -1 ) {
				buffer.write( data, 0, nRead );
			}

			return buffer.toByteArray();
		}
		catch( final Exception e ) {
			throw new RuntimeException( e );
		}
	}

	public static String template( final String name ) {
		try( InputStream stream = PackageMojo.class.getResourceAsStream( "/scripts/" + name + ".template.txt" )) {
			return new String( byteArrayFromInputStream( stream ), StandardCharsets.UTF_8 );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * @return The folder at the given path. Creates the folder if missing, throws an exception if the path exists but is not a folder.
	 */
	public static Path folder( final Path path ) {
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

	public static void copy( final InputStream source, final OutputStream target ) throws IOException {
		final byte[] buf = new byte[8192];
		int length;
		while( (length = source.read( buf )) > 0 ) {
			target.write( buf, 0, length );
		}
	}

	/**
	 * Yeah, two arguments, one is a path, the other one a file. So shoot me.
	 */
	public static void copyWebServerResourcesFromJarToPath( final File sourceJarFile, final Path destinationPath ) {
		Objects.requireNonNull( sourceJarFile );
		Objects.requireNonNull( destinationPath );

		try( final JarFile jarFile = new JarFile( sourceJarFile )) {
			final Enumeration<JarEntry> entries = jarFile.entries();

			while( entries.hasMoreElements() ) {
				final JarEntry entry = entries.nextElement();

				if( entry.getName().startsWith( "WebServerResources/" ) ) {
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
}