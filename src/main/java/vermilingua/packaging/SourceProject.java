package vermilingua.packaging;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.maven.project.MavenProject;

/**
 * Source for packaging of a WO build (Application or framework)
 *
 * @param type The project's type (Application vs. framework)
 * @param name The project's name // CHECKME: Note that if we eventually want to support projects without build.properties, mavenProject().getArtifactId() might be an acceptable replacement value here
 * @param version The project's version, currently as specified in the pom file.
 * @param woresourcesPath Path to folder containing WO resources
 * @param componentsPath Path to folder containing component templates and API files
 * @param webServerResourcesPath Path to folder containing WebServerResources
 * @param principalJarPath Path to the main jar file from the inital compilation/packaging of the project java sources. Including this in "SourceProject" might look strange, but is actually intentional since SourceProject represents a WO project _after_ maven's jar plugin has done it's job
 * @param principalClassName Fully qualified name of the principal class (Application/main class for application, principalClass for frameworks)
 * @param dependencies The project's list of dependencies
 * @param buildProperties The project's build.properties
 */

public record SourceProject(
		Type type,
		String name,
		String version,
		Path woresourcesPath,
		Path componentsPath,
		Path webServerResourcesPath,
		Path principalJarPath,
		String principalClassName,
		Collection<Dependency> dependencies,
		BuildProperties buildProperties ) {

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
	 * Constructs a new SourceProject from the given project data
	 */
	public static SourceProject forMavenProject( final MavenProject mavenProject, final BuildProperties buildProperties, final String woresourcesFolderName ) {
		Objects.requireNonNull( mavenProject );
		Objects.requireNonNull( woresourcesFolderName );
		Objects.requireNonNull( buildProperties );

		final Type type = ProjectUtil.type( mavenProject );
		final String name = ProjectUtil.nameFromProject( buildProperties, mavenProject );
		final String version = mavenProject.getVersion();

		final Path projectBasePath = mavenProject.getBasedir().toPath();
		final Path woresourcesPath = projectBasePath.resolve( "src/main/" + woresourcesFolderName );
		final Path componentsPath = projectBasePath.resolve( "src/main/components" );
		final Path webServerResourcesPath = projectBasePath.resolve( "src/main/webserver-resources" );

		final Path principalJarPath = mavenProject.getArtifact().getFile().toPath();
		final String principalClassName = buildProperties.principalClass();
		final Collection<Dependency> dependencies = mavenProject
				.getArtifacts()
				.stream()
				.map( a -> new Dependency( a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getFile() ) )
				.toList();

		// FIXME: We should allow the construction of a broken SourceProject, for proper validation. Breaking validation happens at build time // Hugi 2025-10-30
		ProjectUtil.validateBuildProperties( type, buildProperties );

		return new SourceProject( type, name, version, woresourcesPath, componentsPath, webServerResourcesPath, principalJarPath, principalClassName, dependencies, buildProperties );
	}

	/**
	 * @return The name of the JAR file that will contain the compiled application/framework sources (which was built by maven's own package goal before we started the WOA assembly)
	 *
	 * CHECKME: Really belongs in packaging, not the source project // Hugi 2026-04-19
	 */
	public String targetJarNameForWOA() {
		return name().toLowerCase() + ".jar";
	}

	/**
	 * Container class for some utility methods to obtain data about the project
	 */
	private static class ProjectUtil {

		/**
		 * @return Name of the WebObjects project. From build.properties, if specified, otherwise the maven project's name
		 */
		private static String nameFromProject( final BuildProperties buildProperties, final MavenProject mavenProject ) {
			final String buildPropertiesProjectName = buildProperties.projectName();

			if( buildPropertiesProjectName != null ) {
				return buildPropertiesProjectName;
			}

			return mavenProject.getName();
		}

		/**
		 * @return The type of the project
		 */
		private static Type type( final MavenProject mavenProject ) {
			final String packaging = mavenProject.getPackaging();

			return switch( packaging ) {
				case "woapplication" -> Type.Application;
				case "woframework" -> Type.Framework;
				default -> throw new IllegalArgumentException( "Unknown packaging '%s'. I only know 'woapplication' and 'woframework'".formatted( packaging ) );
			};
		}

		/**
		 * Ensure all required properties are present
		 *
		 * @throws IllegalArgumentException If a required build property is not present
		 */
		private static void validateBuildProperties( final Type type, final BuildProperties buildProperties ) {
			for( final String propertyName : requiredBuildProperties( type ) ) {
				if( !buildProperties.containsKey( propertyName ) ) {
					throw new IllegalArgumentException( String.format( "%s must be present in build.properties", propertyName ) );
				}
			}
		}

		/**
		 * @return List of properties that must be present in build.properties for a build to succeed
		 */
		private static List<String> requiredBuildProperties( final Type type ) {
			return switch( type ) {
				case Application -> List.of( "principalClass" );
				case Framework -> List.of();
			};
		}
	}
}