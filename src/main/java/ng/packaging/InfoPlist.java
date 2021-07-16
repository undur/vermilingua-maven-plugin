package ng.packaging;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InfoPlist {

	/**
	 * Info.plist is something of a relic from the time a WOA was a type of MacOS application.
	 * Unfortunately, WO/Wonder currently reads the Info.plist and actually use some of the values in it.
	 * This hack generates an Info.plist, based on nothing but replicating the Info.plist generated by wolifecycle.
	 * It would be worth it to go through this and find out if each of these are required, and if their values can be improved.
	 */
	public static String make( final SourceProject sourceProject ) {
		final SourceProject.Type type = sourceProject.type();
		final String bundleName = sourceProject.name();
		final String version = sourceProject.version();
		final String mainJarFileName = sourceProject.name();

		final var infoPlist = new LinkedHashMap<>();
		infoPlist.put( "NSExecutable", bundleName );
		infoPlist.put( "CFBundleDevelopmentRegion", "English" );
		infoPlist.put( "CFBundleExecutable", bundleName );
		infoPlist.put( "CFBundleGetInfoString", "" );
		infoPlist.put( "CFBundleIconFile", type.isApp() ? "WOAfile.icns" : "" );
		infoPlist.put( "CFBundleIdentifier", "com.apple.myapp" );
		infoPlist.put( "CFBundleInfoDictionaryVersion", "6.0" );
		infoPlist.put( "CFBundleName", type.isApp() ? "WOA" : "WOF" );
		infoPlist.put( "CFBundlePackageType", type.isApp() ? "APPL" : "FMWK" );
		infoPlist.put( "CFBundleShortVersionString", version );
		infoPlist.put( "CFBundleSignature", "webo" );
		infoPlist.put( "CFBundleVersion", version );
		infoPlist.put( "Java", Map.of( "JVMVersion", "1.5+" ) );
		infoPlist.put( "NSJavaClientRoot", type.isApp() ? "Contents/WebServerResources/Java" : "WebServerResources/Java" );
		infoPlist.put( "NSJavaNeeded", true );
		infoPlist.put( "NSJavaPath", List.of( mainJarFileName ) );
		infoPlist.put( "NSJavaPathClient", mainJarFileName );
		infoPlist.put( "NSJavaRoot", type.isApp() ? "Contents/Resources/Java" : "Resources/Java" );

		if( type.isFramework() ) {
			// Note; wolifecycle seems to just set this to true for all frameworks, regardless of if they have components or not.
			// We're replicating that, but it's worth looking into // Hugi 2021-07-14
			infoPlist.put( "Has_WOComponents", true );

			final var principalClassName = sourceProject.principalClassName();

			if( principalClassName != null && !principalClassName.isEmpty() ) {
				infoPlist.put( "NSPrincipalClass", principalClassName );
			}
		}

		return new PlistSerialization( infoPlist ).toString();
	}
}