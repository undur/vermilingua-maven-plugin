package vermilingua.maven;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import vermilingua.packaging.BuildProperties;
import vermilingua.packaging.Util;

/**
 * EXPERIMENTAL. Deploys the packaged application to a Linux machine over SSH, and runs it there as a systemd service.
 *
 * - Uploads the .woa with rsync into a new release folder. Files unchanged since the running release are hard linked
 *   on the server instead of uploaded, so a redeploy usually only sends the application's own jar.
 * - Installs a Java runtime on the machine, if missing (Eclipse Temurin, kept under ~/vermilingua/java)
 * - Points the 'current' symlink at the new release, writes the service unit and restarts the service.
 *
 * Uses the system's ssh and rsync commands, so your SSH keys, agent and ~/.ssh/config apply.
 * The SSH user must be able to sudo without a password (to manage the service).
 *
 * With deploy.exe.vm set, deploys to an exe.dev VM: creates the VM if it doesn't exist and points
 * it's HTTPS proxy at the application.
 *
 * Configured like the launch parameters — in build.properties, or overridden per-build with -D system properties:
 *
 *   deploy.host        SSH destination ([user@]host) to deploy to. Not needed with deploy.exe.vm
 *   deploy.exe.vm      Name of an exe.dev VM to deploy to (created if it doesn't exist)
 *   deploy.exe.public  'true' makes the VM's HTTPS URL public. Otherwise only people with access to the VM can open it
 *   deploy.appName     Name of the service and the folder on the machine (defaults to the bundle name)
 *   deploy.port        Port the application listens on (defaults to 8080)
 *   deploy.args        Additional arguments to pass to the application
 *   deploy.java        Java feature version to run on (defaults to the version Maven runs on)
 *
 * Usage: mvn vermilingua:ssh-deploy -Ddeploy.exe.vm=myvm
 */
@Mojo(name = "ssh-deploy", requiresProject = true)
@Execute(phase = LifecyclePhase.PACKAGE)
public class SshDeployMojo extends AbstractMojo {

	private static final String DEFAULT_PORT = "8080";

	/**
	 * Names of exe.dev VMs (and systemd services, which follow the same rule here)
	 */
	private static final Pattern SAFE_NAME = Pattern.compile( "[A-Za-z0-9][A-Za-z0-9_.-]*" );

	/**
	 * The maven project. This gets injected by Maven during the build
	 */
	@Parameter(property = "project", required = true, readonly = true)
	MavenProject mavenProject;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// In a reactor build the CLI goal visits every module; only an actual
		// app bundle is deployable, so anything else is quietly skipped.
		if( !"woapplication".equals( mavenProject.getPackaging() ) ) {
			getLog().info( "Skipping %s — packaging '%s' is not a woapplication".formatted( mavenProject.getArtifactId(), mavenProject.getPackaging() ) );
			return;
		}

		final String environment = System.getProperty( "build.env" );
		final BuildProperties buildProperties = BuildProperties.of( mavenProject.getBasedir().toPath(), environment, System.getProperties() );

		// The launch script is named after the bundle, while finalName only names the .woa folder itself (see RunMojo)
		final String bundleName = orDefault( buildProperties.projectName(), mavenProject.getName() );
		final String woaName = mavenProject.getBuild().getFinalName() + ".woa";
		final Path woaPath = Path.of( mavenProject.getBuild().getDirectory() ).resolve( woaName );

		if( !Files.isRegularFile( woaPath.resolve( bundleName ) ) ) {
			throw new MojoFailureException( "No launch script at %s — did packaging succeed?".formatted( woaPath.resolve( bundleName ) ) );
		}

		final String appName = orDefault( buildProperties.deployProperty( "appName" ), bundleName );
		final String port = orDefault( buildProperties.deployProperty( "port" ), DEFAULT_PORT );
		final String appArgs = orDefault( buildProperties.deployProperty( "args" ), "" );
		final String javaVersion = orDefault( buildProperties.deployProperty( "java" ), String.valueOf( Runtime.version().feature() ) );
		final String exeVM = buildProperties.deployProperty( "exe.vm" );
		final boolean exePublic = "true".equals( buildProperties.deployProperty( "exe.public" ) );

		requireSafe( "deploy.appName", appName );
		requireSafe( "deploy.java", javaVersion );

		if( !port.matches( "[0-9]+" ) ) {
			throw new MojoFailureException( "'deploy.port' must be a port number, not '%s'".formatted( port ) );
		}

		final String host;
		final List<String> sshOptions = new ArrayList<>();

