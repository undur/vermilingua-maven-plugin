package ng.packaging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

public class PackageWOApplication {

	/**
	 * FIXME: Including this as a flag while testing. Will probably get deleted later (since we want this to be the default) // Hugi 2021-07-11
	 */
	private final boolean flattenComponents = false;

	public void execute( final SourceProject sourceProject ) {

		final MavenProject mavenProject = sourceProject.mavenProject();

		// Usually Maven's standard 'target' directory
		final Path buildPath = Path.of( mavenProject.getBuild().getDirectory() );

		// The jar file resulting from the compilation of our application project (App.jar)
		final Path artifactPath = mavenProject.getArtifact().getFile().toPath();

		// The name of the application, gotten from the artifactId
		final String applicationName = mavenProject.getArtifactId();

		// The WOA bundle, the destination for our build. Bundle gets named after the app's artifactId
		final WOA woa = WOA.create( buildPath, applicationName );

		// The eventual name of the app's JAR file. Lowercase app name with .jar appended.
		// CHECKME: I'm not sure why they chose to lowercase the JAR name. It seems totally unnecessary // Hugi 2021-07-08
		final String appJarFilename = mavenProject.getArtifact().getArtifactId().toLowerCase() + ".jar";

		// Copy the app jar to the woa
		Util.copyFile( artifactPath, woa.javaPath().resolve( appJarFilename ) );

		// Start working on that list of jars to add to the classpath
		final List<String> classpathStrings = new ArrayList<>();

		// CHECKME: For some reason the older plugin includes the java folder itself on the classpath. Better replicate that for now, check later // Hugi 2021-07-08
		classpathStrings.add( "APPROOT/Resources/Java/" );

		// CHECKME: Not a fan of using hardcoded folder names // Hugi 2021-07-08
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
			if( Util.containsWebServerResources( artifact.getFile() ) ) {
				final Path destinationPath = woa.contentsPath().resolve( "Frameworks" ).resolve( artifact.getArtifactId() + ".framework" );
				Util.copyFolderFromJarToPath( "WebServerResources", artifact.getFile().toPath(), destinationPath );
			}
		}

		if( flattenComponents ) {
			// So here's the deal:
			// We're going to walk down the tree and look at each path in src/components.
			// If the path represents any plain file (and not in a .wo bundlefolder) we dump it into [resource container], no questions asked.
			// If the path represents a folder with the suffix .wo, we're going to copy it and it's contents to [resource container] and stop going down that path.
			// [resource container] is usually the WOA's /Resources, except
			// if the component is localized (in src/components/[lang].lproj), in which case [resource container] will be /Resources/[lang].lproj
			try {
				Files.walk( sourceProject.comopnentsPath() ).forEach( current -> {

				} );
			}
			catch( final IOException e ) {
				throw new RuntimeException( e );
			}
		}
		else {
			Util.copyContentsOfDirectoryToDirectory( sourceProject.comopnentsPath().toString(), woa.resourcesPath().toString() );
		}

		// FIXME: Flatten components  // Hugi 2021-07-08
		Util.copyContentsOfDirectoryToDirectory( sourceProject.resourcesPath().toString(), woa.resourcesPath().toString() );
		// FIXME: Flatten resources (?)  // Hugi 2021-07-08
		Util.copyContentsOfDirectoryToDirectory( sourceProject.webServerResourcesPath().toString(), woa.webServerResourcesPath().toString() );

		// The classpath files for MacOS, MacOSXServer and UNIX all look the same
		// CHECKME: MacOS, UNIX and MacOS X Server (Rhapsody?)... There be redundancies // Hugi 2021-07-08
		String classPathFileTemplateString = Util.readTemplate( "classpath" );
		classPathFileTemplateString = classPathFileTemplateString.replace( "${ApplicationClass}", sourceProject.applicationClassName( mavenProject ) );
		final String standardClassPathString = classPathFileTemplateString + String.join( "\n", classpathStrings );
		Util.writeStringToPath( standardClassPathString, woa.unixPath().resolve( "UNIXClassPath.txt" ) );
		Util.writeStringToPath( standardClassPathString, woa.macosPath().resolve( "MacOSClassPath.txt" ) );
		Util.writeStringToPath( standardClassPathString, woa.macosPath().resolve( "MacOSXServerClassPath.txt" ) );

