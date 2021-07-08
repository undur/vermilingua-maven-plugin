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

		// Copy in the main jar
		try {
			Files.copy( artifactPath, woa.javaPath().resolve( project.getArtifact().getArtifactId() + ".jar" ) );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}

		// Copy in the dependency jars
		@SuppressWarnings("unchecked")
		final Set<Artifact> artifacts = project.getArtifacts();

		final List<String> stringsForClasspath = new ArrayList<>();

		for( final Artifact artifact : artifacts ) {
			getLog().debug( "Copying artifact: " + artifact );

			final Path artifactPathInRepository = artifact.getFile().toPath();

			final Path artifactFolderPath = woa.javaPath().resolve( artifact.getGroupId().replace( ".", "/" ) + "/" + artifact.getVersion() );

			try {
				if( !Files.exists( artifactFolderPath ) ) {
					Files.createDirectories( artifactFolderPath );
				}
			}
			catch( final IOException e ) {
				throw new RuntimeException( e );
			}

			final Path targetPath = artifactFolderPath.resolve( artifact.getFile().getName() );

			stringsForClasspath.add( targetPath.toString() );

			try {
				Files.copy( artifactPathInRepository, targetPath );
			}
			catch( final IOException e ) {
				throw new RuntimeException( e );
			}
		}

		stringsForClasspath.add( 0, "Contents/Resources/Java/ng-testapp.jar" );

		writeToPath( baseLaunchScript(), woa.baseLaunchScriptPath() );
		makeExecutable( woa.baseLaunchScriptPath() );
	}

	private String baseLaunchScript() {
		return template( "launch-script" );
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

		public Path resourcesPath() {
			return folder( contentsPath().resolve( "Resources" ) );
		}

		public Path javaPath() {
			return folder( resourcesPath().resolve( "Java" ) );
		}

		public Path baseLaunchScriptPath() {
			return woaPath().resolve( _applicationName );
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
					Files.createDirectory( path );
				}
				catch( final IOException e ) {
					throw new RuntimeException( e );
				}
			}

			return path;
		}
	}
}