		if( exeVM != null && !exeVM.isBlank() ) {
			requireSafe( "deploy.exe.vm", exeVM );
			host = exeVM + ".exe.xyz";

			// A VM we just created has a host key we've never seen
			sshOptions.add( "-o" );
			sshOptions.add( "StrictHostKeyChecking=accept-new" );

			if( createExeVMIfMissing( exeVM ) ) {
				awaitSSH( host, sshOptions );
			}
		}
		else {
			host = buildProperties.deployProperty( "host" );

			if( host == null || host.isBlank() ) {
				throw new MojoFailureException( "Nowhere to deploy to. Set 'deploy.host' (an SSH destination, [user@]host) or 'deploy.exe.vm' (an exe.dev VM name) in build.properties, or pass it with -D" );
			}
		}

		final String release = LocalDateTime.now().format( DateTimeFormatter.ofPattern( "yyyyMMdd-HHmmss" ) );
		final String appFolder = "vermilingua/apps/" + appName;
		final String releaseFolder = appFolder + "/releases/" + release;

		getLog().info( "Deploying %s to %s as release %s".formatted( appName, host, release ) );

		// Upload. Paths on the server are relative to the SSH user's home folder, and --link-dest relative to the folder being written
		run( "Creating release folder", ssh( host, sshOptions, "mkdir -p " + releaseFolder ), null );

		final List<String> rsync = new ArrayList<>( List.of( "rsync", "-az", "--delete", "--stats", "--link-dest=../../current" ) );
		rsync.add( "-e" );
		rsync.add( String.join( " ", ssh( "", sshOptions, null ) ).strip() );
		rsync.add( woaPath.toString() ); // No trailing slash, so the .woa folder itself is copied into the release folder
		rsync.add( host + ":" + releaseFolder + "/" );
		run( "Uploading " + woaName, rsync, null );

		// Activate
		final String script = activationScript( appName, serviceName( appName ), bundleName, woaName, release, port, javaVersion, appArgs );
		run( "Activating release " + release, ssh( host, sshOptions, "bash -s" ), script );

