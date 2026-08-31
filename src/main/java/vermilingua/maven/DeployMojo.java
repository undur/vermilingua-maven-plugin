package vermilingua.maven;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import vermilingua.packaging.BuildProperties;
import vermilingua.packaging.Util;

/**
 * Deploys the packaged application through JavaMonitor's admin/deploy:
 * the .woa is tarred and POSTed to the monitor, which hands it to the
 * wotaskd on every host the app has instances on — each swaps the bundle
 * into place and bounces its local instances.
 *
 * Configured like the launch parameters — in build.properties, or
 * overridden per-build with -D system properties:
 *
 *   deploy.appName       the name JavaMonitor knows the app by (defaults to the bundle name)
 *   deploy.monitorHost   the JavaMonitor host, host or host:port (port defaults to 56789)
 *   deploy.password      the monitor password (typically NOT in build.properties —
 *                        pass -Ddeploy.password=... so public repos carry no secrets)
 *
 * Usage: mvn package vermilingua:deploy -Ddeploy.password=...
 */
@Mojo(name = "deploy", requiresProject = true, threadSafe = true)
public class DeployMojo extends AbstractMojo {

	private static final int DEFAULT_MONITOR_PORT = 56789;

	/**
	 * The maven project. This gets injected by Maven during the build
	 */
	@Parameter(property = "project", required = true, readonly = true)
	MavenProject mavenProject;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Same property sources and layering as the package goal's launch parameters
		final String environment = System.getProperty( "build.env" );
		final BuildProperties buildProperties = BuildProperties.of( mavenProject.getBasedir().toPath(), environment, System.getProperties() );

		final String finalName = mavenProject.getBuild().getFinalName();
		final String appName = orDefault( buildProperties.deployProperty( "appName" ), finalName );
		final String monitorHost = buildProperties.deployProperty( "monitorHost" );
		final String password = buildProperties.deployProperty( "password" );

		if( monitorHost == null || monitorHost.isBlank() ) {
			throw new MojoFailureException( "No monitor to deploy to. Set 'deploy.monitorHost' in build.properties or pass -Ddeploy.monitorHost=<host[:port]> (port defaults to " + DEFAULT_MONITOR_PORT + ")" );
		}

		final Path targetPath = Path.of( mavenProject.getBuild().getDirectory() );
		final Path woa = targetPath.resolve( finalName + ".woa" );

		if( !Files.isDirectory( woa ) ) {
			throw new MojoFailureException( "No application bundle at %s — package first: mvn package vermilingua:deploy".formatted( woa ) );
		}

		final Path archive = targetPath.resolve( finalName + ".woapplication-deploy.tar.gz" );
		Util.createTarGz( woa, archive );

		final String hostAndPort = monitorHost.contains( ":" ) ? monitorHost : monitorHost + ":" + DEFAULT_MONITOR_PORT;

		// FIXME: The password travels as a query parameter because that's what JavaMonitor's
		// admin actions read — and query strings end up in access logs when the request passes
		// a front end. Teach admin/deploy to accept it as a header, then prefer that here // Hugi 2026-08-31
		String url = "http://%s/Apps/WebObjects/JavaMonitor.woa/admin/deploy?type=app&name=%s".formatted( hostAndPort, URLEncoder.encode( appName, StandardCharsets.UTF_8 ) );

		if( password != null && !password.isBlank() ) {
			url += "&pw=" + URLEncoder.encode( password, StandardCharsets.UTF_8 );
		}

		try {
			getLog().info( "Deploying %s (%,d bytes) to %s".formatted( appName, Files.size( archive ), hostAndPort ) );

			final HttpRequest request = HttpRequest.newBuilder()
					.uri( URI.create( url ) )
					.header( "Content-Type", "application/octet-stream" )
					.timeout( Duration.ofMinutes( 5 ) )
					.POST( HttpRequest.BodyPublishers.ofFile( archive ) )
					.build();

			final HttpResponse<String> response = HttpClient.newHttpClient().send( request, HttpResponse.BodyHandlers.ofString() );

			for( final String line : response.body().strip().split( "\n" ) ) {
				getLog().info( "  " + line );
			}

			if( response.statusCode() != 200 ) {
				throw new MojoFailureException( "Deploying %s failed: HTTP %s from %s".formatted( appName, response.statusCode(), hostAndPort ) );
			}

			getLog().info( "Deployed " + appName );
		}
		catch( final IOException e ) {
			throw new MojoExecutionException( "Deploying " + appName + " failed", e );
		}
		catch( final InterruptedException e ) {
			Thread.currentThread().interrupt();
			throw new MojoExecutionException( "Deploying " + appName + " was interrupted", e );
		}
	}

	private static String orDefault( final String value, final String defaultValue ) {
		return value != null && !value.isBlank() ? value : defaultValue;
	}
}
