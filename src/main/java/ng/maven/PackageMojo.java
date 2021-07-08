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

	@Parameter(property = "project", required = true, readonly = true)
	MavenProject project;

	private String applicationName() {
		return project.getArtifactId();
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// This is the 'target' directory
		final Path buildPath = Paths.get( project.getBuild().getDirectory() );

		// This is the jar file resulting from the compilation of our application project (App.jar)
		final Path artifactPath = project.getArtifact().getFile().toPath();

		// This is the WOA bundle, the destination for our build
		final WOA woa = WOA.getAtPath( buildPath, applicationName() );

		// This is the name of the JAR file generated for the application. Lowercase application name with .jar appended.
		final String appJarFilename = project.getArtifact().getArtifactId().toLowerCase() + ".jar";

		// Copy the main jar to the woa
		Util.copyFile( artifactPath, woa.javaPath().resolve( appJarFilename ) );

		// Start working on that list of paths to add to classpath
		final List<String> stringsForClasspath = new ArrayList<>();

		// FIXME: Not a fan of don't like to using hardcoded strings to represent directory locations
		stringsForClasspath.add( "Contents/Resources/Java/" + appJarFilename );

		// Copy in the dependency jars
		@SuppressWarnings("unchecked")
		final Set<Artifact> artifacts = project.getArtifacts();

		for( final Artifact artifact : artifacts ) {
			getLog().debug( "Copying artifact: " + artifact );

			final Path artifactPathInRepository = artifact.getFile().toPath();
			final Path artifactFolderPath = Util.folder( woa.javaPath().resolve( artifact.getGroupId().replace( ".", "/" ) + "/" + artifact.getArtifactId() + "/" + artifact.getVersion() ) );
			final Path targetPath = artifactFolderPath.resolve( artifact.getFile().getName() );

			stringsForClasspath.add( targetPath.toString() );

			Util.copyFile( artifactPathInRepository, targetPath );
		}

		Util.copyContentsOfDirectoryToDirectory( project.getBasedir() + "/src/main/components", woa.resourcesPath().toString() );
		Util.copyContentsOfDirectoryToDirectory( project.getBasedir() + "/src/main/resources", woa.resourcesPath().toString() ); // FIXME: This should eventually be woresources
		Util.copyContentsOfDirectoryToDirectory( project.getBasedir() + "/src/main/webserver-resources", woa.webServerResourcesPath().toString() );

		Util.writeStringToPath( Util.template( "launch-script" ), woa.baseLaunchScriptPath() );
		Util.makeExecutable( woa.baseLaunchScriptPath() );

		Util.writeStringToPath( Util.template( "info-plist" ), woa.contentsPath().resolve( "Info.plist" ) );
		Util.writeStringToPath( Util.template( "classpath" ), woa.macosPath().resolve( "MacOSClassPath.txt" ) );
		Util.writeStringToPath( Util.template( "classpath" ), woa.unixPath().resolve( "UNIXClassPath.txt" ) );
		// FIXME: Add Windows classpath
	}

	/**
	 * Our in-memory representation of the WOA bundle
	 */
	public static class WOA {

		private final String _applicationName;

		private final Path _woaPath;

		/**
		 * @return The WOA bundle [applicationName].woa in [containingDirectory]
		 */
		public static WOA getAtPath( final Path containingDirectory, final String applicationName ) {
			Objects.requireNonNull( containingDirectory );
			Objects.requireNonNull( applicationName );
			final Path woaPath = containingDirectory.resolve( applicationName + ".woa" );
			return new WOA( woaPath, applicationName );
		}

		private WOA( final Path woaPath, final String applicationName ) {
			Objects.requireNonNull( woaPath );
			Objects.requireNonNull( applicationName );
			_woaPath = Util.folder( woaPath );
			_applicationName = applicationName;
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

		public Path baseLaunchScriptPath() {
			return woaPath().resolve( _applicationName );
		}
	}
}