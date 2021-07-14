package ng.packaging;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class InfoPlist {

	public static String make( final String applicationName, final String version, final String jarFileName ) {
		final var infoPlist = new LinkedHashMap<>();
		infoPlist.put( "NSExecutable", applicationName );
		infoPlist.put( "CFBundleDevelopmentRegion", "English" );
		infoPlist.put( "CFBundleExecutable", applicationName );
		infoPlist.put( "CFBundleGetInfoString", "" );
		infoPlist.put( "CFBundleIconFile", "WOAfile.icns" );
		infoPlist.put( "CFBundleIdentifier", "com.apple.myapp" );
		infoPlist.put( "CFBundleInfoDictionaryVersion", "6.0" );
		infoPlist.put( "CFBundleName", "WOA" );
		infoPlist.put( "CFBundlePackageType", "APPL" );
		infoPlist.put( "CFBundleShortVersionString", version );
		infoPlist.put( "CFBundleSignature", "webo" );
		infoPlist.put( "CFBundleVersion", version );
		infoPlist.put( "Java", Map.of( "JVMVersion", "1.5+" ) );
		infoPlist.put( "NSJavaClientRoot", "Contents/WebServerResources/Java" );
		infoPlist.put( "NSJavaNeeded", "FIXME" ); // FIXME: This is a boolean in the original version, add support for boolean serialization // Hugi 2021-07-14
		infoPlist.put( "NSJavaPath", Arrays.asList( jarFileName ) );
		infoPlist.put( "NSJavaPathClient", jarFileName );
		infoPlist.put( "NSJavaRoot", "Contents/Resources/Java" );
		// FIXME: Has_WOComponents (for frameworks) // Hugi 2021-07-13
		// FIXME: NSPrincipalClass (for frameworks) // Hugi 2021-07-13
		return new PlistSerialization( infoPlist ).toString();
	}
}