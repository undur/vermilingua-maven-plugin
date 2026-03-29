package vermilingua.packaging;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

/**
 * Utility for creating tar.gz archives of directories.
 */

public class ArchiveUtil {

	/**
	 * Creates a tar.gz archive of the given directory.
	 *
	 * @param sourceDirectory The directory to archive
	 * @param targetFile The destination tar.gz file
	 */
	public static void createTarGz( final Path sourceDirectory, final Path targetFile ) {
		try( final OutputStream fos = Files.newOutputStream( targetFile );
			 final BufferedOutputStream bos = new BufferedOutputStream( fos );
			 final GzipCompressorOutputStream gzos = new GzipCompressorOutputStream( bos );
			 final TarArchiveOutputStream taos = new TarArchiveOutputStream( gzos ) ) {

			taos.setLongFileMode( TarArchiveOutputStream.LONGFILE_GNU );

			final Path parentDir = sourceDirectory.getParent();

			try( Stream<Path> paths = Files.walk( sourceDirectory ) ) {
				paths.forEach( path -> {
					try {
						final String entryName = parentDir.relativize( path ).toString();
						final TarArchiveEntry entry = new TarArchiveEntry( path, entryName );

						// Preserve executable permission
						if( Files.isExecutable( path ) && !Files.isDirectory( path ) ) {
							entry.setMode( 0755 );
						}

						taos.putArchiveEntry( entry );

						if( Files.isRegularFile( path ) ) {
							Files.copy( path, taos );
						}

						taos.closeArchiveEntry();
					}
					catch( final IOException e ) {
						throw new UncheckedIOException( e );
					}
				});
			}
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}
}
