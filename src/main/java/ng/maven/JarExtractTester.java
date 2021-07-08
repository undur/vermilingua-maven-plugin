package ng.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarExtractTester {

	public static void main( String[] args ) {
		try {
			final JarFile jarFile = new JarFile( new File( "/Users/hugi/.m2/repository/is/rebbi/helium/2.0.0-SNAPSHOT/helium-2.0.0-SNAPSHOT.jar" ) );

			final Enumeration<JarEntry> entries = jarFile.entries();

			while( entries.hasMoreElements() ) {
				final JarEntry entry = entries.nextElement();

				if( entry.getName().startsWith( "WebServerResources/" ) ) {
					final File targetFile = new File( "/Users/hugi/Desktop/data/" + entry.getName() );

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
		catch( final Exception e ) {
			throw new RuntimeException( e );
		}
	}

	static void copy( InputStream source, OutputStream target ) throws IOException {
		final byte[] buf = new byte[8192];
		int length;
		while( (length = source.read( buf )) > 0 ) {
			target.write( buf, 0, length );
		}
	}
}