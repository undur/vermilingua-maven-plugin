package ng.packaging;

import java.nio.file.Files;
import java.nio.file.Path;

public class PackageWOFramework {

	/**
	 * FIXME: Flatten components // Hugi 2021-07-10
	 * FIXME: Flatten resources (?)  // Hugi 2021-07-10
	 */
	public void execute( final SourceProject sourceProject ) {

		final Path artifactPath = sourceProject.mavenProject().getArtifact().getFile().toPath();

		Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.componentsPath(), "Resources", artifactPath );

		// FIXME: Should be totally OK for this not to exist. Check in the WOA build as well, check for WebServerResources and Components as well // Hugi 2021-07-10
		if( Files.exists( sourceProject.woresourcesPath() ) ) {
			Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.woresourcesPath(), "Resources", artifactPath );
		}

		Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.webServerResourcesPath(), "WebServerResources", artifactPath );
	}
}