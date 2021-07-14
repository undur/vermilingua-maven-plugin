package ng.packaging;

import java.nio.file.Files;
import java.nio.file.Path;

import ng.packaging.SourceProject.Type;

public class PackageWOFramework {

	/**
	 * FIXME: Flatten components // Hugi 2021-07-10
	 * FIXME: Flatten resources (?)  // Hugi 2021-07-10
	 */
	public void execute( final SourceProject sourceProject ) {

		final Path artifactPath = sourceProject.mavenProject().getArtifact().getFile().toPath();

		// FIXME: Should be totally OK for resource folders not to exist. Applies to the WOA build as well // Hugi 2021-07-10

		if( Files.exists( sourceProject.componentsPath() ) ) {
			Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.componentsPath(), "Resources", artifactPath );
		}

		if( Files.exists( sourceProject.woresourcesPath() ) ) {
			Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.woresourcesPath(), "Resources", artifactPath );
		}

		if( Files.exists( sourceProject.webServerResourcesPath() ) ) {
			Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.webServerResourcesPath(), "WebServerResources", artifactPath );
		}

		// FIXME: Quick hacking to try out Info.plist generation. Refactor, preferably before anyone sees it // Hugi 2021-07-14
		final var mp = sourceProject.mavenProject();
		final var name = mp.getArtifactId();
		final var version = mp.getVersion();
		final var mainJarFilename = mp.getArtifact().getArtifactId().toLowerCase() + ".jar";
		final String infoPlistString = InfoPlist.make( Type.Framework, name, version, mainJarFilename, sourceProject.principalClassName() );
		Util.writeStringToPathInJar( infoPlistString, "Resources/Info.plist", artifactPath );
	}
}