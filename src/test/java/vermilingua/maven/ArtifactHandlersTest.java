package vermilingua.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tests on the artifact handlers declared in components.xml
 */
public class ArtifactHandlersTest {

	/**
	 * Maven names an artifact's file with the extension declared for its type, both when the plugin attaches the archives and when a project depends on them.
	 * The archives are deployed as [artifactId]-[version].[type], as wolifecycle deploys them, so each type's extension is its full name.
	 * With "tar.gz", a dependency of type woapplication.tar.gz looks for [artifactId]-[version].tar.gz, which is never deployed.
	 */
	@Test
	public void archiveTypesHaveTheirFullNameAsExtension() throws Exception {
		final Map<String, String> extensions = extensionsByType();
		assertEquals( "woapplication.tar.gz", extensions.get( "woapplication.tar.gz" ) );
		assertEquals( "wowebserverresources.tar.gz", extensions.get( "wowebserverresources.tar.gz" ) );
	}

	/**
	 * @return The extension declared for each artifact handler's type in components.xml
	 */
	private static Map<String, String> extensionsByType() throws Exception {
		final Path componentsXml = Path.of( "src/main/resources/META-INF/plexus/components.xml" );
		final NodeList configurations = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( componentsXml.toFile() ).getElementsByTagName( "configuration" );
		final Map<String, String> extensions = new HashMap<>();

		for( int i = 0; i < configurations.getLength(); i++ ) {
			final Element configuration = (Element)configurations.item( i );
			final NodeList type = configuration.getElementsByTagName( "type" );
			final NodeList extension = configuration.getElementsByTagName( "extension" );

			if( type.getLength() == 1 && extension.getLength() == 1 ) {
				extensions.put( type.item( 0 ).getTextContent().trim(), extension.item( 0 ).getTextContent().trim() );
			}
		}

		return extensions;
	}
}
