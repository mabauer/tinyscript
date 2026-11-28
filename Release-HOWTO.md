# How to create a release for _tinyscript_

Releases are version-tagged in Git. Pushing a `vX.Y.Z` tag automatically
triggers the GitHub Actions Docker workflow, which builds and pushes
`mkbauer/tinyscript:X.Y.Z` and `:latest` to Docker Hub — no manual Docker steps
required.

## 1. Update version numbers

    ./set-version.sh X.Y.Z

The script does what used to be several manual steps, and checks the result:

- `tycho-versions-plugin:set-version` for the OSGi modules: the root `pom.xml`,
  all child `pom.xml` files, every `META-INF/MANIFEST.MF` (`Bundle-Version`),
  `feature.xml` and `category.xml`, and the `de.mkbauer.tinyscript` dependency
  of `repl` and `standalone.tests`;
- `versions:set` for the plain Maven modules `repl`, `standalone.tests` (in the
  Tycho reactor, but without a parent) and `webdemo` (outside of it);
- the `de.mkbauer.tinyscript` dependency in
  `de.mkbauer.tinyscript.webdemo/pom.xml`, which `versions:set` does not update.

## 2. Check the result

The script fails if the old version is left anywhere, or if the new version is
in fewer or more places than the old one was (21 places in 17 files as of 0.31).
It refuses to start if a `pom.xml`, `MANIFEST.MF`, `feature.xml` or
`category.xml` has uncommitted changes, so `git diff` shows exactly its changes.
It does not commit. It uses JDK 25 via `/usr/libexec/java_home` where that
exists (macOS); elsewhere `JAVA_HOME` must point to a JDK 25.

## 3. Write release notes

For every release that is more than a small fix, write release notes and publish
them as a GitHub release for the tag (step 4). Structure (see v0.19.0 as
example):

- one sentence on what the release is about;
- **Incompatible changes** first: everything that can break existing scripts,
  each with what to write instead;
- **New**: language features, builtins, REPL / web demo, grouped by topic;
- **Fixes**: bugs that users could have noticed;
- links as full URLs to the tagged version (GitHub release pages do not resolve
  relative links).

Collect the content from the merged pull requests since the last tag:

    git log --oneline vOLD_VERSION..HEAD --first-parent

## 4. Commit, tag, push, publish

    git add -A
    git commit -m "Release version X.Y.Z"
    git tag vX.Y.Z
    git push && git push --tags
    gh release create vX.Y.Z --title "tinyscript X.Y.Z" --notes-file release-notes.md

Pushing the `vX.Y.Z` tag triggers `.github/workflows/docker.yml`, which:

- Builds the Tycho reactor (skip tests)
- Builds the webdemo fat JAR (Vitest still runs; Spring Boot tests skipped)
- Pushes `mkbauer/tinyscript:X.Y.Z` and `:latest` to Docker Hub

Monitor progress at: https://github.com/mabauer/tinyscript/actions

## 5. Bump to next SNAPSHOT

    ./set-version.sh X.Y.(Z+1)-SNAPSHOT

    git add -A
    git commit -m "Bump to X.Y.(Z+1)-SNAPSHOT"
    git push

## Notes

- **Use the Maven wrapper:** run `./mvnw`, not a system `mvn`. The wrapper pins
  Maven 3.9.16; Maven 3.10+ fails on the Tycho build with
  `Invalid Collect Request: null`.
- **Java 25 required for Maven:** all modules compile for Java 25 (and
  `tycho-versions-plugin` needs at least Java 21). If your shell's default
  `java` is not 25, prefix every `./mvnw` command with
  `export JAVA_HOME=$(/usr/libexec/java_home -v 25)` (macOS) or set `JAVA_HOME`
  appropriately before running any Maven command in this procedure
  (`set-version.sh` does this itself on macOS).
- **GitHub secrets required:** the Docker workflow reads `DOCKERHUB_USERNAME`
  and `DOCKERHUB_TOKEN` from repository secrets (Settings → Secrets and
  variables → Actions → Repository secrets).
- **CI workflow** (`.github/workflows/ci.yml`) runs the full test suite on every
  push to master and on pull requests — no action needed.
- **Docker base image:** `eclipse-temurin:25-jre-noble` (Ubuntu 24.04; `openjdk`
  images are deprecated). The plain `25-jre` tag is Ubuntu 26.04, whose image
  ships `pebble` with a vulnerable Go runtime (CVE-2026-97054).
- See `docs/build.md` for a full description of the build system.
