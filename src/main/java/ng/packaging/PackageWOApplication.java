package ng.packaging;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageWOApplication {

	private static final Logger logger = LoggerFactory.getLogger( PackageWOApplication.class );

	/**
	 * Builds a WOA bundle
	 *
	 * @param sourceProject The project we're building from
	 * @param woaName Name of the WOA (not including the .woa suffix)
	 * @param targetPath Directory where the WOA bundle will be placed
	 *
	 * @return The assembled WOA bundle
	 */
	public WOA execute( final SourceProject sourceProject, final String woaName, final Path targetPath ) {
		Objects.requireNonNull( sourceProject );
		Objects.requireNonNull( woaName );
		Objects.requireNonNull( targetPath );

		// The WOA bundle, the destination for our build.
		final WOA woa = WOA.create( targetPath, woaName );

		// The eventual name of the app's JAR file
		final String appJarFilename = sourceProject.targetJarNameForWOA();

		// Copy the app jar to the woa
		Util.copyFile( sourceProject.jarPath(), woa.javaPath().resolve( appJarFilename ) );

		// Start working on that list of jars to add to the classpath
		final List<String> classpathStrings = new ArrayList<>();

		classpathStrings.add( "APPROOT/Resources/Java/" ); // WOLifecycle includes the java folder itself on the classpath. I'm not sure why, but better replicate it // Hugi 2021-07-08
		classpathStrings.add( "APPROOT/Resources/Java/" + appJarFilename );

		// Copy the app's resolved dependencies (direct and transient) to the WOA
		for( final Dependency dependency : sourceProject.dependencies() ) {
			final Path artifactPathInMavenRepository = dependency.file().toPath();
			final Path artifactFolderPathInWOA = Util.folder( woa.javaPath().resolve( dependency.groupId().replace( ".", "/" ) + "/" + dependency.artifactId() + "/" + dependency.version() ) );
			final Path artifactPathInWOA = artifactFolderPathInWOA.resolve( dependency.file().getName() );
			Util.copyFile( artifactPathInMavenRepository, artifactPathInWOA );

			// Add the jar to the classpath
			classpathStrings.add( "APPROOT/" + woa.contentsPath().relativize( artifactPathInWOA ) );
		}

		// Copy WebServerResources from framework jars to the WOA
		for( final Dependency dependency : sourceProject.dependencies() ) {
			if( Util.jarContainsNonEmptyWebServerResourcesDirectoryInRoot( dependency.file() ) ) {
				final Path destinationPath = woa.frameworksPath().resolve( dependency.artifactId() + ".framework" );
				Util.copyFolderFromJarToPath( "WebServerResources", dependency.file().toPath(), destinationPath );
			}
		}

		if( Files.exists( sourceProject.componentsPath() ) ) {
			Util.copyContentsOfDirectoryToDirectoryFlatten( sourceProject.componentsPath(), woa.woresourcesPath(), List.of( "wo" ) );
		}
		else {
			logger.warn( "Not copying components. %s does not exist".formatted( sourceProject.componentsPath() ) );
		}

		if( Files.exists( sourceProject.woresourcesPath() ) ) {
			// CHECKME: this would be where we would flatten resources, if we ever were to do that // Hugi 2021-07-08
			Util.copyContentsOfDirectoryToDirectory( sourceProject.woresourcesPath(), woa.woresourcesPath() );
		}
		else {
			logger.warn( "Not copying woresources. %s does not exist".formatted( sourceProject.woresourcesPath() ) );
		}

		if( Files.exists( sourceProject.webServerResourcesPath() ) ) {
			Util.copyContentsOfDirectoryToDirectory( sourceProject.webServerResourcesPath(), woa.webServerResourcesPath() );
		}
		else {
			logger.warn( "Not copying WebServerResources. %s does not exist".formatted( sourceProject.webServerResourcesPath() ) );
		}

		// The classpath files for MacOS, MacOSXServer and UNIX all look the same
		// CHECKME: MacOS, UNIX and MacOS X Server (Rhapsody?)... There be redundancies // Hugi 2021-07-08
		String classPathFileTemplateString = Util.readTemplate( "classpath" );
		classPathFileTemplateString = classPathFileTemplateString.replace( "${ApplicationClass}", sourceProject.principalClassName() );
		classPathFileTemplateString = classPathFileTemplateString.replace( "${JVMOptions}", sourceProject.jvmOptions() );

		// Write out nice looking UNIX class paths
		final String standardClassPathString = classPathFileTemplateString + String.join( "\n", classpathStrings );
		Util.writeStringToPath( standardClassPathString, woa.unixPath().resolve( "UNIXClassPath.txt" ) );
		Util.writeStringToPath( standardClassPathString, woa.macosPath().resolve( "MacOSClassPath.txt" ) );
		Util.writeStringToPath( standardClassPathString, woa.macosPath().resolve( "MacOSXServerClassPath.txt" ) );

		// Write out Windows classpath file, with wrong line endings and wrong path separators
		final String windowsClassPathString = classPathFileTemplateString + String.join( "\r\n", classpathStrings ).replace( "/", "\\" );
		Util.writeStringToPath( windowsClassPathString, woa.windowsPath().resolve( "CLSSPATH.TXT" ) );

		// CHECKME: I have no idea what the subpaths file is for, but I'm still copying it in (to replicate the old build)  We need to figure that out and document it // Hugi 2022-09-28
		final String windowsSubPathsString = Util.readTemplate( "subpaths" );
		Util.writeStringToPath( windowsSubPathsString, woa.windowsPath().resolve( "SUBPATHS.TXT" ) );

		final String infoPlistString = InfoPlist.make( sourceProject );
		Util.writeStringToPath( infoPlistString, woa.infoPlistPath() );

		// Create the executable script for UNIX
		final String unixLaunchScriptString = Util.readTemplate( "launch-script" );
		final Path unixLaunchScriptPath = woa.woaPath().resolve( sourceProject.name() );
		Util.writeStringToPath( unixLaunchScriptString, unixLaunchScriptPath );
		Util.makeUserExecutable( unixLaunchScriptPath );

		// CHECKME: For some reason, Contents/MacOS contains an exact copy of the launch script from the WOA root // Hugi 2021-07-08
		final Path redundantMacOSLaunchScriptPath = woa.macosPath().resolve( sourceProject.name() );
		Util.writeStringToPath( unixLaunchScriptString, redundantMacOSLaunchScriptPath );
		Util.makeUserExecutable( redundantMacOSLaunchScriptPath );

		// Create the executable script for Windows
		final String windowsLaunchScriptString = Util.readTemplate( "launch-script-cmd" );
		final Path windowsLaunchScriptPath = woa.woaPath().resolve( sourceProject.name() + ".cmd" );
		Util.writeStringToPath( windowsLaunchScriptString, windowsLaunchScriptPath );
		Util.makeUserExecutable( windowsLaunchScriptPath );

		// CHECKME: And of course Contents/Windows contains an exact copy of the Windows script from the WOA root // Hugi 2021-07-08
		final Path redundantWindowsLaunchScriptPath = woa.windowsPath().resolve( sourceProject.name() + ".cmd" );
		Util.writeStringToPath( windowsLaunchScriptString, redundantWindowsLaunchScriptPath );
		Util.makeUserExecutable( redundantWindowsLaunchScriptPath );

		return woa;
	}

	/**
	 * Our in-memory representation of the WOA bundle
	 */
	public static class WOA {

		private final Path _woaPath;

		/**
		 * @return The WOA bundle [applicationName].woa in [containingDirectory]
		 */
		public static WOA create( final Path containingDirectory, final String applicationName ) {
			Objects.requireNonNull( containingDirectory );
			Objects.requireNonNull( applicationName );
			final Path woaPath = containingDirectory.resolve( applicationName + ".woa" );
			return new WOA( woaPath, applicationName );
		}

		private WOA( final Path woaPath, final String applicationName ) {
			Objects.requireNonNull( woaPath );
			Objects.requireNonNull( applicationName );
			_woaPath = Util.folder( woaPath );
		}

		/**
		 * @return Path to the WOA root
		 */
		public Path woaPath() {
			return _woaPath;
		}

		/**
		 * @return Root destination path for the WOA's contents
		 */
		public Path contentsPath() {
			return Util.folder( woaPath().resolve( "Contents" ) );
		}

		/**
		 * @return Destination path for frameworks to be embedded in the WOA bundle
		 */
		public Path frameworksPath() {
			return Util.folder( contentsPath().resolve( "Frameworks" ) );
		}

		/**
		 * @return Destination path for macOS specific launch scripts/configuration
		 */
		public Path macosPath() {
			return Util.folder( contentsPath().resolve( "MacOS" ) );
		}

		/**
		 * @return Destination path for Unix/Linux specific launch scripts/configuration
		 */
		public Path unixPath() {
			return Util.folder( contentsPath().resolve( "UNIX" ) );
		}

		/**
		 * @return Destination path for Windows specific launch scripts/configuration
		 */
		public Path windowsPath() {
			return Util.folder( contentsPath().resolve( "Windows" ) );
		}

		/**
		 * @return Destination path for WO's woresources/bundle-resources/application-resources/whacchamacallit
		 */
		public Path woresourcesPath() {
			return Util.folder( contentsPath().resolve( "Resources" ) );
		}

		/**
		 * @return Destination path for WO's webserver resources
		 */
		public Path webServerResourcesPath() {
			return Util.folder( contentsPath().resolve( "WebServerResources" ) );
		}

		/**
		 * @return Destination path for jar files
		 */
		public Path javaPath() {
			return Util.folder( woresourcesPath().resolve( "Java" ) );
		}

		/**
		 * @return Destination path for Info.plist
		 */
		public Path infoPlistPath() {
			return contentsPath().resolve( "Info.plist" );
		}

		/**
		 * CHECKME: Placeholder for this functionality. It's really an outside task that I don't like having part of the WOA // Hugi 2021-07-17
		 */
		public void extractWebServerResources() {
			final Path splitPath = Util.folder( woaPath().getParent().resolve( woaPath().getFileName() + ".webserverresources" ) );
			final Path splitWebServerResourcesPath = Util.folder( splitPath.resolve( "Contents" ).resolve( "WebServerResources" ) );
			final Path splitFrameworksPath = Util.folder( splitPath.resolve( "Contents" ).resolve( "Frameworks" ) );

			Util.copyContentsOfDirectoryToDirectory( webServerResourcesPath(), splitWebServerResourcesPath );
			Util.copyContentsOfDirectoryToDirectory( frameworksPath(), splitFrameworksPath );
		}
	}
}