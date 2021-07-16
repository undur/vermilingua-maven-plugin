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
	 * Allows the user to specify an alternative name for the WO bundle resources folder (probably "resources")
	 */
	@Parameter(property = "woresourcesFolderName", required = false, defaultValue = SourceProject.DEFAULT_WORESOURCES_FOLDER_NAME)
	String woresourcesFolderName;

	/**
	 * Allows the user to specify a different name for the build product (Application name). By default the product is named
	 *
	 * CHECKME: Look into the usage/effect of this for jar frameworks, if any // Hugi 2021-07-15
	 */
	@Parameter(property = "project.build.finalName", required = false, defaultValue = "${artifactId}")
	String finalName;

	/**
	 * CHECKME: Still considering the correct design here, that's why this might look a bit... odd // Hugi 2021-07-10
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if( !woresourcesFolderName.equals( SourceProject.DEFAULT_WORESOURCES_FOLDER_NAME ) ) {
			getLog().warn( String.format( "Using non-standard woresources folder name '%s'. Using the standard name '%s' is recommended", woresourcesFolderName, SourceProject.DEFAULT_WORESOURCES_FOLDER_NAME ) );
		}

		final String packaging = project.getPackaging();

		final SourceProject sourceProject = new SourceProject( project, woresourcesFolderName );

		if( packaging.equals( "woapplication" ) ) {
			new PackageWOApplication().execute( sourceProject, finalName );
		}
		else if( packaging.equals( "woframework" ) ) {
			new PackageWOFramework().execute( sourceProject );
		}
		else {
			throw new MojoExecutionException( String.format( "I have no know what the heck you're asking me to build (%s???) but I don't know how to do it.", packaging ) );
		}
	}
}