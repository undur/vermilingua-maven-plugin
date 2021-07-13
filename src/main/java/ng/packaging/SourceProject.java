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

	public SourceProject( final MavenProject mavenProject, final String woresourcesFolderName ) {
		Objects.requireNonNull( mavenProject );
		Objects.requireNonNull( woresourcesFolderName );
		_mavenProject = mavenProject;
		_woresourcesFolderName = woresourcesFolderName;
	}

	public MavenProject mavenProject() {
		return _mavenProject;
	}

	public String woresourcesFolderName() {
		return _woresourcesFolderName;
	}

	/**
	 * @return The name of the Application's main class, from the project's build.properties
	 *
	 * CHECKME: I don't like depending on build.properties. Additional files make me angry. Oh well, perhaps it's ok. For now // Hugi 2021-07-08
	 */
	public String applicationClassName( final MavenProject project ) {
		try( FileInputStream fis = new FileInputStream( project.getBasedir() + "/build.properties" )) {
			final Properties buildProperties = new Properties();
			buildProperties.load( fis );
			return buildProperties.getProperty( "principalClass" );
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