package ng.maven;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;

/**
 * Integration tests on {@link PackageMojo}.
 * 
 * @author paulh
 */
@MavenJupiterExtension
public class PackageMojoIT {
	@MavenTest
	public void canBuild(MavenExecutionResult result) {
		assertThat(result).isSuccessful();
		return;
	}
}
