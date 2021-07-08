package ng.maven;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class PackageMojo extends AbstractMojo {

	/**
	 * The maven project. This gets injected by Maven during the build
	 */
	@Parameter(property = "project", required = true, readonly = true)
	MavenProject project;

	/**
	 * Allows the user to specify an alternative name for the WO bundle resources folder (probably "resources")
	 *
	 * CHECKME: I'd prefer not to include this and just standardize on the new/correct bundle layout with a separate "woresources" folder  // Hugi 2021-07-08
	 */
	@Parameter(property = "woresourcesFolderName", required = false, defaultValue = "woresources")
	String woresourcesFolderName;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Usually Maven's standard 'target' directory
		final Path buildPath = Paths.get( project.getBuild().getDirectory() );

		// The jar file resulting from the compilation of our application project (App.jar)
		final Path artifactPath = project.getArtifact().getFile().toPath();

		// The name of the application, gotten from the artifactId
		final String applicationName = project.getArtifactId();

		// The WOA bundle, the destination for our build. Bundle gets named after the app's artifactId
		final WOA woa = WOA.create( buildPath, applicationName );

		// The eventual name of the app's JAR file. Lowercase app name with .jar appended.
		// CHECKME: I'm not sure why they chose to lowercase the JAR name. It seems totally unnecessary // Hugi 2021-07-08
		final String appJarFilename = project.getArtifact().getArtifactId().toLowerCase() + ".jar";

		// Copy the app jar to the woa
		Util.copyFile( artifactPath, woa.javaPath().resolve( appJarFilename ) );

		// Start working on that list of jars to add to the classpath
		final List<String> classpathStrings = new ArrayList<>();

		// CHECKME: For some reason the older plugin includes the java folder itself on the classpath. Better replicate that for now, check later // Hugi 2021-07-08
		classpathStrings.add( "Contents/Resources/Java/" );

		// CHECKME: Not a fan of using hardcoded folder names // Hugi 2021-07-08
		classpathStrings.add( "Contents/Resources/Java/" + appJarFilename );

		// Copy the app's resolved dependencies (direct and transient) to the WOA
		for( final Artifact artifact : (Set<Artifact>)project.getArtifacts() ) {
			final Path artifactPathInMavenRepository = artifact.getFile().toPath();
			final Path artifactFolderPathInWOA = Util.folder( woa.javaPath().resolve( artifact.getGroupId().replace( ".", "/" ) + "/" + artifact.getArtifactId() + "/" + artifact.getVersion() ) );
			final Path artifactPathInWOA = artifactFolderPathInWOA.resolve( artifact.getFile().getName() );
			Util.copyFile( artifactPathInMavenRepository, artifactPathInWOA );

			// Add the jar to the classpath
			classpathStrings.add( artifactPathInWOA.toString() );
		}

		// Copy WebServerResources from framework jars to the WOA
		for( final Artifact artifact : (Set<Artifact>)project.getArtifacts() ) {
			if( Util.containsWebServerResources( artifact.getFile() ) ) {
				final Path destinationPath = woa.contentsPath().resolve( "Frameworks" ).resolve( artifact.getArtifactId() + ".framework" );
				Util.copyFolderFromJarToPath( "WebServerResources", artifact.getFile(), destinationPath );
			}
		}

		Util.copyContentsOfDirectoryToDirectory( project.getBasedir() + "/src/main/components", woa.resourcesPath().toString() );
		// FIXME: Flatten components  // Hugi 2021-07-08
		Util.copyContentsOfDirectoryToDirectory( project.getBasedir() + "/src/main/" + woresourcesFolderName, woa.resourcesPath().toString() );
		// FIXME: Flatten resources (?)  // Hugi 2021-07-08
		Util.copyContentsOfDirectoryToDirectory( project.getBasedir() + "/src/main/webserver-resources", woa.webServerResourcesPath().toString() );

		Util.writeStringToPath( Util.readTemplate( "info-plist" ), woa.contentsPath().resolve( "Info.plist" ) );

		// The classpath files for MacOS, MacOSXServer and UNIX all look the same
		// CHECKME: MacOS, UNIX and MacOS X Server (Rhapsody?)... There be redundancies // Hugi 2021-07-08
		final String standardClassPathString = Util.readTemplate( "classpath" );
		Util.writeStringToPath( standardClassPathString, woa.macosPath().resolve( "MacOSClassPath.txt" ) );
		Util.writeStringToPath( Util.readTemplate( "classpath" ), woa.macosPath().resolve( "MacOSXServerClassPath.txt" ) );
		Util.writeStringToPath( Util.readTemplate( "classpath" ), woa.unixPath().resolve( "UNIXClassPath.txt" ) );
		// FIXME: Add Windows classpath // Hugi 2021-07-08

		// FIXME: Generate Info.plist // Hugi 2021-07-08

		// Create the executable script for UNIX
		final String unixLaunchScriptString = Util.readTemplate( "launch-script" );
		final Path unixLaunchScriptPath = woa.woaPath().resolve( applicationName );
		Util.writeStringToPath( unixLaunchScriptString, unixLaunchScriptPath );
		Util.makeUserExecutable( unixLaunchScriptPath );

		// CHECKME: For some reason, Contents/MacOS contains an exact copy of the launch script from the WOA root // Hugi 2021-07-08
		final Path redundantMacOSLaunchScriptString = woa.macosPath().resolve( applicationName );
		Util.writeStringToPath( unixLaunchScriptString, redundantMacOSLaunchScriptString );
		Util.makeUserExecutable( redundantMacOSLaunchScriptString );

		// Create the executable script for Windows
		final String windowsLaunchScriptString = Util.readTemplate( "launch-script-cmd" );
		final Path windowsLaunchScriptPath = woa.woaPath().resolve( applicationName + ".cmd" );
		Util.writeStringToPath( windowsLaunchScriptString, windowsLaunchScriptPath );
		Util.makeUserExecutable( windowsLaunchScriptPath );

		// CHECKME: And of course Contents/Windows contains an exact copy of the Windows script from the WOA root // Hugi 2021-07-08
		final Path redundantWindowsLaunchScriptPath = woa.windowsPath().resolve( applicationName );
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