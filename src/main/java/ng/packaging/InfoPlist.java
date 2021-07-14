package ng.packaging;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InfoPlist {

	public static String make( final SourceProject.Type type, final String applicationName, final String version, final String jarFileName, final String principalClassName ) {
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
		infoPlist.put( "NSJavaPath", List.of( jarFileName ) );
		infoPlist.put( "NSJavaPathClient", jarFileName );
		infoPlist.put( "NSJavaRoot", "Contents/Resources/Java" );

		// FIXME: Has_WOComponents (for frameworks) // Hugi 2021-07-13

		if( type == SourceProject.Type.Framework ) {
			Objects.requireNonNull( principalClassName );
			infoPlist.put( "NSPrincipalClass", principalClassName );
		}

		return new PlistSerialization( infoPlist ).toString();
	}
}