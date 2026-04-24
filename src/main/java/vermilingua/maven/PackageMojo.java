package vermilingua.maven;

import java.nio.file.Path;
import java.util.Properties;

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

import vermilingua.packaging.BuildProperties;
import vermilingua.packaging.PackageWOApplication;
import vermilingua.packaging.PackageWOApplication.WOA;
import vermilingua.packaging.PackageWOFramework;
import vermilingua.packaging.ProjectUtil;
import vermilingua.packaging.SourceProject;
import vermilingua.packaging.Util;

@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class PackageMojo extends AbstractMojo {

	private static final String DEFAULT_WORESOURCES_PATH = "src/main/woresources";

	private static final String DEFAULT_COMPONENTS_PATH = "src/main/components";

	private static final String DEFAULT_WEB_SERVER_RESOURCES_PATH = "src/main/webserver-resources";

	/**
	 * The maven project. This gets injected by Maven during the build
	 */
	@Parameter(property = "project", required = true, readonly = true)
	MavenProject mavenProject;

	/**
	 * Old method of declaring the  woresources folder name
	 */
	@Parameter(property = "woresourcesFolderName", required = false)
	@Deprecated
	String woresourcesFolderName;

	/**
	 * Project-relative path to woresources folder
	 */
	@Parameter(property = "woresourcesPath", required = false, defaultValue = DEFAULT_WORESOURCES_PATH)
	String woresourcesPath;

	/**
	 * Project-relative path to components folder
	 */
	@Parameter(property = "componentsPath", required = false, defaultValue = DEFAULT_COMPONENTS_PATH)
	String componentsPath;

	/**
	 * Project-relative path to webserver-resources folder
	 */
	@Parameter(property = "webserverResourcesPath", required = false, defaultValue = DEFAULT_WEB_SERVER_RESOURCES_PATH)
	String webserverResourcesPath;

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

		// FIXME: Delete soon, probably not many people using this // Hugi 2026-04-24
		if( woresourcesFolderName != null ) {
			throw new MojoFailureException( "The 'woresourcesFolderName' maven plugin configuration property is deprecated. Use 'woresourcesPath' instead" );
		}

		// Environment used for loading additional environment specific build.properties files
		final String environment = System.getProperty( "build.env" );

		// Properties passed to the maven builder, potentially used to override any values present in build.properties (like -Dlaunch.jvm=/some/java)
		final Properties mavenProperties = System.getProperties();

		final BuildProperties buildProperties = BuildProperties.of( mavenProject.getBasedir().toPath(), environment, mavenProperties );

		final SourceProject sourceProject = ProjectUtil.sourceProjectFromMavenProject(
				mavenProject,
				buildProperties,
				woresourcesPath,
				componentsPath,
				webserverResourcesPath );

		switch( sourceProject.type() ) {
			case Application -> {
				final String finalName = mavenProject.getBuild().getFinalName();
				final Path targetPath = Path.of( mavenProject.getBuild().getDirectory() ); // Maven's target directory

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
		Util.createTarGz( woa.woaPath(), woaArchive );

		// Set as primary artifact
		final DefaultArtifactHandler handler = new DefaultArtifactHandler( "woapplication.tar.gz" );
		final DefaultArtifact artifact = new DefaultArtifact(
				mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion(),
				null, "woapplication.tar.gz", null, handler );
		artifact.setFile( woaArchive.toFile() );
		mavenProject.setArtifact( artifact );

		// If split was performed, archive the webserver resources too
		if( performSplit ) {
			final Path splitPath = woa.woaPath().getParent().resolve( woa.woaPath().getFileName() + ".webserverresources" );
			if( splitPath.toFile().isDirectory() ) {
				final Path wsrArchive = targetPath.resolve( finalName + ".wowebserverresources.tar.gz" );
				getLog().info( "Creating " + wsrArchive.getFileName() );
				Util.createTarGz( splitPath, wsrArchive );
				projectHelper.attachArtifact( mavenProject, "tar.gz", "wowebserverresources", wsrArchive.toFile() );
			}
		}
	}

	/**
	 * Once the build is completed, copies the folders:
	 *
	 *  - App.woa/WebServerResources
	 *  - App.woa/Contents/Frameworks
	 *
	 *  from the build product into a new directory created adjacent to the WOA
	 *
	 *  - App.woa.webserverresources
	 */
	private static void extractWebServerResources( final WOA woa ) {
		final Path splitPath = Util.folder( woa.woaPath().getParent().resolve( woa.woaPath().getFileName() + ".webserverresources" ) );
		final Path splitWebServerResourcesPath = Util.folder( splitPath.resolve( "Contents" ).resolve( "WebServerResources" ) );
		final Path splitFrameworksPath = Util.folder( splitPath.resolve( "Contents" ).resolve( "Frameworks" ) );

		Util.copyContentsOfDirectoryToDirectory( woa.webserverResourcesPath(), splitWebServerResourcesPath );
		Util.copyContentsOfDirectoryToDirectory( woa.frameworksPath(), splitFrameworksPath );
	}
}