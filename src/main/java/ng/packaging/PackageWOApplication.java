package ng.packaging;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageWOApplication {

	private static final Logger logger = LoggerFactory.getLogger( PackageWOApplication.class );

	/**
	 * Builds a WOA bundle
	 *
	 * @param sourceProject The project we're building from
	 * @param woaName Name of the WOA (not including the .woa suffix)
	 * @param targetPath Directory where the WOA bundle will be placed
	 *
	 * @return The assembled WOA bundle
	 */
	public WOA execute( final SourceProject sourceProject, final String woaName, final Path targetPath ) {
		Objects.requireNonNull( sourceProject );
		Objects.requireNonNull( woaName );
		Objects.requireNonNull( targetPath );

		// The WOA bundle, the destination for our build.
		final WOA woa = WOA.create( targetPath, woaName );

		// The eventual name of the app's JAR file
		final String appJarFilename = sourceProject.targetJarNameForWOA();

		// Copy the app jar to the woa
		Util.copyFile( sourceProject.jarPath(), woa.javaPath().resolve( appJarFilename ), StandardCopyOption.REPLACE_EXISTING );

		// Start working on that list of jars to add to the classpath
		final List<String> classpathStrings = new ArrayList<>();

		classpathStrings.add( "APPROOT/Resources/Java/" ); // FIXME: WOLifecycle includes the Java folder itself on the classpath. Allows us to drop class files in there, but I think we should probably just… don't // Hugi 2025-03-28
		classpathStrings.add( "APPROOT/Resources/Java/" + appJarFilename );

		// Copy the app's resolved dependencies (direct and transient) to the WOA
		for( final Dependency dependency : sourceProject.dependencies() ) {
			final Path artifactPathInMavenRepository = dependency.file().toPath();
			final Path artifactFolderPathInWOA = Util.folder( woa.javaPath().resolve( dependency.groupId().replace( ".", "/" ) + "/" + dependency.artifactId() + "/" + dependency.version() ) );
			final Path artifactPathInWOA = artifactFolderPathInWOA.resolve( dependency.file().getName() );
			Util.copyFile( artifactPathInMavenRepository, artifactPathInWOA, StandardCopyOption.REPLACE_EXISTING );

			// Add the jar to the classpath
			classpathStrings.add( "APPROOT/" + woa.contentsPath().relativize( artifactPathInWOA ) );
		}

		// Copy WebServerResources from framework jars to the WOA
		for( final Dependency dependency : sourceProject.dependencies() ) {
			if( Util.jarContainsNonEmptyWebServerResourcesDirectoryInRoot( dependency.file() ) ) {
				final Path destinationPath = woa.frameworksPath().resolve( dependency.artifactId() + ".framework" );
				Util.copyFolderFromJarToPath( "WebServerResources", dependency.file().toPath(), destinationPath );
			}
		}

		if( Files.exists( sourceProject.componentsPath() ) ) {
			Util.copyContentsOfDirectoryToDirectoryFlatten( sourceProject.componentsPath(), woa.woresourcesPath(), List.of( "wo" ) );
		}
		else {
			logger.warn( String.format( "Not copying components. %s does not exist", sourceProject.componentsPath() ) );
		}

		if( Files.exists( sourceProject.woresourcesPath() ) ) {
			Util.copyContentsOfDirectoryToDirectory( sourceProject.woresourcesPath(), woa.woresourcesPath() );
		}
		else {
			logger.warn( String.format( "Not copying woresources. %s does not exist", sourceProject.woresourcesPath() ) );
		}

		if( Files.exists( sourceProject.webServerResourcesPath() ) ) {
			Util.copyContentsOfDirectoryToDirectory( sourceProject.webServerResourcesPath(), woa.webServerResourcesPath() );
		}
		else {
			logger.warn( String.format( "Not copying WebServerResources. %s does not exist", sourceProject.webServerResourcesPath() ) );
		}

		// Write config.txt
		String configString = Util.readTemplate( "config" );
		configString = configString.replace( "${ApplicationClass}", sourceProject.principalClassName() );
		configString = configString.replace( "${JVM}", sourceProject.jvm() );
		configString = configString.replace( "${JVMOptions}", sourceProject.jvmOptions() );
		configString = configString.replace( "${JDB}", sourceProject.jdb() );
		configString = configString.replace( "${JDBOptions}", sourceProject.jdbOptions() );
		Util.writeStringToPath( configString, woa.woaPath().resolve( "config.txt" ) );

		// Write classpath.txt
		final String classpathString = String.join( "\n", classpathStrings ) + "\n";
		Util.writeStringToPath( classpathString, woa.woaPath().resolve( "classpath.txt" ) );

		// Write Info.plist
		final String infoPlistString = InfoPlist.make( sourceProject );
		Util.writeStringToPath( infoPlistString, woa.infoPlistPath() );

		// Write executable launch script
		final String launchScriptString = Util.readTemplate( "launch-script" );
		final Path launchScriptPath = woa.woaPath().resolve( sourceProject.name() );
		Util.writeStringToPath( launchScriptString, launchScriptPath );
		Util.makeUserExecutable( launchScriptPath );

		return woa;
	}

	/**
	 * Our in-memory representation of the WOA bundle
	 */
	public static class WOA {

		private final Path _woaPath;

		/**
		 * @return The WOA bundle [applicationName].woa in [containingDirectory]
		 */
		public static WOA create( final Path containingDirectory, final String applicationName ) {
			Objects.requireNonNull( containingDirectory );
			Objects.requireNonNull( applicationName );
			final Path woaPath = containingDirectory.resolve( applicationName + ".woa" );
			return new WOA( woaPath, applicationName );
		}

		private WOA( final Path woaPath, final String applicationName ) {
			Objects.requireNonNull( woaPath );
			Objects.requireNonNull( applicationName );
			_woaPath = Util.folder( woaPath );
		}

		/**
		 * @return Path to the WOA root
		 */
		public Path woaPath() {
			return _woaPath;
		}

		/**
		 * @return Root destination path for the WOA's contents
		 */
		public Path contentsPath() {
			return Util.folder( woaPath().resolve( "Contents" ) );
		}

		/**
		 * @return Destination path for frameworks to be embedded in the WOA bundle
		 */
		public Path frameworksPath() {
			return Util.folder( contentsPath().resolve( "Frameworks" ) );
		}

		/**
		 * @return Destination path for WO's woresources/bundle-resources/application-resources/whacchamacallit
		 */
		public Path woresourcesPath() {
			return Util.folder( contentsPath().resolve( "Resources" ) );
		}

		/**
		 * @return Destination path for WO's webserver resources
		 */
		public Path webServerResourcesPath() {
			return Util.folder( contentsPath().resolve( "WebServerResources" ) );
		}

		/**
		 * @return Destination path for jar files
		 */
		public Path javaPath() {
			return Util.folder( woresourcesPath().resolve( "Java" ) );
		}

		/**
		 * @return Destination path for Info.plist
		 */
		public Path infoPlistPath() {
			return contentsPath().resolve( "Info.plist" );
		}
	}
}