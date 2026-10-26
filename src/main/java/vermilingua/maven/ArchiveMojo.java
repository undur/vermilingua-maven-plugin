package vermilingua.maven;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import vermilingua.packaging.Util;

/**
 * Makes the products built from an application's .woa: the split webserver resources folder (performSplit),
 * and tar.gz archives attached as the build's artifacts (createArchives).
 *
 * Bound to pre-integration-test, so anything bound to package can still work on the .woa before it's split and archived.
 * Doesn't build the .woa itself. Invoked directly, run it after package: mvn package vermilingua:archive
 */
@Mojo(name = "archive", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, threadSafe = true)
public class ArchiveMojo extends AbstractMojo {

	/**
	 * The maven project. This gets injected by Maven during the build
	 */
	@Parameter(property = "project", required = true, readonly = true)
	MavenProject mavenProject;

	/**
	 * Indicates that we want to extract webserver resources (for both the app and it's included frameworks)
	 * to a separate folder alongside the WOA (for installation on a web server)
	 */
	@Parameter(property = "performSplit", required = false)
	boolean performSplit;

	/**
	 * Creates tar.gz archives of the build products and attaches them as Maven artifacts,
	 * making them available to mvn install and mvn deploy.
	 */
	@Parameter(property = "createArchives", required = false)
	boolean createArchives;

	@Inject
	MavenProjectHelper projectHelper;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if( !"woapplication".equals( mavenProject.getPackaging() ) ) {
			getLog().info( "Skipping %s — packaging '%s' is not a woapplication".formatted( mavenProject.getArtifactId(), mavenProject.getPackaging() ) );
			return;
		}

		if( !performSplit && !createArchives ) {
			getLog().info( "Nothing to do: performSplit and createArchives are both off" );
			return;
		}

		final String finalName = mavenProject.getBuild().getFinalName();
		final Path targetPath = Path.of( mavenProject.getBuild().getDirectory() ); // Maven's target directory
		final Path woaPath = targetPath.resolve( finalName + ".woa" );

		if( !Files.isDirectory( woaPath ) ) {
			throw new MojoFailureException( "No application bundle at %s — package first: mvn package vermilingua:archive".formatted( woaPath ) );
		}

		final Path splitPath = splitPath( woaPath );

		if( performSplit ) {
			extractWebServerResources( woaPath, splitPath );
		}

		if( createArchives ) {
			createAndAttachArchives( woaPath, splitPath, finalName, targetPath );
		}
	}

	/**
	 * @return The split webserver resources folder for the WOA at [woaPath]: App.woa.webserverresources, beside it
	 */
	private static Path splitPath( final Path woaPath ) {
		return woaPath.resolveSibling( woaPath.getFileName() + ".webserverresources" );
	}

	/**
	 * Copies the folders:
	 *
	 *  - App.woa/Contents/WebServerResources
	 *  - App.woa/Contents/Frameworks
	 *
	 *  from the WOA into a new directory created adjacent to the WOA
	 *
	 *  - App.woa.webserverresources
	 *
	 * The folder starts empty, so it doesn't keep resources that have since been removed from the WOA.
	 */
	private static void extractWebServerResources( final Path woaPath, final Path splitPath ) {
		Util.deleteRecursively( splitPath );

		final Path splitWebServerResourcesPath = Util.folder( splitPath.resolve( "Contents" ).resolve( "WebServerResources" ) );
		final Path splitFrameworksPath = Util.folder( splitPath.resolve( "Contents" ).resolve( "Frameworks" ) );

		copyIfPresent( woaPath.resolve( "Contents" ).resolve( "WebServerResources" ), splitWebServerResourcesPath );
		copyIfPresent( woaPath.resolve( "Contents" ).resolve( "Frameworks" ), splitFrameworksPath );
	}

	private static void copyIfPresent( final Path sourceDirectory, final Path destinationDirectory ) {
		if( Files.isDirectory( sourceDirectory ) ) {
			Util.copyContentsOfDirectoryToDirectory( sourceDirectory, destinationDirectory );
		}
	}

	/**
	 * Creates tar.gz archives of the WOA (and optionally the split webserver resources)
	 * and attaches them as Maven artifacts.
	 */
	private void createAndAttachArchives( final Path woaPath, final Path splitPath, final String finalName, final Path targetPath ) {
		// Archive the .woa bundle
		final Path woaArchive = targetPath.resolve( finalName + ".woapplication.tar.gz" );
		getLog().info( "Creating " + woaArchive.getFileName() );
		Util.createTarGz( woaPath, woaArchive );

		// Set as primary artifact
		final DefaultArtifactHandler handler = new DefaultArtifactHandler( "woapplication.tar.gz" );
		final DefaultArtifact artifact = new DefaultArtifact(
				mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion(),
				null, "woapplication.tar.gz", null, handler );
		artifact.setFile( woaArchive.toFile() );
		mavenProject.setArtifact( artifact );

		// If split was performed, archive the webserver resources too. Named and laid out like wolifecycle's archive, for deployment scripts written for it:
		// deployed as [artifactId]-[version].wowebserverresources.tar.gz (the extension declared for the type in components.xml), unpacking to [finalName].woa
		if( performSplit && Files.isDirectory( splitPath ) ) {
			final Path wsrArchive = targetPath.resolve( finalName + ".wowebserverresources.tar.gz" );
			getLog().info( "Creating " + wsrArchive.getFileName() );
			Util.createTarGz( splitPath, woaPath.getFileName().toString(), wsrArchive );
			projectHelper.attachArtifact( mavenProject, "wowebserverresources.tar.gz", null, wsrArchive.toFile() );
		}
	}
}
