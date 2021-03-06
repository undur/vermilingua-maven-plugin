package ng.packaging;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

public class PackageWOApplication {

	public WOA execute( final SourceProject sourceProject, final String finalName ) {

		final MavenProject mavenProject = sourceProject.mavenProject();

		// Usually Maven's standard 'target' directory
		final Path buildPath = Path.of( mavenProject.getBuild().getDirectory() );

		// The WOA bundle, the destination for our build.
		final WOA woa = WOA.create( buildPath, finalName );

		// The eventual name of the app's JAR file
		final String appJarFilename = sourceProject.targetJarNameForWOA();

		// Copy the app jar to the woa
		Util.copyFile( sourceProject.jarPath(), woa.javaPath().resolve( appJarFilename ) );

		// Start working on that list of jars to add to the classpath
		final List<String> classpathStrings = new ArrayList<>();

		// CHECKME: For some reason the older plugin includes the java folder itself on the classpath. Better replicate that for now, check later // Hugi 2021-07-08
		classpathStrings.add( "APPROOT/Resources/Java/" );
		classpathStrings.add( "APPROOT/Resources/Java/" + appJarFilename );

		// Copy the app's resolved dependencies (direct and transient) to the WOA
		for( final Artifact artifact : mavenProject.getArtifacts() ) {
			final Path artifactPathInMavenRepository = artifact.getFile().toPath();
			final Path artifactFolderPathInWOA = Util.folder( woa.javaPath().resolve( artifact.getGroupId().replace( ".", "/" ) + "/" + artifact.getArtifactId() + "/" + artifact.getVersion() ) );
			final Path artifactPathInWOA = artifactFolderPathInWOA.resolve( artifact.getFile().getName() );
			Util.copyFile( artifactPathInMavenRepository, artifactPathInWOA );

			// Add the jar to the classpath
			classpathStrings.add( "APPROOT/" + woa.contentsPath().relativize( artifactPathInWOA ) );
		}

		// Copy WebServerResources from framework jars to the WOA
		for( final Artifact artifact : mavenProject.getArtifacts() ) {
			if( Util.containsNonEmptyWebServerResourcesDirectory( artifact.getFile() ) ) {
				final Path destinationPath = woa.frameworksPath().resolve( artifact.getArtifactId() + ".framework" );
				Util.copyFolderFromJarToPath( "WebServerResources", artifact.getFile().toPath(), destinationPath );
			}
		}

		// FIXME: Flatten components // Hugi 2021-07-08
		// FIXME: Flatten resources // Hugi 2021-07-08

		if( Files.exists( sourceProject.componentsPath() ) ) {
			Util.copyContentsOfDirectoryToDirectory( sourceProject.componentsPath(), woa.woresourcesPath() );
		}
		else {
			System.out.println( "[WARNING] - Not copying components. %s does not exist".formatted( sourceProject.componentsPath() ) ); // FIXME: Replace with logger
		}

		if( Files.exists( sourceProject.woresourcesPath() ) ) {
			Util.copyContentsOfDirectoryToDirectory( sourceProject.woresourcesPath(), woa.woresourcesPath() );
		}
		else {
			System.out.println( "[WARNING] - Not copying woresources. %s does not exist".formatted( sourceProject.woresourcesPath() ) ); // FIXME: Replace with logger
		}

		if( Files.exists( sourceProject.webServerResourcesPath() ) ) {
			Util.copyContentsOfDirectoryToDirectory( sourceProject.webServerResourcesPath(), woa.webServerResourcesPath() );
		}
		else {
			System.out.println( "[WARNING] - Not copying WebServerResources. %s does not exist".formatted( sourceProject.webServerResourcesPath() ) ); // FIXME: Replace with logger
		}

		// The classpath files for MacOS, MacOSXServer and UNIX all look the same
		// CHECKME: MacOS, UNIX and MacOS X Server (Rhapsody?)... There be redundancies // Hugi 2021-07-08
		String classPathFileTemplateString = Util.readTemplate( "classpath" );
		classPathFileTemplateString = classPathFileTemplateString.replace( "${ApplicationClass}", sourceProject.principalClassName() );
		final String standardClassPathString = classPathFileTemplateString + String.join( "\n", classpathStrings );
		Util.writeStringToPath( standardClassPathString, woa.unixPath().resolve( "UNIXClassPath.txt" ) );
		Util.writeStringToPath( standardClassPathString, woa.macosPath().resolve( "MacOSClassPath.txt" ) );
		Util.writeStringToPath( standardClassPathString, woa.macosPath().resolve( "MacOSXServerClassPath.txt" ) );

		final String windowsClassPathString = classPathFileTemplateString + String.join( "\r\n", classpathStrings ).replace( "/", "\\" ); //CHECKME: Nice pretzels. We can make this more understandable // Hugi 2021-07-08
		Util.writeStringToPath( windowsClassPathString, woa.windowsPath().resolve( "CLSSPATH.TXT" ) );

		// CHECKME: I have no idea what the subpaths file does. // Hugi 2021-07-08
		final String windowsSubPathsString = Util.readTemplate( "subpaths" );
		Util.writeStringToPath( windowsSubPathsString, woa.windowsPath().resolve( "SUBPATHS.TXT" ) );

		final String infoPlistString = InfoPlist.make( sourceProject );
		final Path infoPlistPath = woa.contentsPath().resolve( "Info.plist" );
		Util.writeStringToPath( infoPlistString, infoPlistPath );

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

		public Path woaPath() {
			return _woaPath;
		}

		public Path contentsPath() {
			return Util.folder( woaPath().resolve( "Contents" ) );
		}

		public Path frameworksPath() {
			return Util.folder( contentsPath().resolve( "Frameworks" ) );
		}

		public Path macosPath() {
			return Util.folder( contentsPath().resolve( "MacOS" ) );
		}

		public Path unixPath() {
			return Util.folder( contentsPath().resolve( "UNIX" ) );
		}

		public Path windowsPath() {
			return Util.folder( contentsPath().resolve( "Windows" ) );
		}

		public Path woresourcesPath() {
			return Util.folder( contentsPath().resolve( "Resources" ) );
		}

		public Path webServerResourcesPath() {
			return Util.folder( contentsPath().resolve( "WebServerResources" ) );
		}

		public Path javaPath() {
			return Util.folder( woresourcesPath().resolve( "Java" ) );
		}

		/**
		 * FIXME: Initial placeholder for this functionality, it's an outside task that I don't like having as a part of the WOA
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