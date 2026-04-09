package vermilingua.maven;

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
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import vermilingua.packaging.ArchiveUtil;
import vermilingua.packaging.BuildProperties;
import vermilingua.packaging.PackageWOApplication;
import vermilingua.packaging.PackageWOApplication.WOA;
import vermilingua.packaging.PackageWOFramework;
import vermilingua.packaging.SourceProject;
import vermilingua.packaging.Util;

@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class PackageMojo extends AbstractMojo {

	/**
	 * The maven project. This gets injected by Maven during the build
	 */
	@Parameter(property = "project", required = true, readonly = true)
	MavenProject project;

	/**
	 * Allows the user to specify an alternative name for the source project's WO bundle resources folder (probably "resources", since that's the old wolifecycle default)
	 */
	@Parameter(property = "woresourcesFolderName", required = false, defaultValue = SourceProject.DEFAULT_WORESOURCES_FOLDER_NAME)
	String woresourcesFolderName;

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

	/**
	 * Entry point for the assembly process
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if( !woresourcesFolderName.equals( SourceProject.DEFAULT_WORESOURCES_FOLDER_NAME ) ) {
			getLog().warn( String.format( "Using non-standard woresources folder name '%s'. Using the standard name '%s' is recommended", woresourcesFolderName, SourceProject.DEFAULT_WORESOURCES_FOLDER_NAME ) );
		}

		final String environment = System.getProperty( "build.env" );
		final BuildProperties buildProperties = BuildProperties.of( project.getBasedir().toPath(), environment, System.getProperties() );
		final SourceProject sourceProject = new SourceProject( project, woresourcesFolderName, buildProperties );

		switch( sourceProject.type() ) {
			case Application -> {
				final String finalName = project.getBuild().getFinalName();
				final Path targetPath = Path.of( project.getBuild().getDirectory() ); // Maven's target directory

				final WOA woa = new PackageWOApplication().execute( sourceProject, finalName, targetPath );

				if( performSplit ) {
					extractWebServerResources( woa );
				}

				if( createArchives ) {
					createAndAttachArchives( woa, finalName, targetPath );
				}
			}
			case Framework -> {
				new PackageWOFramework().execute( sourceProject );
			}
		}
	}

	/**
	 * Creates tar.gz archives of the WOA (and optionally the split webserver resources)
	 * and attaches them as Maven artifacts.
	 */
	private void createAndAttachArchives( final WOA woa, final String finalName, final Path targetPath ) {
		// Archive the .woa bundle
		final Path woaArchive = targetPath.resolve( finalName + ".woapplication.tar.gz" );
		getLog().info( "Creating " + woaArchive.getFileName() );
		ArchiveUtil.createTarGz( woa.woaPath(), woaArchive );

		// Set as primary artifact
		final DefaultArtifactHandler handler = new DefaultArtifactHandler( "woapplication.tar.gz" );
		final DefaultArtifact artifact = new DefaultArtifact(
			project.getGroupId(), project.getArtifactId(), project.getVersion(),
			null, "woapplication.tar.gz", null, handler
		);
		artifact.setFile( woaArchive.toFile() );
		project.setArtifact( artifact );

		// If split was performed, archive the webserver resources too
		if( performSplit ) {
			final Path splitPath = woa.woaPath().getParent().resolve( woa.woaPath().getFileName() + ".webserverresources" );
			if( splitPath.toFile().isDirectory() ) {
				final Path wsrArchive = targetPath.resolve( finalName + ".wowebserverresources.tar.gz" );
				getLog().info( "Creating " + wsrArchive.getFileName() );
				ArchiveUtil.createTarGz( splitPath, wsrArchive );
				projectHelper.attachArtifact( project, "tar.gz", "wowebserverresources", wsrArchive.toFile() );
			}
		}
	}

	/**
	 * Once the build is completed, copies the folders:
	 *
	 *  - App.woa/WebServerResources
	 *  - App.woa/Frameworks
	 *
	 *  from the build product and places it in a new directory (Alongside the woa)
	 *
	 *  - App.woa.webserverresources
	 */
	private static void extractWebServerResources( final WOA woa ) {
		final Path splitPath = Util.folder( woa.woaPath().getParent().resolve( woa.woaPath().getFileName() + ".webserverresources" ) );
		final Path splitWebServerResourcesPath = Util.folder( splitPath.resolve( "Contents" ).resolve( "WebServerResources" ) );
		final Path splitFrameworksPath = Util.folder( splitPath.resolve( "Contents" ).resolve( "Frameworks" ) );

		Util.copyContentsOfDirectoryToDirectory( woa.webServerResourcesPath(), splitWebServerResourcesPath );
		Util.copyContentsOfDirectoryToDirectory( woa.frameworksPath(), splitFrameworksPath );
	}
}