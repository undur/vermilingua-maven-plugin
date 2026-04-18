package vermilingua.packaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for build properties with layered resolution.
 *
 * Values are resolved in this order (first match wins):
 * 1. Overrides. Usually system properties with "launch." prefix (e.g. -Dlaunch.jvm=/opt/jdk/bin/java)
 * 2. Environment-specific properties file (e.g. build.properties.prod)
 * 3. The base build.properties file
 */

public class BuildProperties {

	private static final Logger logger = LoggerFactory.getLogger( BuildProperties.class );

	private static final String LAUNCH_PREFIX = "launch.";

	private final Properties _baseProperties;
	private final Properties _environmentProperties;
	private final Properties _overriddes;

	private BuildProperties( final Properties baseProperties, final Properties environmentProperties, final Properties overrides ) {
		_baseProperties = baseProperties;
		_environmentProperties = environmentProperties;
		_overriddes = overrides;
	}

	/**
	 * @return BuildProperties by parsing build.properties at the given path, with optional environment overlay and system property overrides
	 */
	public static BuildProperties of( final Path basePath, final String environment, final Properties overrides ) {

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

		return new BuildProperties( baseProperties, environmentProperties, overrides );
	}

	/**
	 * @return BuildProperties by parsing build.properties at the given path (no environment overlay or system properties)
	 */
	public static BuildProperties of( final Path basePath ) {
		return of( basePath, null, new Properties() );
	}

	private static Properties loadProperties( final Path path ) {
		try( final InputStream is = Files.newInputStream( path )) {
			final Properties properties = new Properties();
			properties.load( is );
			return properties;
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	private String get( String key ) {
		final String prefixedKey = LAUNCH_PREFIX + key;

		// 1. Check system properties with "launch." prefix
		final String systemValue = _overriddes.getProperty( prefixedKey );
		if( systemValue != null ) {
			return systemValue;
		}

		// 2. Check environment-specific properties (prefixed, then legacy unprefixed)
		final String envPrefixedValue = _environmentProperties.getProperty( prefixedKey );
		if( envPrefixedValue != null ) {
			return envPrefixedValue;
		}

		final String envLegacyValue = _environmentProperties.getProperty( key );
		if( envLegacyValue != null ) {
			logDeprecationWarning( key );
			return envLegacyValue;
		}

		// 3. Check base build.properties (prefixed, then legacy unprefixed)
		final String basePrefixedValue = _baseProperties.getProperty( prefixedKey );
		if( basePrefixedValue != null ) {
			return basePrefixedValue;
		}

		final String baseLegacyValue = _baseProperties.getProperty( key );
		if( baseLegacyValue != null ) {
			logDeprecationWarning( key );
			return baseLegacyValue;
		}

		return null;
	}

	public boolean containsKey( String key ) {
		final String prefixedKey = LAUNCH_PREFIX + key;
		return _overriddes.containsKey( prefixedKey )
				|| _environmentProperties.containsKey( prefixedKey )
				|| _environmentProperties.containsKey( key )
				|| _baseProperties.containsKey( prefixedKey )
				|| _baseProperties.containsKey( key );
	}

	private static void logDeprecationWarning( String key ) {
		logger.warn( "Property '{}' in build.properties is deprecated. Use 'launch.{}' instead.", key, key );
	}

	public String principalClass() {
		return _baseProperties.getProperty( "principalClass" );
	}

	public String jvm() {
		return get( "jvm" );
	}

	public String jvmOptions() {
		return get( "jvmOptions" );
	}

	public String projectName() {
		return _baseProperties.getProperty( "project.name" );
	}
}