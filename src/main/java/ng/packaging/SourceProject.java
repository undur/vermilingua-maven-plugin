package ng.packaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * Source for packaging of a WO build (Application or framework)
 */

public class SourceProject {

	public static enum Type {
		Application,
		Framework;

		public boolean isApp() {
			return this == Application;
		}

		public boolean isFramework() {
			return this == Framework;
		}
	}

	public static final String DEFAULT_WORESOURCES_FOLDER_NAME = "woresources";

	/**
	 * Note: We're currently including the maven project to get references to the resolved dependencies (.getArtifacts()).
	 * This isn't a huge problem now, but Eventually this should probably be abstracted away removed to make building possible outside of a maven context
	 */
	private final MavenProject _mavenProject;

	/**
	 * Name of the bundle folder that contains WO resources.
	 */
	private final String _woresourcesFolderName;

	/**
	 * Contents of the build.properties file in the project root.
	 */
	private final Properties _buildProperties;

	public SourceProject( final MavenProject mavenProject, final String woresourcesFolderName ) {
		Objects.requireNonNull( mavenProject );
		Objects.requireNonNull( woresourcesFolderName );
		_mavenProject = mavenProject;
		_woresourcesFolderName = woresourcesFolderName;
		_buildProperties = readBuildProperties();

		validateBuildProperties();
	}

	/**
	 * @return The maven project used this SourceProject was constructed from
	 */
	private MavenProject mavenProject() {
		return _mavenProject;
	}

	/**
	 * @return Dependencies of this project
	 */
	public Collection<Dependency> dependencies() {
		List<Dependency> dependencies = new ArrayList<>();

		for( final Artifact a : mavenProject().getArtifacts() ) {
			dependencies.add( new Dependency( a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getFile() ) );
		}

		return dependencies;
	}

	/**
	 * @return The name of the WO bundle resources folder
	 */
	public String woresourcesFolderName() {
		return _woresourcesFolderName;
	}

	/**
	 * @return In the case of applications, this is the main class. In the case of frameworks, this is the framework's principalClass
	 */
	public String principalClassName() {
		return _buildProperties.getProperty( "principalClass" );
	}

	/**
	 * @return Name of the WebObjects project as specified in build.properties
	 *
	 * Note that if we eventually want to support projects without build.properties, mavenProject().getArtifactId() might be an acceptable replacement value here
	 */
	public String name() {
		String projectName = _buildProperties.getProperty( "project.name" );

		if( projectName == null ) {
			projectName = mavenProject().getName();
		}

		return projectName;
	}

	/**
	 * @return String of arguments to pass on to the generated launch scripts' JVM
	 *
	 * CHECKME:
	 * We're currently assuming JDK>=17 and adding the required parameters for that.
	 * A nicer course of action might be to check the targeted java version and adding parameters as required.
	 * Or not do anything at all and make the user do it. Explicit good, magic bad.
	 * // Hugi 2022-09-28
	 */
	public String jvmOptions() {
		String jvmOptions = _buildProperties.getProperty( "jvmOptions" );

		if( jvmOptions == null ) {
			jvmOptions = "";
		}

		// We're injecting this into all apps, since WO won't run without it. Not really great.
		final String requiredParameter = "--add-exports java.base/sun.security.action=ALL-UNNAMED";

		if( !jvmOptions.contains( requiredParameter ) ) {
			jvmOptions = jvmOptions + " " + requiredParameter;
		}

		return jvmOptions;
	}

	/**
	 * @return Version of the project, as specified in the pom file.
	 */
	public String version() {
		return mavenProject().getVersion();
	}

	/**
	 * @return The path to the jar file from the inital compilation/packaging of the project java sources
	 *
	 * Including this as a part of "SourceProject" might look strange but note that
	 * SourceProject represents a WO project after maven's jar plugin has done it's job.
	 */
	public Path jarPath() {
		return mavenProject().getArtifact().getFile().toPath();
	}

	/**
	 * @return The type of the project
	 */
	public Type type() {
		final String stringType = mavenProject().getPackaging();

		switch( stringType ) {
		case "woapplication":
			return Type.Application;
		case "woframework":
			return Type.Framework;
		default:
			throw new IllegalArgumentException( String.format( "I'm not familiar with packaging '%s'. The only packaging types I know are 'woapplication' and 'woframework'", stringType ) );
		}
	}

	/**
	 * @return The projects build.properties
	 *
	 * In theory, most of the stuff we actually _need_ from there could be derived from the pom.
	 * But this is the mechanism we currently have and it works well, so let's stick with it,
	 * at least until we start working on WOLips // Hugi 2021-07-14
	 */
	private Properties readBuildProperties() {
		final String pathToBuildPropertiesFile = mavenProject().getBasedir() + "/build.properties";

		if( !new File( pathToBuildPropertiesFile ).exists() ) {
			throw new IllegalStateException( String.format( "build.properties not found in project root (%s). To build a project with vermilingua, a file called 'build.properties' file must exist in the root and must contain at least the properties %s", pathToBuildPropertiesFile, requiredBuildProperties() ) );
		}

		try( FileInputStream fis = new FileInputStream( pathToBuildPropertiesFile )) {
			final Properties buildProperties = new Properties();
			buildProperties.load( fis );
			return buildProperties;
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * Ensure all required properties are present
	 *
	 * @throws IllegalArgumentException If a required build property is not present
	 */
	private void validateBuildProperties() {
		for( final String propertyName : requiredBuildProperties() ) {
			if( !_buildProperties.containsKey( propertyName ) ) {
				throw new IllegalArgumentException( String.format( "%s must be present in build.properties", propertyName ) );
			}
		}
	}

	/**
	 * @return The list of properties that _must_ be present in build.properties for a build to succeed
	 */
	private List<String> requiredBuildProperties() {
		final List<String> requiredBuildProperties = new ArrayList<>();
		//		requiredBuildProperties.add( "project.name" ); // FIXME: Experimenting with making the project name a non-requirement // Hugi 2022-10-10

		// No sense in building an application without a main class to run
		// However, frameworks do not need one
		if( type() == Type.Application ) {
			requiredBuildProperties.add( "principalClass" );
		}

		return requiredBuildProperties;
	}

	/**
	 * @return Path to source components
	 */
	public Path componentsPath() {
		return Path.of( mavenProject().getBasedir() + "/src/main/components" );
	}

	/**
	 * @return Path to source woresources
	 */
	public Path woresourcesPath() {
		return Path.of( mavenProject().getBasedir() + "/src/main/" + woresourcesFolderName() );
	}

	/**
	 * @return Path to source webserver-resources
	 */
	public Path webServerResourcesPath() {
		return Path.of( mavenProject().getBasedir() + "/src/main/webserver-resources" );
	}

	/**
	 * @return The name of the JAR file that will contain the compiled application/framework sources (which was built by maven's own package goal before we started the WOA assembly)
	 */
	public String targetJarNameForWOA() {
		return name().toLowerCase() + ".jar";
	}
}