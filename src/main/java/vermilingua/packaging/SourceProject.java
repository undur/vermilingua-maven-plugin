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
 * @param jarPath The path to the jar file from the inital compilation/packaging of the project java sources. Including this as a part of "SourceProject" might look strange but note that SourceProject represents a WO project after maven's jar plugin has done it's job.
 * @param woresourcesPath Path to folder containing WO resources
 * @param componentsPath Path to folder containing component templates and API files
 * @param webServerResourcesPath Path to folder containing WebServerResources
 * @param principalClassName Fully qualified name of the principal class (Application/main class for application, principalClass for frameworks)
 * @param dependencies The project's list of dependencies
 * @param buildProperties The project's build.properties
 */

public record SourceProject(
		Type type,
		String name,
		String version,
		Path jarPath,
		Path woresourcesPath,
		Path componentsPath,
		Path webServerResourcesPath,
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
		final Path jarPath = mavenProject.getArtifact().getFile().toPath();

		final Path basePath = mavenProject.getBasedir().toPath();
		final Path woresourcesPath = basePath.resolve( "src/main/" + woresourcesFolderName );
		final Path componentsPath = basePath.resolve( "src/main/components" );
		final Path webServerResourcesPath = basePath.resolve( "src/main/webserver-resources" );

		final String principalClassName = buildProperties.principalClass();
		final Collection<Dependency> dependencies = ProjectUtil.dependenciesFromMaven( mavenProject );

		// FIXME: We should allow the construction of a broken SourceProject, for proper validation. Breaking validation happens at build time // Hugi 2025-10-30
		ProjectUtil.validateBuildProperties( type, buildProperties );

		return new SourceProject( type, name, version, jarPath, woresourcesPath, componentsPath, webServerResourcesPath, principalClassName, dependencies, buildProperties );
	}

	/**
	 * @return The JVM executable to use for launching the application
	 */
	public String jvm() {
		final String jvm = buildProperties().jvm();
		return jvm != null ? jvm : "java";
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
		String jvmOptions = buildProperties().jvmOptions();

		if( jvmOptions == null ) {
			jvmOptions = "";
		}

		final List<String> requiredParameters = List.of(
				"--add-exports java.base/sun.security.action=ALL-UNNAMED", // WO won't run without this one (required by NSTimezone)
				"--add-opens java.base/java.util=ALL-UNNAMED", // And we need this one to (at least) access the private List implementations created all over the place by more recent JDKs
				"--add-opens java.base/java.time=ALL-UNNAMED", // For accessing methods on java.time related objects (like LocalDate.year)
				"--add-opens java.base/java.lang=ALL-UNNAMED" // Various classes in the lang package
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
	 * @return The name of the JAR file that will contain the compiled application/framework sources (which was built by maven's own package goal before we started the WOA assembly)
	 */
	public String targetJarNameForWOA() {
		return name().toLowerCase() + ".jar";
	}

	/**
	 * Container class for some utility methods to obtain data about the project
	 */
	private static class ProjectUtil {

		/**
		 * @return Dependencies of the given project
		 */
		private static Collection<Dependency> dependenciesFromMaven( final MavenProject mavenProject ) {
			return mavenProject
					.getArtifacts()
					.stream()
					.map( a -> new Dependency( a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getFile() ) )
					.toList();
		}

		/**
		 * @return Name of the WebObjects project as specified in build.properties
		 *
		 * Note that if we eventually want to support projects without build.properties, mavenProject().getArtifactId() might be an acceptable replacement value here
		 */
		private static String nameFromProject( final BuildProperties buildProperties, final MavenProject mavenProject ) {
			String projectName = buildProperties.projectName();

			if( projectName == null ) {
				projectName = mavenProject.getName();
			}

			return projectName;
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