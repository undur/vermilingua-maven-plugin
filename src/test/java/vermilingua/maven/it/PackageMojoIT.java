package vermilingua.maven.it;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
	 * resources' archive unpacks to {@code HelloWorld.woa}. A plugin bound to
	 * {@code package} adds a file to the .woa and replaces another, as a
	 * minifier would. Both reach the split folder and both archives, since
	 * they're made later, by {@code vermilingua:archive}.
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

		Path target = result.getMavenProjectResult().getTargetProjectDirectory().resolve("target");
		Path woaArchive = installed.resolve("HelloWorld-1.0-SNAPSHOT.woapplication.tar.gz");
		Path wsrArchive = installed.resolve("HelloWorld-1.0-SNAPSHOT.wowebserverresources.tar.gz");
		String added = "HelloWorld.woa/Contents/WebServerResources/post-processed.txt";
		String replaced = "HelloWorld.woa/Contents/WebServerResources/app.css";
		String minified = "body{margin:0}\n";

		// The file the post-processing added
		assertThat(target.resolve("HelloWorld.woa.webserverresources/Contents/WebServerResources/post-processed.txt")).isRegularFile();
		assertThat(entryNames(woaArchive)).contains(added);
		assertThat(entryNames(wsrArchive)).contains(added);

		// The file it replaced: the minified version everywhere, not the one from src/main/webserver-resources
		assertThat(Files.readString(target.resolve(replaced))).isEqualTo(minified);
		assertThat(Files.readString(target.resolve("HelloWorld.woa.webserverresources/Contents/WebServerResources/app.css"))).isEqualTo(minified);
		assertThat(entryContent(woaArchive, replaced)).isEqualTo(minified);
		assertThat(entryContent(wsrArchive, replaced)).isEqualTo(minified);
		return;
	}

	/**
	 * {@code mvn package} only builds the .woa. The split folder and the
	 * archives are made by {@code vermilingua:archive}, at
	 * {@code pre-integration-test}, even when the POM enables them.
	 */
	@MavenTest
	@MavenGoal("package")
	public void packageMakesNoArchives(MavenExecutionResult result) {
		assertThat(result).isSuccessful();

		Path target = result.getMavenProjectResult().getTargetProjectDirectory().resolve("target");
		assertThat(target.resolve("HelloWorld.woa/HelloWorld")).isRegularFile();
		assertThat(target.resolve("HelloWorld.woa.webserverresources")).doesNotExist();
		assertThat(target.resolve("HelloWorld.woapplication.tar.gz")).doesNotExist();
		assertThat(target.resolve("HelloWorld.wowebserverresources.tar.gz")).doesNotExist();
		return;
	}

	/**
	 * Returns the content of an entry in a tar.gz archive.
	 *
	 * @param archive tar.gz archive
	 * @param name name of the entry
	 * @return content of the entry named {@code name} in {@code archive}, as UTF-8
	 */
	private static String entryContent(Path archive, String name) throws IOException {
		try (InputStream in = Files.newInputStream(archive);
				TarArchiveInputStream tar = new TarArchiveInputStream(new GzipCompressorInputStream(in))) {
			TarArchiveEntry entry;
			while ((entry = tar.getNextEntry()) != null) {
				if (entry.getName().equals(name)) {
					return new String(tar.readAllBytes(), StandardCharsets.UTF_8);
				}
			}
		}
		throw new AssertionError("No entry named " + name + " in " + archive);
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
