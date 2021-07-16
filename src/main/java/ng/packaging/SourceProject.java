package ng.packaging;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
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
		final String stringType = _buildProperties.getProperty( "project.type" );

		switch( stringType ) {
		case "application":
			return Type.Application;
		case "framework":
			return Type.Framework;
		default:
			throw new IllegalArgumentException( String.format( "I've never seen a project of type '%s' before. Check your build.properties", stringType ) );
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
		try( FileInputStream fis = new FileInputStream( mavenProject().getBasedir() + "/build.properties" )) {
			final Properties buildProperties = new Properties();
			buildProperties.load( fis );
			return buildProperties;
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}
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
}