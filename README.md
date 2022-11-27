![](https://github.com/undur/vermilingua-maven-plugin/workflows/build/badge.svg)
<img align="right" src="https://www.hugi.io/github/img/antkiller2.png" width="60">

## What is This? A Center for Ants? 

No, it's a pure Maven plugin for building
[WebObjects](https://en.wikipedia.org/wiki/WebObjects) and [Project
Wonder](https://github.com/wocommunity/wonder) applications and
frameworks.  "Pure Maven" means it doesn't use the [WOProject
Ant Tasks](https://wiki.wocommunity.org/display/WOL/WOProject-Ant)
which makes it run faster and makes it easier to improve the plugin and
build process.  It's close to a drop-in alternative to the [WOLifecycle Maven
Plugin](https://github.com/wocommunity/wolifecycle-maven-plugin),
although with some differeces (see below).

This plugin is already used to build many production applications and
frameworks (including a fork of Wonder).

## Usage

1. Clone this repository and run `mvn install` on it to install the
   plugin locally.
2. Replace the `wolifecycle-maven-plugin` `<plugin>` element in your
   `pom.xml` with:

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

The `<woresourcesFolderName>` configuration parameter is to keep
compatibility with current projects. This plugin defaults to use the
folder `src/main/woresources` for WebObjects bundle resources, rather
than the `src/main/resources` folder.

## Differences from `wolifecycle-maven-plugin`


* Patternsets are not supported
* No support for building WAR files (servlet projects).
* Only generates Maven-style JAR frameworks (not a `.framework` folder
  bundle for use with Ant).
* Default location for WebObjects bundle resources is
  `src/main/woresources` rather than `src/main/resources` (which is
  now reserved for Java classpath resources As God Intended).
* `flattenComponents` defaults to true (WO doesn't know how to locate components in sub-folders in production anyway).
* When building applications, `${build.finalName}` (set in the POM)
  will only affect the name of the WOA folder. The insides of two WOAs
  made from the same project, but compiled with different
  `finalName`s, will look exactly the same.
  
  ## Work in progress

* Applications: Currently only generates a WOA bundle (i.e. does not
  generate split install artifacts nor compressed artifacts).
* `flattenResources` has not yet been implemented.