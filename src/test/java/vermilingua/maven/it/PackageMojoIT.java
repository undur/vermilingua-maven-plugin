package vermilingua.maven.it;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import com.soebes.itf.jupiter.extension.MavenGoal;
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

	/**
	 * With {@code performSplit} and {@code createArchives}, both archives are
	 * installed with the names wolifecycle gives them, and the webserver
	 * resources' archive unpacks to {@code HelloWorld.woa}.
	 */
	@MavenTest
	@MavenGoal("install")
	public void canBuildSplitArchives(MavenExecutionResult result) throws IOException {
		assertThat(result).isSuccessful();

		Path installed = result.getMavenProjectResult().getTargetCacheDirectory().resolve("ng/maven/HelloWorld/1.0-SNAPSHOT");
		assertThat(installed.resolve("HelloWorld-1.0-SNAPSHOT.woapplication.tar.gz")).isRegularFile();
		assertThat(installed.resolve("HelloWorld-1.0-SNAPSHOT.wowebserverresources.tar.gz")).isRegularFile();
		assertThat(installed.resolve("HelloWorld-1.0-SNAPSHOT-wowebserverresources.tar.gz")).doesNotExist();

		assertThat(entryNames(installed.resolve("HelloWorld-1.0-SNAPSHOT.wowebserverresources.tar.gz")))
				.contains("HelloWorld.woa/Contents/WebServerResources/readme.txt")
				.allMatch(name -> name.startsWith("HelloWorld.woa/"));
		return;
	}

	/**
	 * Returns the names of the entries in a tar.gz archive.
	 *
	 * @param archive tar.gz archive
	 * @return names of the entries in {@code archive}
	 */
	private static List<String> entryNames(Path archive) throws IOException {
		List<String> names = new ArrayList<>();
		try (InputStream in = Files.newInputStream(archive);
				TarArchiveInputStream tar = new TarArchiveInputStream(new GzipCompressorInputStream(in))) {
			TarArchiveEntry entry;
			while ((entry = tar.getNextEntry()) != null) {
				names.add(entry.getName());
			}
		}
		return names;
	}
}
