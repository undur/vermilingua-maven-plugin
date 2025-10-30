package ng.packaging;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
	 * Base path of the project
	 */
	private final Path _basePath;

	/**
	 * Name of the bundle folder that contains WO resources.
	 */
	private final String _woresourcesFolderName;

	/**
	 * Contents of the build.properties file in the project root.
	 */
	private final BuildProperties _buildProperties;

	public SourceProject( final MavenProject mavenProject, final String woresourcesFolderName ) {
		Objects.requireNonNull( mavenProject );
		Objects.requireNonNull( woresourcesFolderName );
		_mavenProject = mavenProject;
		_woresourcesFolderName = woresourcesFolderName;
		_basePath = mavenProject().getBasedir().toPath();
		_buildProperties = BuildProperties.of( _basePath.resolve( "build.properties" ) );

		// FIXME: We should allow the construction of a broken SourceProject, for proper validation. Breaking validation happens at build time // Hugi 2025-10-30
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
	 * We currently assume the app will run on JDK >= 17 and add the parameters required for that to work.
	 * It could be nicer to check the targeted java version and add parameters as required.
	 * Or not do anything at all and make the user handle this in build.properties? Explicit good, magic bad.
	 * // Hugi 2022-09-28
	 */
	public String jvmOptions() {
		String jvmOptions = _buildProperties.getProperty( "jvmOptions" );

		if( jvmOptions == null ) {
			jvmOptions = "";
		}

		final List<String> requiredParameters = List.of(
				"--add-exports java.base/sun.security.action=ALL-UNNAMED", // WO won't run without this one (required by NSTimezone)
				"--add-opens java.base/java.util=ALL-UNNAMED" // And we need this one to (at least) access the private List implementations created all over the place by more recent JDKs
		);

		// We add the "forced" parameters only if they aren't already present in build.properties
		for( final String requiredParameter : requiredParameters ) {
			if( !jvmOptions.contains( requiredParameter ) ) {
				jvmOptions = jvmOptions + " " + requiredParameter;
			}
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
		final String packaging = mavenProject().getPackaging();

		return switch( packaging ) {
			case "woapplication" -> Type.Application;
			case "woframework" -> Type.Framework;
			default -> throw new IllegalArgumentException( "Unknown packaging '%s' (I onlu know 'woapplication' and 'woframework'".formatted( packaging ) );
		};
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

		// No sense in building an application without a main class to run
		// However, frameworks do not need one
		if( type().isApp() ) {
			requiredBuildProperties.add( "principalClass" );
		}

		return requiredBuildProperties;
	}

	/**
	 * @return base path of the project
	 */
	private Path basePath() {
		return _basePath;
	}

	/**
	 * @return Path to source components
	 */
	public Path componentsPath() {
		return basePath().resolve( "src/main/components" );
	}

	/**
	 * @return Path to source woresources
	 */
	public Path woresourcesPath() {
		return basePath().resolve( "src/main/" + woresourcesFolderName() );
	}

	/**
	 * @return Path to source webserver-resources
	 */
	public Path webServerResourcesPath() {
		return basePath().resolve( "src/main/webserver-resources" );
	}

	/**
	 * @return The name of the JAR file that will contain the compiled application/framework sources (which was built by maven's own package goal before we started the WOA assembly)
	 */
	public String targetJarNameForWOA() {
		return name().toLowerCase() + ".jar";
	}
}