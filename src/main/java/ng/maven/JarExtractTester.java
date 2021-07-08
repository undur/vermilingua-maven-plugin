package ng.maven;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JarExtractTester {

	public static void main( String[] args ) {
		final File file = new File( "/Users/hugi/.m2/repository/is/rebbi/helium/2.0.0-SNAPSHOT/helium-2.0.0-SNAPSHOT.jar" );
		final Path destinationPath = Paths.get( "/Users/hugi/Desktop/data/" );
		Util.copyWebServerResourcesFromJarToPath( file, destinationPath );
	}
}