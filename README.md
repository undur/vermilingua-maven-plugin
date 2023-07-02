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
  <version>1.0.3</version>
  <extensions>true</extensions>
  <!-- Configuration only if you're using the old 'resources' name for the resources folder
  <configuration>
    <woresourcesFolderName>resources</woresourcesFolderName>
  </configuration>
  -->
</plugin>
```

### Configuration

There are several optional parameters.

* `woresourcesFolderName`: provided for compatibility with
`wolifecycle-maven-plugin`. Without it, `vermilingua` defaults to
`src/main/woresources` for WebObjects bundle resources, rather than
`src/main/resources`, allowing that folder to serve it's designated
standard Maven purpose, which is to keep Java classpath resources.
* `performSplit`: when set `true`, `vermilingua` will generate an
  additional "WebServerResources" bundle for "split deployments".

## Differences from `wolifecycle-maven-plugin`

There are some features in `wolifecycle-maven-plugin` that are not
supported at all.

* Ant-style `.patternset` files: you can throw out your `woproject`
  folders.
* Building WAR files for servlet projects.
* `.framework` bundles: only generates Maven-style JAR frameworks.
* `flattenComponents` configuration parameter: any folder structure in
  `src/main/components` is flattened, as WebObjects doesn't know how
  to locate components in sub-folders at runtime anyway.
* `flattenResources` configuration parameter: it's not clear what the
  use case is for this parameter, as WebObjects _can_ find other
  resources in sub-folders at runtime.

Other differences include:

* Default location for WebObjects bundle resources is
  `src/main/woresources` rather than `src/main/resources` (which is
  now reserved for Java classpath resources As God Intended).
* When building applications, `${build.finalName}` (set in the POM)
  will only affect the name of the WOA folder. The insides of two WOAs
  made from the same project, but compiled with different
  `finalName`s, will look exactly the same.
