# Build info file — planning notes

Status: **planning** (not yet scheduled for a release).

## Idea

Have vermilingua write a build-info properties file into every built WOA, by default, capturing the provenance of the build. The value proposition over the existing standalone plugins (e.g. `git-commit-id-maven-plugin`) is that this would be **zero-config and on by default** — the WOA carries its own provenance automatically, which a per-project plugin can't be.

## File

Written to:

```
Contents/Resources/vermilingua-build-info.properties
```

(Placed under `Resources` so it's bundled and readable by the running app like any other resource.)

## Base set (dependency-free, default-on)

Every field here is available with no new dependency, no shelling out to `git`, and no parsing of git's object store — just plaintext `.git` reads plus the Maven project model and JVM system properties.

```
buildDateTime={ISO-8601 timestamp, UTC, e.g. 2026-06-22T14:03:11Z}
buildAppVersion={mavenProject.getVersion()}
buildJavaVersion={java.version at build time}
buildVermilinguaVersion={the plugin's own version}
buildCommitSha={full 40-char SHA}     # omitted if no git repo
buildBranch={branch name}              # omitted if no repo, or detached HEAD
```

### Why these

- **buildDateTime** — "when was this built". Always available; no git involved.
- **buildAppVersion** — most-wanted field; with the SHA, answers "is this the build I think it is". `mavenProject.getVersion()`.
- **buildJavaVersion** — WO is sensitive to JDK version; "built on 21, running on 17" is a real failure mode. From `java.version` system property at build time.
- **buildVermilinguaVersion** — which plugin version produced the bundle; useful when debugging build-output quirks across plugin upgrades.
- **buildCommitSha** — full SHA from `.git`. Full (not abbreviated) because abbreviating requires resolving against the object store, which we don't want to hand-roll. Full is both the zero-work and the more useful choice.
- **buildBranch** — branch name from `.git/HEAD`.

## Rules / decisions

1. **No git repo present → still write the file, but omit the git keys** (`buildCommitSha`, `buildBranch`). The non-git half is always knowable, so consumers never have to handle "file might not exist". (Covers building from an exported tarball, or running `mvn` outside a repo.)
2. **Detached HEAD → omit `buildBranch`.** During a tag-based deploy build, `.git/HEAD` holds a raw SHA, not `ref: refs/heads/...`, so there is no branch. Do NOT write something misleading (not the SHA, not `HEAD`).
3. **Packed refs fallback.** Reading the branch SHA must fall back to `.git/packed-refs` when `.git/refs/heads/<branch>` doesn't exist (the normal state after `git gc` or a fresh clone). This is part of the base tier, not extra scope.
4. **SHA format:** full 40 characters, as found in the ref file.
5. **Timestamp:** ISO-8601 with offset, UTC preferred, so two builds are comparable regardless of build-host timezone.
6. **Key naming:** consistent camelCase — note `buildCommitSha`, not `buildCommitSHA`.
7. **Graceful by default.** A default-on feature must never fail the build because of missing/odd git state — omit fields rather than erroring.

## The "git line"

There's a hard boundary in how much is cheaply available:

- **Cheap (plaintext `.git` reads):** current SHA (`.git/HEAD` → direct SHA, or `ref:` → `.git/refs/heads/<branch>` → `.git/packed-refs` fallback) and branch name.
- **NOT cheap:** anything inside the commit object — **commit date, commit message, author, tag, dirty-flag**. These require finding the object (loose *or* packfile — and after gc/clone it's almost always in a packfile, with delta compression + a binary index), zlib-inflating, and parsing. Can't be hand-rolled robustly.

So commit date is the natural fork in the road: the dependency-free base set stops just before it.

## Tiers below the base set (NOT in base scope)

- **Build host / username** (`hostname`, `user.name`) — cheap and answers "CI or someone's laptop?", but username is mildly sensitive. Make it the first **opt-in** extra rather than a default.
- **Git extras** (commit date, commit message, author, tag, dirty working-tree flag) — all on the wrong side of the git line. Only viable if we accept **either** shelling out to `git` (needs `git` on PATH; fails in some CI/tarball contexts) **or** adding **JGit** (heavy dependency on every build). If we accept one of those, all of these fields come essentially for free together — at which point the feature is "shell out / JGit and capture a handful of fields", and the "why not just use git-commit-id-maven-plugin" comparison gets sharper. Decide deliberately.
- **Resolved key dependency versions** (e.g. Wonder / ERExtensions) — useful, but "which deps, where do you stop?" makes it ill-defined. Maybe later.
- **CI metadata** (build number, job URL) — reading CI env vars is endless and CI-specific. Out of scope; if anything, let users inject their own.

## Open questions for execution time

- Should any of the base fields be individually toggleable, or is it all-or-nothing (plus the opt-in tiers)?
- Confirm `Contents/Resources/` is the right home vs. folding (some of) this into `Info.plist`.
- Naming: `vermilingua-build-info.properties` vs. something shorter / namespaced.
