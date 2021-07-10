<img align="right" src="https://www.hugi.io/github/img/antkiller2.png" width="40">

## What is This? A Center for Ants? 

No, it's a pure maven plugin for building Wonder applications and frameworks, "pure maven" meaning it doesn't use the woproject ant tasks. In theory, this makes it easier to modify the plugin and improve the build process.

## Usage

* Clone this repository and run `mvn install` on it to install the plugin
* Replace the wolifecycle-maven-plugin <plugin> element in your pom.xml with

```
<plugin>
	<groupId>is.rebbi</groupId>
	<artifactId>vermilingua-maven-plugin</artifactId>
	<version>1.0.0-SNAPSHOT</version>
	<extensions>true</extensions>
	<configuration>
		<woresourcesFolderName>resources</woresourcesFolderName>
	</configuration>
</plugin>
```

The `<woresourcesFolderName>` configuration parameter is to keep compatibility with current projects. This plugin defaults to use the folder `src/main/woresources` for wo bundle resources rather than the `src/main/resources` folder.

## Differences from wolifecycle-maven-plugin

* Currently only supports building applications, not frameworks (`woapplication` packaging, not `woframework`)
* No generation of split install artifacts nor compressed artifacts, just generates a WOA.
* Patternsets are not supported, primarily because I don't use them myself
* `flattenComponents` and `flattenResources` have not yet been implemented
* And finally, as mentioned before; the default location for wo bundle resources is `src/main/woresources` rather than `src/main/resources` (which is now reserved for java classpath resources As God Intended).
* No support for building war files (servlet projects)

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
