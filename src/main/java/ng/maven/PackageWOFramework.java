package ng.maven;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.project.MavenProject;

public class PackageWOFramework {

	/**
	 * FIXME: currently this will only build a maven style project
	 * FIXME: This duplicates some logic from the WOA build. That's on purpose, to help see the natural common aspects of the WOA build and the framework build, before consolidating common logic.
	 */
	public void execute( final MavenProject mavenProject, final String woresourcesFolderName ) {

		// The jar file resulting from the compilation of our application project (App.jar)
		final Path artifactPath = mavenProject.getArtifact().getFile().toPath();

		// FIXME: Flatten components  // Hugi 2021-07-10
		// FIXME: Flatten resources (?)  // Hugi 2021-07-10
		// FIXME: Both of the above happen in some logic common to both the .WOA and the .framework packaging

		Util.copyFolderAtPathToRootOfJar( Paths.get( mavenProject.getBasedir() + "/src/main/components" ), artifactPath );
		Util.copyFolderAtPathToRootOfJar( Paths.get( mavenProject.getBasedir() + "/src/main/" + woresourcesFolderName ), artifactPath );
		Util.copyFolderAtPathToRootOfJar( Paths.get( mavenProject.getBasedir() + "/src/main/webserver-resources" ), artifactPath );
	}
}