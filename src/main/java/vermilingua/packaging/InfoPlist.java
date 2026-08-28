package vermilingua.packaging;

import java.util.LinkedHashMap;

public class InfoPlist {

	/**
	 * Info.plist is something of a relic from the time a WOA was a type of MacOS application,
	 * but NSBundle still depends on it, so we generate a minimal one containing only the keys actually read:
	 *
	 * - The mere existence of Resources/Info.plist inside a jar is what makes NSBundle recognize it as a bundle at all
	 * - NSExecutable: The bundle's name. Required for the application bundle (NSBundle.InitMainBundle() throws if missing
	 *   or mismatched) and effectively required for frameworks, since the fallback derives the name from the jar's
	 *   filename, which for maven-built jars includes the version
	 * - CFBundlePackageType: "FMWK" is what makes NSBundle treat a jar bundle as a framework
	 *   (NSLegacyBundle.couldBeAFramework()), including it in frameworkBundles() and properties loading
	 * - NSPrincipalClass: Loaded and initialized by NSBundle, the hook used for framework initialization
	 * - CFBundleShortVersionString/CFBundleVersion: Only used for version reporting, but kept since they're nice to have
	 */
	public static String make( final SourceProject sourceProject ) {
		final SourceProject.Type type = sourceProject.type();
		final String bundleName = sourceProject.name();
		final String version = sourceProject.version();

		final var infoPlist = new LinkedHashMap<>();
		infoPlist.put( "NSExecutable", bundleName );
		infoPlist.put( "CFBundlePackageType", type.isApp() ? "APPL" : "FMWK" );
		infoPlist.put( "CFBundleShortVersionString", version );
		infoPlist.put( "CFBundleVersion", version );

		if( type.isFramework() ) {
			final var principalClassName = sourceProject.principalClassName();

			if( principalClassName != null && !principalClassName.isEmpty() ) {
				infoPlist.put( "NSPrincipalClass", principalClassName );
			}
		}

		return new PlistSerialization( infoPlist ).toString();
	}
}