package vermilingua.packaging;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class PackageWOFramework {

	public void execute( final SourceProject sourceProject ) {
		Objects.requireNonNull( sourceProject );

		final Path stagingDir;
		try {
			stagingDir = Files.createTempDirectory( "vermilingua-framework-" );
		}
		catch( final IOException e ) {
			throw new UncheckedIOException( e );
		}

		try {
			if( Files.exists( sourceProject.componentsPath() ) ) {
				Util.copyContentsOfDirectoryToDirectoryFlatten( sourceProject.componentsPath(), stagingDir, List.of( "wo" ), List.of( "lproj" ) );
			}

			if( Files.exists( sourceProject.woresourcesPath() ) ) {
				Util.copyContentsOfDirectoryToDirectory( sourceProject.woresourcesPath(), stagingDir );
			}

			Util.copyContentsOfFolderAtPathToFolderInJar( stagingDir, "Resources", sourceProject.principalJarPath() );
			Util.copyContentsOfFolderAtPathToFolderInJar( sourceProject.webServerResourcesPath(), "WebServerResources", sourceProject.principalJarPath() );

			final String infoPlistString = InfoPlist.make( sourceProject );
			Util.writeStringToPathInJar( infoPlistString, "Resources/Info.plist", sourceProject.principalJarPath() );
		}
		finally {
			Util.deleteRecursively( stagingDir );
		}
	}
}
