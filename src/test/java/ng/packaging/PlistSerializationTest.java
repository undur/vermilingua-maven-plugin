package ng.packaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ng.AbstractTest;

/**
 * Unit tests on {@link PlistSerialization} class.
 *
 * @author paulh
 */
public class PlistSerializationTest extends AbstractTest {
	private static final String LIST_STRING_PLIST = "plists/listString.plist";
	private static List<String> listString;

	private static final String MAP_STRING_STRING_PLIST = "plists/mapStringString.plist";
	private static Map<String, String> mapStringString;

	@BeforeEach
	public void setup() {
		listString = new ArrayList<>();
		listString.add("alpha");
		listString.add("beta");
		listString.add("gamma");

		mapStringString = new HashMap<>();
		mapStringString.put("alpha", "beta");
		mapStringString.put("gamma", "delta");
		return;
	}

	@Test
	public void canSerializeListString() {
		comparePlistSerializationToFile(listString, LIST_STRING_PLIST);
		return;
	}

	@Test
	public void canSerializeMapStringString() {
		comparePlistSerializationToFile(mapStringString, MAP_STRING_STRING_PLIST);
		return;
	}

	/**
	 * Compares the serialization of {@code object} to the expected result in
	 * {@code filename}.
	 * 
	 * @param object   an object
	 * @param filename file containing expected result
	 */
	private void comparePlistSerializationToFile(Object object, String filename) {
		String result = new PlistSerialization(object).toString();
		assertEquals(result, testResourceAsString(filename));
		return;
	}
}
