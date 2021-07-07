# Vermilingua

A pure maven plugin for building Wonder applications and frameworks, "pure maven" meaning it doesn't use the woproject ant tasks. In theory, this should give us more flexibility when it comes to performing changes/modifications to the plugin and the build process.

## The build process 

* Generate the woa's directory structure
* Copy the compiled jar to `Contents/Resources/Java/App.jar`
* Copy dependency jars to `Contents/Resources/Java/`. Structure mimics that of a maven repo
* Copy webserver-resources to `Contents/WebServerResources`
* Process dependency jars to find frameworks containing `WebServerResources`. Copy the data from the jars to generate the content of `Contents/Frameworks/.../WebServerResources/`
* Generate  ClassPath.txt files *(do we really need platform dependent ones?)*
* Generate `Info.plist`
* Generate run scripts

## Bundle structure

```java
AnApp.woa
	AnApp
	AnApp.cmd
	Contents
		Frameworks
			SomeFramework.framework
  				WebServerResources
		Info.plist
		MacOS
			[...]
		Resources
			Java
				AnApp.jar
				some/other/dependency/2.0.0/dependency-2.0.0.jar
		UNIX
			[...]
		WebServerResources
		Windows
			[...]
```

