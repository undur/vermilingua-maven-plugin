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

`vermilingua` has seen production use for years in multiple
applications and frameworks (including a fork of Wonder) and can
be considered stable and safe to use.

## Usage

Replace the `wolifecycle-maven-plugin` `<plugin>` element in your
`pom.xml` with `vermilingua-maven-plugin`:

```xml
<plugin>
  <groupId>is.rebbi</groupId>
  <artifactId>vermilingua-maven-plugin</artifactId>
  <version>1.1.2</version>
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
* `createArchives`: when set `true`, `vermilingua` will generate
  compressed archives of the build products (application bundle, and
  "WebServerResources" bundle if created) using `tar` and `gzip`.

## Differences from `wolifecycle-maven-plugin`

`vermilingua` does _not_ support:

* Building of `.war` files (servlet deployment).
* Building of `.framework` bundles. We only build Maven-style JAR frameworks.
* `flattenComponents` configuration parameter. Any folder structure in
  `src/main/components` is flattened, as WebObjects can't locate components in sub-folders at runtime anyway.
* `flattenResources` configuration parameter. Unsupported since it's use case is unclear.

Other differences include:

* `.patternset` files from the `woproject` folder aren't used at all. Instead, the build relies on the source project using the standard folder structure.
* Default location for WebObjects bundle resources is
  `src/main/woresources` instead of `src/main/resources` (which is
  now reserved for Java classpath resources As God Intended).
* When building applications, `${build.finalName}` (set in the POM)
  will only affect the name of the WOA folder. The insides of two WOAs
  made from the same project, but compiled with different
  `finalName`s, will look exactly the same.
* The launch script no longer requires or reads the `NEXT_ROOT`
  environment variable.
* The launch script no longer passes `-DWORootDirectory` or
  `-DWOLocalRootDirectory` to the JVM, as applications are now
  self-contained bundles with no dependency on a system-wide
  WebObjects installation.
* The launch script no longer sets default heap sizes (`-Xms32m`,
  `-Xmx64m`, `-XX:NewSize=2m`) on macOS, deferring to the JVM's
  built-in ergonomics which select appropriate defaults based on
  available system memory.
* No Windows launch script (`.cmd`) or Windows-specific files
  (`CLSSPATH.TXT`, `SUBPATHS.TXT`, `Contents/Windows/`) are generated.
  If deploying on Windows, use the standard launch script via WSL or Git Bash.
* The `Contents/MacOS/` directory no longer contains a redundant copy
  of the launch script.
* Support for the Rhapsody and classic Mac OS platforms has been
  removed from the launch script.
* No `MacOSXServerClassPath.txt` is generated (it was identical to
  `MacOSClassPath.txt` and only used by the Rhapsody platform path).