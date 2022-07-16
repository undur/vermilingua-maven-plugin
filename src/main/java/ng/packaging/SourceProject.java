package ng.packaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.project.MavenProject;

/**
 * Source for packaging of a WO build (Application or framework)
 *
 * FIXME: This includes MavenProject at the moment, we want to gradually abstract that away // Hugi 2021-07-12
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

	public MavenProject mavenProject() {
		return _mavenProject;
	}

	public String woresourcesFolderName() {
		return _woresourcesFolderName;
	}

	/**
	 * @return The name of the Application's main class, from the project's build.properties
	 */
	public String principalClassName() {
		return _buildProperties.getProperty( "principalClass" );
	}

	/**
	 * @return Name of the WebObjects project as specified in build.properties
	 *
	 * CHECKME: Should we be using mavenProject().getArtifactId()? // Hugi 2021-07-16
	 */
	public String name() {
		return _buildProperties.getProperty( "project.name" );
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
	 *
	 * Still, feels weird. I'd like to move this away from here eventually // Hugi 2021-07-16
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

		// FIXME: we might want to do this in project preflighting, rather than in the actual read method // Hugi 2022-07-16
		if( !new File( pathToBuildPropertiesFile ).exists() ) {
			throw new IllegalStateException( "build.properties file not found ing project root (%s)".formatted( pathToBuildPropertiesFile ) );
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
	 * @throws IllegalArgumentException // FIXME: Pick a better exception type or make our own // Hugi 2022-07-16
	 */
	private void validateBuildProperties() {
		for( final String propertyName : requiredBuildProperties() ) {
			if( !_buildProperties.containsKey( propertyName ) ) {
				throw new IllegalArgumentException( "%s must be present in build.properties".formatted( propertyName ) );
			}
		}
	}

	/**
	 * @return The list of properties that _must_ be present in build.properties for a build to succeed
	 */
	private List<String> requiredBuildProperties() {
		final List<String> requiredBuildProperties = new ArrayList<>();
		requiredBuildProperties.add( "project.name" );

		// No sense in building an application without a Principal class to run
		// However, frameworks do not need one
		if( type() == Type.Application ) {
			requiredBuildProperties.add( "principalClass" );
		}

		return requiredBuildProperties;
	}

	public Path componentsPath() {
		return Path.of( mavenProject().getBasedir() + "/src/main/components" );
	}

	public Path woresourcesPath() {
		return Path.of( mavenProject().getBasedir() + "/src/main/" + woresourcesFolderName() );
	}

	public Path webServerResourcesPath() {
		return Path.of( mavenProject().getBasedir() + "/src/main/webserver-resources" );
	}

	/**
	 * FIXME: This is added as a temporary measure until the final name for the app's JAR file is decided // Hugi 2021-07-16
	 */
	public String targetJarNameForWOA() {
		return name() + ".jar";
	}
}