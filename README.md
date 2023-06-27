![](https://github.com/undur/vermilingua-maven-plugin/workflows/build/badge.svg)
<img align="right" src="https://www.hugi.io/github/img/antkiller2.png" width="60">

## What is This? A Center for Ants? 

No, it's a pure Maven plugin for building
[WebObjects](https://en.wikipedia.org/wiki/WebObjects) and [Project
Wonder](https://github.com/wocommunity/wonder) applications and
frameworks.

"Pure Maven" means it does not use the [WOProject Ant Tasks](https://wiki.wocommunity.org/display/WOL/WOProject-Ant)
which means it runs faster and makes it easier to improve both the plugin and the
build process.  It's close to a drop-in alternative to the [WOLifecycle Maven
Plugin](https://github.com/wocommunity/wolifecycle-maven-plugin),
although with some differences (listed below).

`vermilingua` has seen production use for over a year in multiple
applications and frameworks (including a fork of Wonder) and can
be considered stable and safe to use.

## Usage

Replace the `wolifecycle-maven-plugin` `<plugin>` element in your
`pom.xml` with `vermilingua-maven-plugin`:

```xml
<plugin>
	<groupId>is.rebbi</groupId>
	<artifactId>vermilingua-maven-plugin</artifactId>
	<version>1.0.0</version>
	<extensions>true</extensions>
	<configuration> <!-- Only include this if you're using the old 'resources' name for the resources folder -->
		<woresourcesFolderName>resources</woresourcesFolderName>
	</configuration>
</plugin>
```

The `<woresourcesFolderName>` configuration parameter is for compatibility
with `wolifecycle-maven-plugin`. Without it, `vermilingua` defaults to 
`src/main/woresources` for WebObjects bundle resources, rather
than `src/main/resources`, allowing that folder to serve it's designated standard
maven purpose, which is to keep java classpath resources.


## Differences from `wolifecycle-maven-plugin`

* Patternsets are not supported
* No support for building WAR files (servlet projects).
* Only generates Maven-style JAR frameworks (not a `.framework` folder
  bundle for use with Ant).
* Default location for WebObjects bundle resources is
  `src/main/woresources` rather than `src/main/resources` (which is
  now reserved for Java classpath resources As God Intended).
* `flattenComponents` defaults to true and cannot be changed (WO doesn't know how to locate components in sub-folders in production anyway).
* When building applications, `${build.finalName}` (set in the POM)
  will only affect the name of the WOA folder. The insides of two WOAs
  made from the same project, but compiled with different
  `finalName`s, will look exactly the same.
  
## Work in progress

* Currently only generates the WOA bundle for Applications (i.e. does not
  generate split install artifacts/compressed artifacts).
* `flattenResources` has not yet been implemented.