package ng.packaging;

import java.nio.file.Path;

import org.apache.maven.project.MavenProject;

public class PackageWOFramework {

	/**
	 * FIXME: Flatten components // Hugi 2021-07-10
	 * FIXME: Flatten resources (?)  // Hugi 2021-07-10
	 */
	public void execute( final SourceProject sourceProject ) {

		final MavenProject mp = sourceProject.mavenProject();
		final Path artifactPath = mp.getArtifact().getFile().toPath();

		Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.componentsPath(), "Resources", artifactPath );
		Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.woresourcesPath(), "Resources", artifactPath );
		Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.webServerResourcesPath(), "WebServerResources", artifactPath );

		final String infoPlistString = InfoPlist.make( sourceProject );
		Util.writeStringToPathInJar( infoPlistString, "Resources/Info.plist", artifactPath );
	}
}