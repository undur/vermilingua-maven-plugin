package vermilingua.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

public class PackageMojoFolderPathTest {

	private static String folderPath( final String configured, final String declared ) throws MojoFailureException {
		return new PackageMojo().folderPath( "componentsPath", configured, "dir.components", declared, "src/main/components" );
	}

	@Test
	public void defaultWhenNothingIsDeclared() throws MojoFailureException {
		assertEquals( "src/main/components", folderPath( null, null ) );
	}

	@Test
	public void buildPropertiesOverridesTheDefault() throws MojoFailureException {
		assertEquals( "Components", folderPath( null, "Components" ) );
	}

	@Test
	public void pluginConfigurationOverridesBuildProperties() throws MojoFailureException {
		assertEquals( "src/components", folderPath( "src/components", "Components" ) );
		assertEquals( "src/components", folderPath( "src/components", null ) );
	}

	@Test
	public void blankInBuildPropertiesFails() {
		final MojoFailureException e = assertThrows( MojoFailureException.class, () -> folderPath( null, "" ) );
		assertTrue( e.getMessage().contains( "dir.components" ), e.getMessage() );
		assertThrows( MojoFailureException.class, () -> folderPath( null, "   " ) );
	}

	@Test
	public void blankInPluginConfigurationFails() {
		final MojoFailureException e = assertThrows( MojoFailureException.class, () -> folderPath( "", "Components" ) );
		assertTrue( e.getMessage().contains( "componentsPath" ), e.getMessage() );
	}
}
