package ng.packaging;

import java.io.FileInputStream;
import java.io.IOException;
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

	public static final String DEFAULT_WORESOURCES_FOLDER_NAME = "woresources";

	private final MavenProject _mavenProject;
	private final String _woresourcesFolderName;
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

	public Properties readBuildProperties() {
		try( FileInputStream fis = new FileInputStream( mavenProject().getBasedir() + "/build.properties" )) {
			final Properties buildProperties = new Properties();
			buildProperties.load( fis );
			return buildProperties;
		}
		catch( final IOException e ) {
			throw new RuntimeException( e );
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