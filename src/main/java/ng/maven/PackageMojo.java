package ng.maven;

import java.nio.file.Path;

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
	 * Entry point for the assembly process
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if( !woresourcesFolderName.equals( SourceProject.DEFAULT_WORESOURCES_FOLDER_NAME ) ) {
			getLog().warn( String.format( "Using non-standard woresources folder name '%s'. Using the standard name '%s' is recommended", woresourcesFolderName, SourceProject.DEFAULT_WORESOURCES_FOLDER_NAME ) );
		}

		final SourceProject sourceProject = new SourceProject( project, woresourcesFolderName );

		if( sourceProject.type().isApp() ) {
			final String finalName = project.getBuild().getFinalName();
			final Path targetPath = Path.of( project.getBuild().getDirectory() ); // Maven's target directory

			final WOA woa = new PackageWOApplication().execute( sourceProject, finalName, targetPath );

			if( performSplit ) {
				woa.extractWebServerResources();
			}
		}
		else if( sourceProject.type().isFramework() ) {
			new PackageWOFramework().execute( sourceProject );
		}
		else {
			throw new MojoExecutionException( String.format( "I have no idea what you're asking me to build ('%s'? WTF??) but I don't know how to do it.", project.getPackaging() ) );
		}
	}
}