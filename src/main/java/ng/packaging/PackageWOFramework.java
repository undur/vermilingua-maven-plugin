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

		// FIXME: Quick hacking to try out Info.plist generation. Refactor, preferably before anyone sees it // Hugi 2021-07-14
		final var name = sourceProject.finalName();
		final var version = mp.getVersion();
		final var mainJarFilename = mp.getArtifact().getArtifactId().toLowerCase() + ".jar";
		final String infoPlistString = InfoPlist.make( sourceProject, name, version, mainJarFilename );
		Util.writeStringToPathInJar( infoPlistString, "Resources/Info.plist", artifactPath );
	}
}