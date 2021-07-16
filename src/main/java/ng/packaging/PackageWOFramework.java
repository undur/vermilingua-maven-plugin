package ng.packaging;

public class PackageWOFramework {

	/**
	 * FIXME: Flatten components // Hugi 2021-07-10
	 * FIXME: Flatten resources (?)  // Hugi 2021-07-10
	 */
	public void execute( final SourceProject sourceProject ) {

		Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.componentsPath(), "Resources", sourceProject.jarPath() );
		Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.woresourcesPath(), "Resources", sourceProject.jarPath() );
		Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.webServerResourcesPath(), "WebServerResources", sourceProject.jarPath() );

		final String infoPlistString = InfoPlist.make( sourceProject );
		Util.writeStringToPathInJar( infoPlistString, "Resources/Info.plist", sourceProject.jarPath() );
	}
}