		if( exeVM != null && !exeVM.isBlank() ) {
			run( "Pointing https://%s.exe.xyz/ at port %s".formatted( exeVM, port ), List.of( "ssh", "exe.dev", "share", "port", exeVM, port ), null );

			if( exePublic ) {
				run( "Making https://%s.exe.xyz/ public".formatted( exeVM ), List.of( "ssh", "exe.dev", "share", "set-public", exeVM ), null );
			}

			getLog().info( "Deployed %s to https://%s.exe.xyz/".formatted( appName, exeVM ) );
		}
		else {
			getLog().info( "Deployed %s to %s, port %s".formatted( appName, host, port ) );
		}
	}

	/**
	 * Creates the exe.dev VM named [vm], if it doesn't exist
	 *
	 * @return true if the VM was created
	 */
	private boolean createExeVMIfMissing( final String vm ) throws MojoExecutionException, MojoFailureException {
		final String vms;

		try {
			vms = output( List.of( "ssh", "-o", "BatchMode=yes", "exe.dev", "ls", "--json" ) );
		}
		catch( final MojoFailureException e ) {
			throw new MojoFailureException( "Couldn't reach exe.dev over SSH. Run 'ssh exe.dev' once in a terminal to accept it's host key and check your account works, then try again", e );
		}

		// We only need to know if the VM exists, so we look for it's name rather than parse the JSON (and keep the plugin free of a JSON library)
		if( Pattern.compile( "\"vm_name\"\\s*:\\s*\"%s\"".formatted( Pattern.quote( vm ) ) ).matcher( vms ).find() ) {
			return false;
		}

		run( "Creating exe.dev VM " + vm, List.of( "ssh", "exe.dev", "new", "--name=" + vm, "--no-email" ), null );
		return true;
	}

	/**
	 * Waits for [host] to accept SSH connections, since a VM can take a moment to start after it's been created
	 */
	private void awaitSSH( final String host, final List<String> sshOptions ) throws MojoExecutionException, MojoFailureException {
		final List<String> command = new ArrayList<>( List.of( "ssh", "-o", "BatchMode=yes", "-o", "ConnectTimeout=5" ) );
		command.addAll( sshOptions );
		command.add( host );
		command.add( "true" );

		getLog().info( "Waiting for %s to accept SSH connections".formatted( host ) );

		for( int i = 0; i < 30; i++ ) {
			try {
				if( new ProcessBuilder( command ).redirectErrorStream( true ).redirectOutput( ProcessBuilder.Redirect.DISCARD ).start().waitFor() == 0 ) {
					return;
				}

				Thread.sleep( 2000 );
			}
			catch( final IOException e ) {
				throw new MojoExecutionException( "Failed to run ssh", e );
			}
			catch( final InterruptedException e ) {
				Thread.currentThread().interrupt();
				throw new MojoExecutionException( "Interrupted waiting for " + host, e );
			}
		}

		throw new MojoFailureException( "%s didn't accept SSH connections within a minute".formatted( host ) );
	}

	/**
	 * @return The script that activates the uploaded release on the server
	 */
	static String activationScript( final String appName, final String serviceName, final String bundleName, final String woaName, final String release, final String port, final String javaVersion, final String appArgs ) {
		return Util.readTemplate( "ssh-deploy-activate" )
				.replace( "${appName}", appName )
				.replace( "${serviceName}", serviceName )
				.replace( "${bundleName}", bundleName )
				.replace( "${woaName}", woaName )
				.replace( "${release}", release )
				.replace( "${port}", port )
				.replace( "${javaVersion}", javaVersion )
				.replace( "${appArgs}", appArgs );
	}

	/**
	 * @return The name of the systemd service running the app
	 */
	static String serviceName( final String appName ) {
		return "vermilingua-" + appName;
	}

	/**
	 * @return An ssh command line connecting to [host]. A null [remoteCommand] gives just the ssh command and options, for rsync's -e
	 */
	private static List<String> ssh( final String host, final List<String> sshOptions, final String remoteCommand ) {
		final List<String> command = new ArrayList<>();
		command.add( "ssh" );
		command.addAll( sshOptions );

		if( remoteCommand != null ) {
			command.add( host );
			command.add( remoteCommand );
		}

		return command;
	}

	/**
	 * Runs [command] with it's output streaming to the console. [stdin] is passed to the command's standard input, if not null
	 */
	private void run( final String description, final List<String> command, final String stdin ) throws MojoExecutionException, MojoFailureException {
		getLog().info( description );
		getLog().debug( String.join( " ", command ) );

		try {
			final ProcessBuilder builder = new ProcessBuilder( command ).redirectOutput( ProcessBuilder.Redirect.INHERIT ).redirectError( ProcessBuilder.Redirect.INHERIT );

			if( stdin == null ) {
				builder.redirectInput( ProcessBuilder.Redirect.INHERIT );
			}

			final Process process = builder.start();

			if( stdin != null ) {
				try( var in = process.getOutputStream() ) {
					in.write( stdin.getBytes( StandardCharsets.UTF_8 ) );
				}
			}

			final int exitCode = process.waitFor();

			if( exitCode != 0 ) {
				throw new MojoFailureException( "%s failed (%s exited with status %s)".formatted( description, command.get( 0 ), exitCode ) );
			}
		}
		catch( final IOException e ) {
			throw new MojoExecutionException( "%s failed. Are '%s' installed and on the PATH?".formatted( description, command.get( 0 ) ), e );
		}
		catch( final InterruptedException e ) {
			Thread.currentThread().interrupt();
			throw new MojoExecutionException( description + " was interrupted", e );
		}
	}

	/**
	 * @return The standard output of [command]
	 */
	private static String output( final List<String> command ) throws MojoExecutionException, MojoFailureException {
		try {
			final Process process = new ProcessBuilder( command ).redirectError( ProcessBuilder.Redirect.INHERIT ).start();
			final String output = new String( process.getInputStream().readAllBytes(), StandardCharsets.UTF_8 );

			if( process.waitFor() != 0 ) {
				throw new MojoFailureException( "'%s' failed".formatted( String.join( " ", command ) ) );
			}

			return output;
		}
		catch( final IOException e ) {
			throw new MojoExecutionException( "Failed to run " + command.get( 0 ), e );
		}
		catch( final InterruptedException e ) {
			Thread.currentThread().interrupt();
			throw new MojoExecutionException( "Interrupted running " + command.get( 0 ), e );
		}
	}

	private static void requireSafe( final String key, final String value ) throws MojoFailureException {
		if( !SAFE_NAME.matcher( value ).matches() ) {
			throw new MojoFailureException( "'%s' may only contain letters, digits, '.', '_' and '-', not '%s'".formatted( key, value ) );
		}
	}

	private static String orDefault( final String value, final String defaultValue ) {
		return value != null && !value.isBlank() ? value : defaultValue;
	}
}
