package vermilingua.packaging;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

import org.apache.maven.project.MavenProject;

import vermilingua.packaging.SourceProject.Type;

/**
 * Container class for some utility methods to obtain data about the project
 */
public class ProjectUtil {

	/**
	 * Constructs a new SourceProject from the given project data
	 */
	public static SourceProject sourceProjectFromMavenProject( final MavenProject mavenProject, final BuildProperties buildProperties, final String woresourcesFolderName ) {
		Objects.requireNonNull( mavenProject );
		Objects.requireNonNull( woresourcesFolderName );
		Objects.requireNonNull( buildProperties );

		final SourceProject.Type type = ProjectUtil.type( mavenProject );
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

		final SourceProject sp = new SourceProject( type, name, version, woresourcesPath, componentsPath, webServerResourcesPath, principalJarPath, principalClassName, dependencies, buildProperties );

		sp.validate();

		return sp;
	}

	/**
	 * @return Name of the WebObjects project. From build.properties, if specified, otherwise the maven project's name
	 *
	 * CHECKME: mavenProject().getArtifactId() might be more appropriate than the project's name? // Hugi 2026-04-20
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
}