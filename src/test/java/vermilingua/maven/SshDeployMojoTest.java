package vermilingua.maven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SshDeployMojoTest {

	@Test
	public void activationScriptHasEveryPlaceholderReplaced() {
		final String script = SshDeployMojo.activationScript( "MyApp", SshDeployMojo.serviceName( "MyApp" ), "MyApp", "MyApp-1.0.woa", "20261019-120000", "8080", "25", "-Some arg" );

		// Shell variables are written $NAME, so anything left in ${...} form is a placeholder we missed
		assertFalse( script.matches( "(?s).*\\$\\{[A-Za-z]+\\}.*" ), script );
		assertTrue( script.contains( "SERVICE=\"vermilingua-MyApp\"" ), script );
		assertTrue( script.contains( "ln -sfn \"releases/20261019-120000\"" ), script );
		assertTrue( script.contains( "ExecStart=$APP_DIR/current/MyApp-1.0.woa/MyApp -launch.jvm=$JAVA -WOPort $PORT -Some arg" ), script );
	}
}
