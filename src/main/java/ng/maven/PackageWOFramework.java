package ng.maven;

import java.nio.file.Files;
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

		Util.copyContentsOfFolderAtPathToFolderInJar( Paths.get( mavenProject.getBasedir() + "/src/main/components" ), "Resources", artifactPath );

		final Path resourcesSourcePath = Paths.get( mavenProject.getBasedir() + "/src/main/" + woresourcesFolderName );

		// FIXME: Should be totally OK for this not to exist. Check in the WOA build as well, check for WebsServerResources and Components as well
		if( Files.exists( resourcesSourcePath ) ) {
			Util.copyContentsOfFolderAtPathToFolderInJar( resourcesSourcePath, "Resources", artifactPath );
		}

		Util.copyContentsOfFolderAtPathToFolderInJar( Paths.get( mavenProject.getBasedir() + "/src/main/webserver-resources" ), "WebServerResources", artifactPath );
	}
}