package ng.packaging;

public class InfoPlist {

	public static String make( final String applicationName, final String version, final String jarFileName ) {
		String infoPlistString = Util.readTemplate( "info-plist" );
		infoPlistString = infoPlistString.replace( "${NSExecutable}", applicationName );
		infoPlistString = infoPlistString.replace( "${CFBundleExecutable}", applicationName );
		infoPlistString = infoPlistString.replace( "${CFBundleIconFile}", "WOAfile.icns" );
		infoPlistString = infoPlistString.replace( "${CFBundleName}", "WOA" );
		infoPlistString = infoPlistString.replace( "${CFBundlePackageType}", "APPL" );
		infoPlistString = infoPlistString.replace( "${CFBundleShortVersionString}", version );
		infoPlistString = infoPlistString.replace( "${CFBundleVersion}", version );
		infoPlistString = infoPlistString.replace( "${NSJavaPath}", jarFileName );
		infoPlistString = infoPlistString.replace( "${NSJavaPathClient}", jarFileName );
		// FIXME: Has_WOComponents (for frameworks) // Hugi 2021-07-13
		// FIXME: NSPrincipalClass (for frameworks) // Hugi 2021-07-13
		return infoPlistString;
	}
}