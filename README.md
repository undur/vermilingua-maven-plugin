![](https://github.com/undur/vermilingua-maven-plugin/workflows/build/badge.svg)
<img align="right" src="https://www.hugi.io/github/img/antkiller2.png" width="60">

## What is This? A Center for Ants? 

No, it's a pure Maven plugin for building
[WebObjects](https://en.wikipedia.org/wiki/WebObjects) and [Project
Wonder](https://github.com/wocommunity/wonder) applications and
frameworks.  By "pure Maven", we mean it doesn't use the [WOProject
Ant Tasks](https://wiki.wocommunity.org/display/WOL/WOProject-Ant).
In theory, this makes it easier to modify the plugin and improve the
build process.  Eventually it will be _close to_ a drop-in alternative
to [WOLifecycle Maven
Plugin](https://github.com/wocommunity/wolifecycle-maven-plugin),
though some features have not been implemented yet (see below).

This plugin is already in production use on a few applications and
frameworks (including a fork of Wonder) and works well.

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

## Work in progress

* Applications: Currently only generates a WOA bundle (i.e. does not
  generate split install artifacts nor compressed artifacts).
* `flattenComponents` has not yet been implemented.
* `flattenResources` has not yet been implemented.

## Differences from `wolifecycle-maven-plugin`


* Patternsets are not supported, primarily because I don't use them
  myself.
* No support for building WAR files (servlet projects).
* Only generates Maven-style JAR frameworks (not a `.framework` folder
  bundle for use with Ant).
* Default location for WebObjects bundle resources is
  `src/main/woresources` rather than `src/main/resources` (which is
  now reserved for Java classpath resources As God Intended).
* `flattenComponents` will default to true (once implemented) since
  WebObjects doesn't know how to locate components in sub-folders, in
  production anyway.
* When building applications, `${build.finalName}` (set in the POM)
  will only affect the name of the WOA folder. The insides of two WOAs
  made from the same project, but compiled with different
  `finalName`s, will look exactly the same.
