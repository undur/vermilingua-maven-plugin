package ng.maven;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
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

		// Start working on that list of paths to add to classpath
		final List<String> stringsForClasspath = new ArrayList<>();

		// This is the name of the JAR file generated for the application
		final String appJarFilename = project.getArtifact().getArtifactId().toLowerCase() + ".jar";

		// Copy the main jar to the woa
		try {
			Files.copy( artifactPath, woa.javaPath().resolve( appJarFilename ) );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}

		// FIXME: I don't like to use strings to represent locations in the tree
		stringsForClasspath.add( "Contents/Resources/Java/" + appJarFilename );

		// Copy in the dependency jars
		@SuppressWarnings("unchecked")
		final Set<Artifact> artifacts = project.getArtifacts();

		for( final Artifact artifact : artifacts ) {
			getLog().debug( "Copying artifact: " + artifact );

			final Path artifactPathInRepository = artifact.getFile().toPath();

			final Path artifactFolderPath = folder( woa.javaPath().resolve( artifact.getGroupId().replace( ".", "/" ) + "/" + artifact.getArtifactId() + "/" + artifact.getVersion() ) );

			final Path targetPath = artifactFolderPath.resolve( artifact.getFile().getName() );

			stringsForClasspath.add( targetPath.toString() );

			try {
				Files.copy( artifactPathInRepository, targetPath );
			}
			catch( final IOException e ) {
				throw new RuntimeException( e );
			}
		}

		copyDirectory( project.getBasedir() + "/src/main/components", woa.resourcesPath().toString() );
		copyDirectory( project.getBasedir() + "/src/main/resources", woa.resourcesPath().toString() ); // FIXME: This should eventually be woresources
		copyDirectory( project.getBasedir() + "/src/main/webserver-resources", woa.webServerResourcesPath().toString() );

		writeToPath( template( "launch-script" ), woa.baseLaunchScriptPath() );
		makeExecutable( woa.baseLaunchScriptPath() );

		writeToPath( template( "info-plist" ), woa.contentsPath().resolve( "Info.plist" ) );
		writeToPath( template( "classpath" ), woa.macosPath().resolve( "MacOSClassPath.txt" ) );
		writeToPath( template( "classpath" ), woa.unixPath().resolve( "UNIXClassPath.txt" ) );
		// FIXME: Add Windows classpath
	}

	/**
	 * FIXME: Change to accept Paths as parameters
	 */
	private static void copyDirectory( String sourceDirectoryLocation, String destinationDirectoryLocation ) {
		try {
			Files.walk( Paths.get( sourceDirectoryLocation ) )
					.forEach( source -> {
						final Path destination = Paths.get( destinationDirectoryLocation, source.toString().substring( sourceDirectoryLocation.length() ) );
						try {
							if( !Files.exists( destination ) ) { // FIXME: This is just a hackyhack
								Files.copy( source, destination );
							}
						}
						catch( final IOException e ) {
							throw new RuntimeException( e );
						}
					} );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}
	}

	private static void writeToPath( final String string, final Path path ) {
		try {
			Files.write( path, string.getBytes( StandardCharsets.UTF_8 ) );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}
	}

	private static void makeExecutable( final Path path ) {
		Objects.requireNonNull( path );

		try {
			final Set<PosixFilePermission> perms = new HashSet<>();
			perms.add( PosixFilePermission.OWNER_READ );
			perms.add( PosixFilePermission.OWNER_WRITE );
			perms.add( PosixFilePermission.OWNER_EXECUTE );
			perms.add( PosixFilePermission.GROUP_READ );
			perms.add( PosixFilePermission.OTHERS_READ );
			Files.setPosixFilePermissions( path, perms );
		}
		catch( final IOException e ) {
			e.printStackTrace();
		}
	}

	private static String template( final String name ) {
		try( InputStream stream = PackageMojo.class.getResourceAsStream( "/scripts/" + name + ".template" )) {
			return new String( b( stream ), StandardCharsets.UTF_8 );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}
	}

	private static byte[] b( InputStream is ) {
		try {
			final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			final byte[] data = new byte[16384];

			while( (nRead = is.read( data, 0, data.length )) != -1 ) {
				buffer.write( data, 0, nRead );
			}

			return buffer.toByteArray();
		}
		catch( final Exception e ) {
			throw new RuntimeException( e );
		}
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
			_woaPath = folder( woaPath );
			_applicationName = applicationName;
		}

		public Path woaPath() {
			return _woaPath;
		}

		public Path contentsPath() {
			return folder( woaPath().resolve( "Contents" ) );
		}

		public Path macosPath() {
			return folder( contentsPath().resolve( "MacOS" ) );
		}

		public Path unixPath() {
			return folder( contentsPath().resolve( "UNIX" ) );
		}

		public Path windowsPath() {
			return folder( contentsPath().resolve( "Windows" ) );
		}

		public Path resourcesPath() {
			return folder( contentsPath().resolve( "Resources" ) );
		}

		public Path webServerResourcesPath() {
			return folder( contentsPath().resolve( "WebServerResources" ) );
		}

		public Path javaPath() {
			return folder( resourcesPath().resolve( "Java" ) );
		}

		public Path baseLaunchScriptPath() {
			return woaPath().resolve( _applicationName );
		}
	}

	/**
	 * @return The folder at the given path. Creates the folder if missing, throws an exception if the path exists but is not a folder.
	 */
	private static Path folder( final Path path ) {
		if( Files.exists( path ) ) {
			if( !Files.isDirectory( path ) ) {
				throw new IllegalArgumentException( "Given folder path exists but is not a folder" );
			}
		}
		else {
			try {
				Files.createDirectories( path );
			}
			catch( final IOException e ) {
				throw new RuntimeException( e );
			}
		}

		return path;
	}
}