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
			throw new IllegalStateException( "build.properties not found in project root (%s). To build a project with vermilingua, this file must exist".formatted( path ) );
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

	private String get( String key ) {
		return properties().getProperty( key );
	}

	public boolean containsKey( String key ) {
		return properties().containsKey( key );
	}

	public String principalClass() {
		return get( "principalClass" );
	}

	public String jvm() {
		return get( "jvm" );
	}

	public String jvmOptions() {
		return get( "jvmOptions" );
	}

	public String jdb() {
		return get( "jdb" );
	}

	public String jdbOptions() {
		return get( "jdbOptions" );
	}

	public String projectName() {
		return get( "project.name" );
	}
}