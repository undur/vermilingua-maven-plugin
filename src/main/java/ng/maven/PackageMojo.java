package ng.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class PackageMojo extends AbstractMojo {

	/**
	 * The maven project. This gets injected by Maven during the build
	 */
	@Parameter(property = "project", required = true, readonly = true)
	MavenProject project;

	/**
	 * Allows the user to specify an alternative name for the WO bundle resources folder (probably "resources")
	 *
	 * CHECKME: I'd prefer not to include this and just standardize on the new/correct bundle layout with a separate "woresources" folder  // Hugi 2021-07-08
	 */
	@Parameter(property = "woresourcesFolderName", required = false, defaultValue = "woresources")
	String woresourcesFolderName;

	/**
	 * CHECKME: Still considering the correct design here, that's why this might look a bit... odd // Hugi 2021-07-10
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final String packaging = project.getPackaging();

		if( packaging.equals( "woapplication" ) ) {
			new PackageWOApplication().execute( project, woresourcesFolderName );
		}
		else if( packaging.equals( "woframework" ) ) {
			new PackageWOFramework().execute( project, woresourcesFolderName );
		}
		else {
			throw new MojoExecutionException( String.format( "I have no know what the heck you're asking me to build (%s???) but I don't know how to do it.", packaging ) );
		}
	}
}