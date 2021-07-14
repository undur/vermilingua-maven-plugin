## Notes

Just a few notes for the project.

### Build process (`woapplication`)

* Generate the woa's directory structure
* Copy the compiled jar to `Contents/Resources/Java/app.jar`
* Copy dependency jars to `Contents/Resources/Java/`. Structure mimics that of a maven repo
* Copy app resources to `Contents/Resources/`
* Copy app components to `Contents/Resources/`
* Copy app webserver-resources to `Contents/WebServerResources`
* Process dependency jars to find those containing `WebServerResources`. Copy those from the jars to generate the content of `Contents/Frameworks/.../WebServerResources/` for each framework
* Generate launch scripts for MacOS/UNIX/Windows
* Generate the Classpath files
* Generate `Info.plist`
* Post processing *TODO*
  * Flatten components
  * Flatten resources

### Bundle structure (`woapplication`)

```java
AppName.woa
	AppName
	AppName.cmd
	Contents
		Frameworks
			SomeFramework.framework
  				WebServerResources
		Info.plist
		MacOS
			AppName
			MacOSClassPath.txt
			MacOSXServerClassPath.txt
		Resources
			Java
				appname.jar
				some/other/dependency/2.0.0/dependency-2.0.0.jar
		UNIX
			UNIXClassPath.txt
		WebServerResources
		Windows
			CLSSPATH.TXT
			AppName.cmd
			SUBPATHS.cmd
```

### Build process (`woframework`)

*TODO*

### Bundle structure (`woframework`)

*TODO*