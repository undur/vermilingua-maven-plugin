package ng.packaging;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InfoPlist {

	public static String make( final SourceProject.Type type, final String applicationName, final String version, final String mainJarFileName, final String principalClassName ) {
		final var infoPlist = new LinkedHashMap<>();
		infoPlist.put( "NSExecutable", applicationName );
		infoPlist.put( "CFBundleDevelopmentRegion", "English" );
		infoPlist.put( "CFBundleExecutable", applicationName );
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
		infoPlist.put( "NSJavaClientRoot", "Contents/WebServerResources/Java" );
		infoPlist.put( "NSJavaNeeded", true );
		infoPlist.put( "NSJavaPath", List.of( mainJarFileName ) );
		infoPlist.put( "NSJavaPathClient", mainJarFileName );
		infoPlist.put( "NSJavaRoot", "Contents/Resources/Java" );

		// FIXME: WE probably need to check if the framework actually contains components. Don't lie // Hugi 2021-07-14
		if( !type.isApp() ) {
			infoPlist.put( "Has_WOComponents", true );
		}

		if( type == SourceProject.Type.Framework ) {
			if( principalClassName != null && !principalClassName.isEmpty() ) {
				infoPlist.put( "NSPrincipalClass", principalClassName );
			}
		}

		return new PlistSerialization( infoPlist ).toString();
	}
}