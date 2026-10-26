package vermilingua.maven;

import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import vermilingua.packaging.BuildProperties;
import vermilingua.packaging.PackageWOApplication;
import vermilingua.packaging.PackageWOFramework;
import vermilingua.packaging.ProjectUtil;
import vermilingua.packaging.SourceProject;

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
	 * Project-relative path to woresources folder. Defaults to dir.woresources in build.properties, then src/main/woresources.
	 */
	@Parameter(property = "woresourcesPath", required = false)
	String woresourcesPath;

	/**
	 * Project-relative path to components folder. Defaults to dir.components in build.properties, then src/main/components.
	 */
	@Parameter(property = "componentsPath", required = false)
	String componentsPath;

	/**
	 * Project-relative path to webserver-resources folder. Defaults to dir.webserverResources in build.properties, then src/main/webserver-resources.
	 */
	@Parameter(property = "webserverResourcesPath", required = false)
	String webserverResourcesPath;

	/**
	 * Lists the Apple WebObjects frameworks (group id com.webobjects) at the end of classpath.txt.
	 * Project Wonder replaces classes from those frameworks, and the JVM loads the first copy of a class it finds,
	 * so the Apple jars have to come last for the replacements to win. This is what wolifecycle does.
	 * Other dependencies stay in Maven's resolve order. Defaults to false.
	 */
	@Parameter(property = "allowWebObjectsPatching", required = false, defaultValue = "false")
	boolean allowWebObjectsPatching;

	/**
	 * Entry point for the assembly process
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Environment used for loading additional environment specific build.properties files
		final String environment = System.getProperty( "build.env" );

		// Properties passed to the maven builder, potentially used to override any values present in build.properties (like -Dlaunch.jvm=/some/java)
		final Properties mavenProperties = System.getProperties();

		final BuildProperties buildProperties = BuildProperties.of( mavenProject.getBasedir().toPath(), environment, mavenProperties );

		final SourceProject sourceProject = ProjectUtil.sourceProjectFromMavenProject(
				mavenProject,
				buildProperties,
				folderPath( "woresourcesPath", woresourcesPath, "dir.woresources", buildProperties.woresourcesDir(), DEFAULT_WORESOURCES_PATH ),
				folderPath( "componentsPath", componentsPath, "dir.components", buildProperties.componentsDir(), DEFAULT_COMPONENTS_PATH ),
				folderPath( "webserverResourcesPath", webserverResourcesPath, "dir.webserverResources", buildProperties.webserverResourcesDir(), DEFAULT_WEB_SERVER_RESOURCES_PATH ) );

		switch( sourceProject.type() ) {
			case Application -> {
				final String finalName = mavenProject.getBuild().getFinalName();
				final Path targetPath = Path.of( mavenProject.getBuild().getDirectory() ); // Maven's target directory

				new PackageWOApplication().execute( sourceProject, finalName, targetPath, allowWebObjectsPatching );
			}
			case Framework -> {
				new PackageWOFramework().execute( sourceProject );
			}
		}
	}

	/**
	 * @return The folder path configured for the plugin, else the one declared in build.properties, else the default.
	 *
	 * The application reads its folders from build.properties when run from its project folder, so a plugin configuration
	 * that disagrees with it packages something other than what runs in development. That gets a warning.
	 */
	String folderPath( final String parameterName, final String configured, final String buildPropertiesKey, final String declared, final String defaultPath ) throws MojoFailureException {

		// A blank path resolves to the project folder itself, which would package the whole project (including the build output being written)
		if( configured != null && configured.isBlank() ) {
			throw new MojoFailureException( "'%s' is blank in the plugin configuration. Remove it, or set it to a folder".formatted( parameterName ) );
		}

		if( declared != null && declared.isBlank() ) {
			throw new MojoFailureException( "'%s' is blank in build.properties. Remove it, or set it to a folder".formatted( buildPropertiesKey ) );
		}

		if( configured != null ) {
			if( declared != null && !Path.of( configured ).normalize().equals( Path.of( declared ).normalize() ) ) {
				getLog().warn( "'%s' is '%s' in the plugin configuration but '%s' is '%s' in build.properties. Packaging uses '%s'".formatted( parameterName, configured, buildPropertiesKey, declared, configured ) );
			}

			return configured;
		}

		return declared != null ? declared : defaultPath;
	}
}
