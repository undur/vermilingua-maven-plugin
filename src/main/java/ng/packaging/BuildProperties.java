package ng.packaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Wrapper for the build.properties file
 */

public record BuildProperties( Properties properties ) {

	/**
	 * @return BuildProperties by parsing the given path
	 */
	public static BuildProperties of( final Path path ) {

		if( !Files.exists( path ) ) {
			throw new IllegalStateException( "build.properties not found in project root (%s). To build a project with vermilingua, a file called 'build.properties' file must exist in the root and must contain at least the properties %s".formatted( path, requiredBuildProperties() ) );
		}

		try( final InputStream is = Files.newInputStream( path )) {
			final Properties buildProperties = new Properties();
			buildProperties.load( is );
			return new BuildProperties( buildProperties );
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * FIXME: We should be getting this from the source project
	 */
	private static Object requiredBuildProperties() {
		return null;
	}

	public String getProperty( String key ) {
		return properties.getProperty( key );
	}

	public boolean containsKey( String key ) {
		return properties.containsKey( key );
	}
}