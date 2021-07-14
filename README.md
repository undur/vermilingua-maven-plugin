<img align="right" src="https://www.hugi.io/github/img/antkiller2.png" width="60">

## What is This? A Center for Ants? 

No, it's a pure maven plugin for building Wonder applications and frameworks, "pure maven" meaning it doesn't use the woproject ant tasks. In theory, this makes it easier to modify the plugin and improve the build process.

The plugin is already in real production use on a couple of apps and frameworks (including a fork of Wonder) and works fine for the most (although not all features hav been implemented yet.

## Usage

* Clone this repository and run `mvn install` on it to install the plugin
* Replace the wolifecycle-maven-plugin <plugin> element in your pom.xml with

```xml
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

## Work in progress

* Applications: Currently only generates a WOA (i.e. does not generate split install artifacts nor compressed artifacts).
* `flattenComponents`  has not yet been implemented.
* `flattenResources` has not yet been implemented.

## Differences from wolifecycle-maven-plugin

* Patternsets are not supported, primarily because I don't use them myself.
* No support for building war files (servlet projects).
* Only generates maven-style jar frameworks (not a .framework folder bundle for use with Ant).
* Default location for wo bundle resources is `src/main/woresources` rather than `src/main/resources` (which is now reserved for java classpath resources As God Intended).
* `flattenComponents` will default to true (once implemented) since WO doesn't know how to locate components in subfolders in production anyway.