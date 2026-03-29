# Changes

## 1.1.0

### New bundle structure

The platform-specific directories (`Contents/MacOS/`, `Contents/UNIX/`, `Contents/Windows/`) and their classpath files (`MacOSClassPath.txt`, `UNIXClassPath.txt`, `MacOSXServerClassPath.txt`, `CLSSPATH.TXT`) have been replaced by two simple files at the `.woa` root:

- **`config.txt`** — launch configuration (application class, JVM executable, JVM options, JDB settings)
- **`classpath.txt`** — one classpath entry per line

The new bundle structure:

```
AppName.woa/
    AppName          (launch script)
    config.txt
    classpath.txt
    Contents/
        Info.plist
        Frameworks/
        Resources/
        WebServerResources/
```

### Unified `launch.*` configuration

Launch properties (`jvm`, `jvmOptions`, `jdb`, `jdbOptions`) can be set at three levels, using a consistent `launch.` prefix:

**In `build.properties`** (the new prefixed form is preferred; the old unprefixed form still works but logs a deprecation warning):

```properties
launch.jvm = /opt/jdk-26/bin/java
launch.jvmOptions = -Xmx2g
launch.jdb = /opt/jdk-26/bin/jdb
launch.jdbOptions = -sourcepath src
```

**At build time** via system properties:

```
mvn package -Dlaunch.jvm=/opt/jdk-26/bin/java
```

**At runtime** via command line arguments to the launch script:

```
./MyApp.woa/MyApp "-launch.jvm=/opt/jdk-26/bin/java" "-launch.jvmOptions=-Xmx4g --add-exports java.base/sun.security.action=ALL-UNNAMED"
```

Runtime `-launch.*` arguments are consumed by the launch script and not passed through to the application. Values containing spaces must be quoted.

### Layered property resolution

Build properties support a layered override mechanism. Values are resolved in this order (first match wins):

1. **Runtime `-launch.*` arguments** — passed when launching the application
2. **Build-time system properties** — e.g. `mvn package -Dlaunch.jvm=...`
3. **Environment-specific properties file** — activated with `-Dbuild.env=<name>`, loads `build.properties.<name>`
4. **`build.properties`** — the base project defaults (supports both `launch.jvm` and legacy `jvm` forms)
5. **Built-in defaults** — `java`, `jdb`, empty strings

This allows environment-specific configuration without modifying committed files. For example, with a `build.properties.prod` containing only:

```properties
launch.jvm = /opt/jdk-26/bin/java
```

Building with `mvn package -Dbuild.env=prod` will use that JVM path while inheriting all other values from `build.properties`.

### Standardized config key naming

Config keys are now consistent across `build.properties`, `config.txt`, and `launch.*` arguments: `principalClass`, `jvm`, `jvmOptions`, `jdb`, `jdbOptions`. The old mixed-case names (`ApplicationClass`, `JVM`, `JVMOptions`, etc.) in config.txt have been replaced.

### Optional archive creation (`createArchives`)

When `<createArchives>true</createArchives>` is set in the plugin configuration, the build will create tar.gz archives of the build products and attach them as Maven artifacts:

- **`{finalName}.woapplication.tar.gz`** — the complete `.woa` bundle, attached as the project's primary artifact
- **`{finalName}.wowebserverresources.tar.gz`** — the split webserver resources (only if `performSplit` is also enabled), attached as a secondary artifact with classifier `wowebserverresources`

This allows `mvn install` to place the archives in the local repository and `mvn deploy` to publish them to a remote repository.

```xml
<plugin>
    <groupId>is.rebbi</groupId>
    <artifactId>vermilingua-maven-plugin</artifactId>
    <version>1.1.0</version>
    <extensions>true</extensions>
    <configuration>
        <createArchives>true</createArchives>
    </configuration>
</plugin>
```

## 1.0.6

The application launch script (the shell script generated at the root of your `.woa` bundle) has been significantly simplified, removing legacy platform support and obsolete configuration that dates back to the NeXTSTEP/OpenStep era.

### Removed: Default JVM heap size arguments

The launch script no longer sets `-Xms32m`, `-Xmx64m`, or `-XX:NewSize=2m` on macOS. These values were hardcoded in the original Apple launch script for machines with 256 MB of RAM and have no place in modern deployments.

Without these overrides, the JVM's built-in ergonomics will now determine heap sizing automatically based on the host machine's available resources. On any JVM since JDK 9, the defaults are:

| Parameter | Default |
|---|---|
| Initial heap (`-Xms`) | 1/64 of physical memory |
| Maximum heap (`-Xmx`) | 1/4 of physical memory |
| Garbage collector | G1GC (on server-class machines) |

For example, on a machine with 16 GB of RAM, the JVM will default to an initial heap of ~256 MB and a maximum of ~4 GB — compared to the old script's 32 MB / 64 MB cap. On a server with 64 GB, you'd get up to ~16 GB max heap automatically.

