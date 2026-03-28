package ng.packaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Wrapper for build properties with layered resolution.
 *
 * Values are resolved in this order (first match wins):
 * 1. System properties with "launch." prefix (e.g. -Dlaunch.jvm=/opt/jdk/bin/java)
 * 2. Environment-specific properties file (e.g. build.properties.prod)
 * 3. The base build.properties file
 */

public class BuildProperties {

	private final Properties _baseProperties;
	private final Properties _environmentProperties;
	private final Properties _systemProperties;

	private BuildProperties( final Properties baseProperties, final Properties environmentProperties, final Properties systemProperties ) {
		_baseProperties = baseProperties;
		_environmentProperties = environmentProperties;
		_systemProperties = systemProperties;
	}

	/**
	 * @return BuildProperties by parsing build.properties at the given path, with optional environment overlay and system property overrides
	 */
	public static BuildProperties of( final Path basePath, final String environment, final Properties systemProperties ) {

		final Path baseFile = basePath.resolve( "build.properties" );

		if( !Files.exists( baseFile ) ) {
			throw new IllegalStateException( "build.properties not found in project root (%s). To build a project with vermilingua, this file must exist".formatted( baseFile ) );
		}

		final Properties baseProperties = loadProperties( baseFile );

		Properties environmentProperties = new Properties();
		if( environment != null && !environment.isEmpty() ) {
			final Path envFile = basePath.resolve( "build.properties." + environment );
			if( Files.exists( envFile ) ) {
				environmentProperties = loadProperties( envFile );
			}
		}

		return new BuildProperties( baseProperties, environmentProperties, systemProperties );
	}

	/**
	 * @return BuildProperties by parsing build.properties at the given path (no environment overlay or system properties)
	 */
	public static BuildProperties of( final Path basePath ) {
		return of( basePath, null, new Properties() );
	}

	private static Properties loadProperties( final Path path ) {
		try( final InputStream is = Files.newInputStream( path ) ) {
			final Properties properties = new Properties();
			properties.load( is );
			return properties;
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	private String get( String key ) {
		// 1. Check system properties with "launch." prefix
		final String launchValue = _systemProperties.getProperty( "launch." + key );
		if( launchValue != null ) {
			return launchValue;
		}

		// 2. Check environment-specific properties
		final String envValue = _environmentProperties.getProperty( key );
		if( envValue != null ) {
			return envValue;
		}

		// 3. Fall back to base build.properties
		return _baseProperties.getProperty( key );
	}

	public boolean containsKey( String key ) {
		return _systemProperties.containsKey( "launch." + key )
			|| _environmentProperties.containsKey( key )
			|| _baseProperties.containsKey( key );
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