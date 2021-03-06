package ng.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import ng.packaging.PackageWOApplication;
import ng.packaging.PackageWOApplication.WOA;
import ng.packaging.PackageWOFramework;
import ng.packaging.SourceProject;

@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class PackageMojo extends AbstractMojo {

	/**
	 * The maven project. This gets injected by Maven during the build
	 */
	@Parameter(property = "project", required = true, readonly = true)
	MavenProject project;

	/**
	 * Allows the user to specify an alternative name for the source project's WO bundle resources folder (probably "resources")
	 */
	@Parameter(property = "woresourcesFolderName", required = false, defaultValue = SourceProject.DEFAULT_WORESOURCES_FOLDER_NAME)
	String woresourcesFolderName;

	/**
	 * Indicates that we want to extract webserver resources (for both the app and it's included frameworks)
	 * to a separate folder alongside the WOA (for installation on a web server)
	 *
	 * FIXME: Parameter needs a better name // Hugi 2021-07-17
	 * FIXME: Add a separate parameter for product/artifact compression // Hugi 2021-07-17
	 * FIXME: Since this is a relatively lightweight task, it *could* be performed by default. It's the compression that takes time // Hugi 2021-07-17
	 * FIXME: Decide on names for the final artifacts // Hugi 2021-07-17
	 */
	@Parameter(property = "performSplit", required = false)
	boolean performSplit;

	/**
	 * CHECKME: Still considering the correct design here, that's why this might look a bit... odd // Hugi 2021-07-10
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if( !woresourcesFolderName.equals( SourceProject.DEFAULT_WORESOURCES_FOLDER_NAME ) ) {
			getLog().warn( String.format( "Using non-standard woresources folder name '%s'. Using the standard name '%s' is recommended", woresourcesFolderName, SourceProject.DEFAULT_WORESOURCES_FOLDER_NAME ) );
		}

		final SourceProject sourceProject = new SourceProject( project, woresourcesFolderName );

		if( sourceProject.type().isApp() ) {
			final String finalName = project.getBuild().getFinalName();
			final WOA woa = new PackageWOApplication().execute( sourceProject, finalName );

			if( performSplit ) {
				woa.extractWebServerResources();
			}
		}
		else if( sourceProject.type().isFramework() ) {
			new PackageWOFramework().execute( sourceProject );
		}
		else {
			throw new MojoExecutionException( String.format( "I have no know what the heck you're asking me to build (%s???) but I don't know how to do it.", project.getPackaging() ) );
		}
	}
}