package vermilingua.packaging;

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
		final String appJarFilename = sourceProject.name().toLowerCase() + ".jar";

		// Copy the app jar to the woa
		Util.copyFile( sourceProject.principalJarPath(), woa.javaPath().resolve( appJarFilename ), StandardCopyOption.REPLACE_EXISTING );

		// Start collection the list of jars for the classpath
		final List<String> classpath = new ArrayList<>();

		classpath.add( "APPROOT/Resources/Java/" ); // We include the Java folder on the classpath because WOLifecycle does. Allows the user to drop class files in there, but I don't think anyone ever does. Remove?
		classpath.add( "APPROOT/Resources/Java/" + appJarFilename );

		// Copy the app's resolved dependencies (direct and transient) to the WOA
		for( final Dependency dependency : sourceProject.dependencies() ) {
			final Path artifactPathInMavenRepository = dependency.file().toPath();
			final Path artifactFolderPathInWOA = Util.folder( woa.javaPath().resolve( dependency.groupId().replace( ".", "/" ) + "/" + dependency.artifactId() + "/" + dependency.version() ) );
			final Path artifactPathInWOA = artifactFolderPathInWOA.resolve( dependency.file().getName() );
			Util.copyFile( artifactPathInMavenRepository, artifactPathInWOA, StandardCopyOption.REPLACE_EXISTING );

			// Add the jar to the classpath
			classpath.add( "APPROOT/" + woa.contentsPath().relativize( artifactPathInWOA ) );
		}

		// Copy WebServerResources from framework jars to the WOA
		for( final Dependency dependency : sourceProject.dependencies() ) {
			if( Util.jarContainsNonEmptyWebServerResourcesDirectoryInRoot( dependency.file() ) ) {
				final Path destinationPath = woa.frameworksPath().resolve( dependency.artifactId() + ".framework" );
				Util.copyFolderFromJarToPath( "WebServerResources", dependency.file().toPath(), destinationPath );
			}
		}

		// Copy components
		if( Files.exists( sourceProject.componentsPath() ) ) {
			Util.copyContentsOfDirectoryToDirectoryFlatten( sourceProject.componentsPath(), woa.woresourcesPath(), List.of( "wo" ), List.of( "lproj" ) );
		}
		else {
			logger.warn( String.format( "Not copying components. %s does not exist", sourceProject.componentsPath() ) );
		}

		// Copy woresources
		if( Files.exists( sourceProject.woresourcesPath() ) ) {
			Util.copyContentsOfDirectoryToDirectory( sourceProject.woresourcesPath(), woa.woresourcesPath() );
		}
		else {
			logger.warn( String.format( "Not copying woresources. %s does not exist", sourceProject.woresourcesPath() ) );
		}

		// Copy webserverresources
		if( Files.exists( sourceProject.webserverResourcesPath() ) ) {
			Util.copyContentsOfDirectoryToDirectory( sourceProject.webserverResourcesPath(), woa.webserverResourcesPath() );
		}
		else {
			logger.warn( String.format( "Not copying WebServerResources. %s does not exist", sourceProject.webserverResourcesPath() ) );
		}

		// Write config.txt
		String configString = Util.readTemplate( "config" );
		configString = configString.replace( "${principalClass}", sourceProject.principalClassName() );
		configString = configString.replace( "${jvm}", jvm( sourceProject.buildProperties() ) );
		configString = configString.replace( "${jvmOptions}", jvmOptions( sourceProject.buildProperties() ) );
		Util.writeStringToPath( configString, woa.woaPath().resolve( "config.txt" ) );

		// Write classpath.txt
		final String classpathString = String.join( "\n", classpath ) + "\n";
		Util.writeStringToPath( classpathString, woa.woaPath().resolve( "classpath.txt" ) );

		// Write Info.plist
		final String infoPlistString = InfoPlist.make( sourceProject, appJarFilename );
		Util.writeStringToPath( infoPlistString, woa.infoPlistPath() );

		// Write executable launch script
		final String launchScriptString = Util.readTemplate( "launch-script" );
		final Path launchScriptPath = woa.woaPath().resolve( sourceProject.name() );
		Util.writeStringToPath( launchScriptString, launchScriptPath );
		Util.makeUserExecutable( launchScriptPath );

		return woa;
	}

	/**
	 * @return The JVM executable to use for launching the application
	 */
	private static String jvm( final BuildProperties buildProperties ) {
		final String jvm = buildProperties.jvm();
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
	private static String jvmOptions( final BuildProperties buildProperties ) {
		String jvmOptions = buildProperties.jvmOptions();

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
			return new WOA( woaPath );
		}

		private WOA( final Path woaPath ) {
			Objects.requireNonNull( woaPath );
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
		public Path webserverResourcesPath() {
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