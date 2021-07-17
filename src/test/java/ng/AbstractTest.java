package ng;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

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
	protected String testResourceAsString(String filename) {
		InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
		return reader.lines().collect(Collectors.joining(System.lineSeparator()));
	}
}
