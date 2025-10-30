package ng.packaging;

import java.util.Properties;

/**
 * Wrapper for the build properties file
 */

public record BuildProperties( Properties properties ) {

	public String getProperty( String key ) {
		return properties.getProperty( key );
	}

	public boolean containsKey( String key ) {
		return properties.containsKey( key );
	}
}