		final String windowsClassPathString = classPathFileTemplateString + String.join( "\r\n", classpathStrings ).replace( "/", "\\" ); //CHECKME: Nice pretzels. We can make this more understandable // Hugi 2021-07-08
		Util.writeStringToPath( windowsClassPathString, woa.windowsPath().resolve( "CLSSPATH.TXT" ) );

		// CHECKME: I have no idea what the subpaths file does. Ditch it? // Hugi 2021-07-08
		final String windowsSubPathsString = Util.readTemplate( "subpaths" );
		Util.writeStringToPath( windowsSubPathsString, woa.windowsPath().resolve( "SUBPATHS.TXT" ) );

		// CHECKME: Fugly template implementation, beautify // Hugi 2021-07-08
		String infoPlistString = Util.readTemplate( "info-plist" );
		infoPlistString = infoPlistString.replace( "${NSExecutable}", applicationName );
		infoPlistString = infoPlistString.replace( "${CFBundleExecutable}", applicationName );
		infoPlistString = infoPlistString.replace( "${CFBundleShortVersionString}", mavenProject.getVersion() );
		infoPlistString = infoPlistString.replace( "${CFBundleVersion}", mavenProject.getVersion() );
		infoPlistString = infoPlistString.replace( "${NSJavaPath}", appJarFilename );
		infoPlistString = infoPlistString.replace( "${NSJavaPathClient}", appJarFilename );
		Util.writeStringToPath( infoPlistString, woa.contentsPath().resolve( "Info.plist" ) );

		// Create the executable script for UNIX
		final String unixLaunchScriptString = Util.readTemplate( "launch-script" );
		final Path unixLaunchScriptPath = woa.woaPath().resolve( applicationName );
		Util.writeStringToPath( unixLaunchScriptString, unixLaunchScriptPath );
		Util.makeUserExecutable( unixLaunchScriptPath );

		// CHECKME: For some reason, Contents/MacOS contains an exact copy of the launch script from the WOA root // Hugi 2021-07-08
		final Path redundantMacOSLaunchScriptPath = woa.macosPath().resolve( applicationName );
		Util.writeStringToPath( unixLaunchScriptString, redundantMacOSLaunchScriptPath );
		Util.makeUserExecutable( redundantMacOSLaunchScriptPath );

		// Create the executable script for Windows
		final String windowsLaunchScriptString = Util.readTemplate( "launch-script-cmd" );
		final Path windowsLaunchScriptPath = woa.woaPath().resolve( applicationName + ".cmd" );
		Util.writeStringToPath( windowsLaunchScriptString, windowsLaunchScriptPath );
		Util.makeUserExecutable( windowsLaunchScriptPath );

		// CHECKME: And of course Contents/Windows contains an exact copy of the Windows script from the WOA root // Hugi 2021-07-08
		final Path redundantWindowsLaunchScriptPath = woa.windowsPath().resolve( applicationName + ".cmd" );
		Util.writeStringToPath( windowsLaunchScriptString, redundantWindowsLaunchScriptPath );
		Util.makeUserExecutable( redundantWindowsLaunchScriptPath );
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

		public Path macosPath() {
			return Util.folder( contentsPath().resolve( "MacOS" ) );
		}

		public Path unixPath() {
			return Util.folder( contentsPath().resolve( "UNIX" ) );
		}

		public Path windowsPath() {
			return Util.folder( contentsPath().resolve( "Windows" ) );
		}

		public Path resourcesPath() {
			return Util.folder( contentsPath().resolve( "Resources" ) );
		}

		public Path webServerResourcesPath() {
			return Util.folder( contentsPath().resolve( "WebServerResources" ) );
		}

		public Path javaPath() {
			return Util.folder( resourcesPath().resolve( "Java" ) );
		}
	}
}