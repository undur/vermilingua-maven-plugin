# Vermilingua

A pure maven plugin for building Wonder applications and frameworks, "pure maven" meaning it doesn't use the woproject ant tasks. In theory, this should give us more flexibility when it comes to performing changes/modifications to the plugin and the build process.

## The build process 

* Generate the woa's directory structure
* Copy the compiled jar to `Contents/Resources/Java/App.jar`
* Copy dependency jars to `Contents/Resources/Java/`. Structure mimics that of a maven repo
* Copy app resources to `Contents/Resources/`
* Copy app components to `Contents/Resources/`
* Copy app webserver-resources to `Contents/WebServerResources`
* Process dependency jars to find those containing `WebServerResources`. Copy those from the jars to generate the content of `Contents/Frameworks/.../WebServerResources/` for each framework
* Generate  ClassPath.txt files *(do we really need platform dependent ones?)*
* Generate `Info.plist`
* Generate run scripts

## Bundle structure

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
				AnApp.jar
				some/other/dependency/2.0.0/dependency-2.0.0.jar
		UNIX
			UNIXClassPath.txt
		WebServerResources
		Windows
			CLSSPATH.TXT
			AppName.cmd
			SUBPATHS.cmd
```

