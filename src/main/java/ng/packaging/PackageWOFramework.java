package ng.packaging;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.project.MavenProject;

public class PackageWOFramework {

	/**
	 * FIXME: currently this will only build a maven style project // Hugi 2021-07-10
	 * FIXME: This duplicates some logic from the WOA build. That's on purpose, to help see the natural common aspects of the WOA build and the framework build, before consolidating common logic. // Hugi 2021-07-10
	 */
	public void execute( final SourceProject sourceProject ) {

		final MavenProject mavenProject = sourceProject.mavenProject();

		// The jar file resulting from the compilation of our application project (App.jar)
		final Path artifactPath = mavenProject.getArtifact().getFile().toPath();

		// FIXME: Flatten components // Hugi 2021-07-10
		// FIXME: Flatten resources (?)  // Hugi 2021-07-10
		// FIXME: Both of the above happen in some logic common to both the .WOA and the .framework packaging // Hugi 2021-07-10

		Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.componentsPath(), "Resources", artifactPath );

		// FIXME: Should be totally OK for this not to exist. Check in the WOA build as well, check for WebsServerResources and Components as well // Hugi 2021-07-10
		if( Files.exists( sourceProject.woresourcesPath() ) ) {
			Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.woresourcesPath(), "Resources", artifactPath );
		}

		Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.webServerResourcesPath(), "WebServerResources", artifactPath );
	}
}