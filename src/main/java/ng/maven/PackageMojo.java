package ng.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

		final Path buildPath = Paths.get( project.getBuild().getDirectory() );
		final Path woaPath = buildPath.resolve( applicationName() + ".woa" );
		final Path contentsPath = woaPath.resolve( "Contents" );
		final Path resourcesPath = contentsPath.resolve( "Resources" );
		final Path javaPath = resourcesPath.resolve( "Java" );

		try {
			Files.createDirectory( woaPath );
			Files.createDirectory( contentsPath );
			Files.createDirectory( resourcesPath );
			Files.createDirectory( javaPath );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}

		//
		// Copy in the main jar
		//
		try {
			Files.copy( project.getArtifact().getFile().toPath(), javaPath.resolve( project.getArtifact().getArtifactId() + ".jar" ) );
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
		}

		//
		// Copy in the dependency jars
		//
		@SuppressWarnings("unchecked")
		final Set<Artifact> artifacts = project.getArtifacts();

		final List<String> stringsForClasspath = new ArrayList<>();

		for( final Artifact artifact : artifacts ) {
			getLog().info( "Copying artifact: " + artifact );

			final Path artifactPathInRepository = artifact.getFile().toPath();

			final Path artifactFolderPath = javaPath.resolve( artifact.getGroupId().replace( ".", "/" ) + "/" + artifact.getVersion() );

			try {
				if( !Files.exists( artifactFolderPath ) ) {
					Files.createDirectories( artifactFolderPath );
				}
			}
			catch( final IOException e ) {
				throw new RuntimeException( e );
			}

			final Path targetPath = artifactFolderPath.resolve( artifact.getFile().getName() );
			System.out.println( "targetPath: " + targetPath );

			stringsForClasspath.add( targetPath.toString() );

			try {
				Files.copy( artifactPathInRepository, targetPath );
			}
			catch( final IOException e ) {
				throw new RuntimeException( e );
			}
		}

		stringsForClasspath.add( 0, "Contents/Resources/Java/ng-testapp.jar" );
		final String cpString = String.join( ":", stringsForClasspath );
		//
		// Create the executable
		//
		final StringBuilder b = new StringBuilder();
		b.append( "java -cp " + cpString + " ng.testapp.Application" );
		try {
			final Path executable = woaPath.resolve( project.getArtifactId() );
			Files.write( executable, b.toString().getBytes() );
			final Set<PosixFilePermission> perms = new HashSet<>();
			perms.add( PosixFilePermission.OWNER_READ );
			perms.add( PosixFilePermission.OWNER_WRITE );
			perms.add( PosixFilePermission.OWNER_EXECUTE );
			perms.add( PosixFilePermission.GROUP_READ );
			perms.add( PosixFilePermission.OTHERS_READ );
			Files.setPosixFilePermissions( executable, perms );
		}
		catch( final IOException e ) {
			e.printStackTrace();
		}
	}
}