In containerized environments (Docker, Kubernetes), the JVM respects cgroup memory limits, so these ergonomics apply to the container's memory allocation, not the host's total RAM.

Note that this change only affected applications launched on macOS. On Linux, the old script did not set heap defaults either, so there is no behavioral change on Linux deployments.

If you need explicit control over heap sizing, you can set them in `JVMOptions` in your `build.properties`:

```
jvmOptions = -Xms256m -Xmx2g
```

Or pass them on the command line when launching your application:

```
./MyApp.woa/MyApp -Xms256m -Xmx2g
```

### Removed: `-DWORootDirectory` and `-DWOLocalRootDirectory` system properties

The launch script no longer passes `-DWORootDirectory` or `-DWOLocalRootDirectory` as JVM arguments.

These properties pointed to system-wide WebObjects installation directories — `/System` on macOS, or `$NEXT_ROOT` on other platforms — where shared frameworks and libraries were installed on the server. This concept has been obsolete for many years. Modern WO/ng-objects applications are deployed as self-contained bundles with all dependencies packaged inside the `.woa`.

If any code in your application calls `System.getProperty("WORootDirectory")` or `System.getProperty("WOLocalRootDirectory")`, it will now receive `null`. If you encounter issues, you can restore these properties via `JVMOptions` in `build.properties`:

```
jvmOptions = -DWORootDirectory=/System -DWOLocalRootDirectory=
```

Or on the command line:

```
./MyApp.woa/MyApp -DWORootDirectory=/System -DWOLocalRootDirectory=
```

### Removed: `NEXT_ROOT` environment variable dependency

The launch script no longer reads or requires the `NEXT_ROOT` environment variable. Previously, on non-macOS UNIX platforms, the script would check for `NEXT_ROOT` and print a warning if it was unset, falling back to `/tmp/nextroot`.

`NEXT_ROOT` was the NeXTSTEP/OpenStep installation prefix, used to locate the WebObjects system installation on non-Apple UNIX platforms (Solaris, HP-UX, etc.). If you had `NEXT_ROOT` set in your environment or deployment scripts, it is now ignored and can be cleaned up.

### Removed: `WOROOT`, `LOCALROOT`, and `HOMEROOT` classpath token substitution

The classpath file previously supported four path tokens that were substituted at launch time: `APPROOT`, `WOROOT`, `LOCALROOT`, and `HOMEROOT`. Only `APPROOT` is now substituted.

The build has generated classpath entries using only `APPROOT` for a long time. The other tokens pointed to server-wide installation directories that no longer exist. `APPROOT` continues to work exactly as before, resolving to the `Contents/` directory of the `.woa` bundle.

If you have manually edited classpath files inside a built `.woa` and used `WOROOT`, `LOCALROOT`, or `HOMEROOT` tokens, those tokens will no longer be replaced, causing `ClassNotFoundException` errors.

### Removed: Rhapsody, classic Mac OS, and Solaris platform support

The launch script no longer has special handling for the `Rhapsody` or `Mac OS` platform identifiers (as reported by `uname -s`), and no longer prepends Solaris-specific `/usr/xpg4/bin` to the `PATH`. The platform detection now simply distinguishes between Darwin (macOS) and everything else (Linux/UNIX) for the purpose of selecting the correct classpath file.

### Changed: Debug mode detection

The script now detects `-agentlib:jdwp` (the modern JDWP flag) in addition to the deprecated `-Xrunjdwp` when checking the `_JAVA_OPTIONS` environment variable for debug mode. Debug mode detection via command line flags (`-NSPBDebug`, `-NSJavaDebugging YES`) and `jdb` support are unchanged.

### Summary

| Aspect | Before | After |
|---|---|---|
| Default heap size (macOS) | `-Xms32m -Xmx64m -XX:NewSize=2m` | JVM ergonomic defaults |
| Default heap size (Linux) | JVM ergonomic defaults | JVM ergonomic defaults (unchanged) |
| `WORootDirectory` system property | `/System` (macOS) or `$NEXT_ROOT` (Linux) | Not set |
| `WOLocalRootDirectory` system property | Empty (macOS) or `$NEXT_ROOT/Local` (Linux) | Not set |
| `WOUserDirectory` system property | Set to invoking directory | Set to invoking directory (unchanged) |
| `NEXT_ROOT` required on Linux | Yes (warned if missing) | No |
| Classpath token substitution | `APPROOT`, `WOROOT`, `LOCALROOT`, `HOMEROOT` | `APPROOT` only |
| JDWP detection | `-Xrunjdwp` only | `-Xrunjdwp` and `-agentlib:jdwp` |
| Platform support | Darwin, Mac OS, Rhapsody, Solaris, UNIX | Darwin and UNIX |
