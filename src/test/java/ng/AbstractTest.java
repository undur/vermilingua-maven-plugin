package ng;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Abstract parent class for unit tests providing some potentially useful
 * methods.
 *
 * @author paulh
 */
public abstract class AbstractTest {

	/**
	 * Returns a resource as a {@link String}. {@code filename} should be a path
	 * relative to {@code src/test/resources}.
	 *
	 * @param filename filename
	 * @return content of {@code filename} as a {@link String}
	 */
	protected String testResourceAsString( String filename ) {
		try( InputStream is = getClass().getClassLoader().getResourceAsStream( filename )) {
			return new String( is.readAllBytes() );
		}
		catch( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}
}