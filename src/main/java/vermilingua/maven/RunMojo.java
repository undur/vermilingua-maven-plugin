package vermilingua.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import vermilingua.packaging.BuildProperties;

/**
 * Runs the packaged application by executing the launch script inside the
 * built .woa — the artifact runs exactly as it would when deployed (launch
 * script, config.txt, classpath.txt, resources resolved from the bundle).
 *
 * Invoking the goal forks the build lifecycle through 'package' first, so
 * running an application is a single command:
 *
 *   mvn vermilingua:run
 *
 * The application's output streams to the Maven console and Ctrl-C stops
 * both. Arguments are passed with run.args (split on whitespace), and reach
 * the application — or the launch script's own -launch.* handling:
 *
 *   mvn vermilingua:run -Drun.args="-WOPort 1200"
 */
@Mojo(name = "run", requiresProject = true)
@Execute(phase = LifecyclePhase.PACKAGE)
public class RunMojo extends AbstractMojo {

	/**
	 * The maven project. This gets injected by Maven during the build
	 */
	@Parameter(property = "project", required = true, readonly = true)
	MavenProject mavenProject;

	/**
	 * Arguments passed to the launch script (and on to the application), split on whitespace
	 */
	@Parameter(property = "run.args", required = false)
	String runArgs;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// In a reactor build the CLI goal visits every module; only an actual
		// app bundle is runnable, so anything else is quietly skipped.
		if( !"woapplication".equals( mavenProject.getPackaging() ) ) {
			getLog().info( "Skipping %s — packaging '%s' is not a woapplication".formatted( mavenProject.getArtifactId(), mavenProject.getPackaging() ) );
			return;
		}

		// The launch script is named after the bundle (project.name in
		// build.properties, falling back to the Maven project name), while
		// finalName only names the .woa folder itself.
		final String environment = System.getProperty( "build.env" );
		final BuildProperties buildProperties = BuildProperties.of( mavenProject.getBasedir().toPath(), environment, System.getProperties() );
		final String bundleName = orDefault( buildProperties.projectName(), mavenProject.getName() );

		final String finalName = mavenProject.getBuild().getFinalName();
		final Path woaPath = Path.of( mavenProject.getBuild().getDirectory() ).resolve( finalName + ".woa" );
		final Path launchScriptPath = woaPath.resolve( bundleName );

		if( !Files.isRegularFile( launchScriptPath ) ) {
			throw new MojoFailureException( "No launch script at %s — did packaging succeed?".formatted( launchScriptPath ) );
		}

		final List<String> command = new ArrayList<>();
		command.add( launchScriptPath.toAbsolutePath().toString() );

		if( runArgs != null && !runArgs.isBlank() ) {
			for( final String arg : runArgs.strip().split( "\\s+" ) ) {
				command.add( arg );
			}
		}

		getLog().info( "Running %s — Ctrl-C to stop".formatted( launchScriptPath ) );

		try {
			final Process process = new ProcessBuilder( command )
					.inheritIO()
					.start();

			// If Maven goes down (Ctrl-C included), take the application with it
			Runtime.getRuntime().addShutdownHook( new Thread( process::destroy ) );

			final int exitCode = process.waitFor();

			if( exitCode != 0 ) {
				throw new MojoFailureException( "%s exited with status %s".formatted( bundleName, exitCode ) );
			}
		}
		catch( final IOException e ) {
			throw new MojoExecutionException( "Failed to run " + launchScriptPath, e );
		}
		catch( final InterruptedException e ) {
			Thread.currentThread().interrupt();
			throw new MojoExecutionException( "Run of " + bundleName + " was interrupted", e );
		}
	}

	private static String orDefault( final String value, final String defaultValue ) {
		return value != null && !value.isBlank() ? value : defaultValue;
	}
}
