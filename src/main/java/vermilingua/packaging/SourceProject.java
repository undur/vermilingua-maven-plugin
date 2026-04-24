package vermilingua.packaging;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Source for packaging of a WO build (Application or framework)
 *
 * @param type The project's type (Application vs. framework)
 * @param name The project's name
 * @param version The project's version, currently as specified in the pom file.
 * @param woresourcesPath Path to folder containing WO resources
 * @param componentsPath Path to folder containing component templates and API files
 * @param webserverResourcesPath Path to folder containing webserver resources
 * @param principalJarPath Path to the main jar file from the initial compilation/packaging of the project java sources. Including this in "SourceProject" might look strange, but is actually intentional since SourceProject represents a WO project _after_ maven's jar plugin has done it's job
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
		Path webserverResourcesPath,
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

	/**
	 * Validates the project
	 *
	 * CHECKME: Allow full validation of a broken SourceProject, showing all problems at once. Failing on validation should only happen at build time // Hugi 2025-10-30
	 */
	public void validate() {
		if( type == Type.Application && principalClassName == null ) {
			throw new IllegalArgumentException( "'principalClass' must be declared in build.properties when building an application" );
		}
	}
}