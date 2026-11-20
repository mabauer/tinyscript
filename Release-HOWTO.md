How to create a release for *tinyscript*
========================================

Releases are version-tagged in Git. Pushing a `vX.Y.Z` tag automatically
triggers the GitHub Actions Docker workflow, which builds and pushes
`mkbauer/tinyscript:X.Y.Z` and `:latest` to Docker Hub — no manual Docker
steps required.

## 1. Update version numbers

**OSGi modules** — updates the root `pom.xml`, all child `pom.xml` files, every
`META-INF/MANIFEST.MF` (`Bundle-Version`), and `feature.xml` in one shot:

    ./mvnw org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=X.Y.Z

**Plain Maven modules** — these are outside the Tycho reactor and must be updated
separately:

    ./mvnw versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false \
        -f de.mkbauer.tinyscript.repl/pom.xml
    ./mvnw versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false \
        -f de.mkbauer.tinyscript.webdemo/pom.xml
    ./mvnw versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false \
        -f de.mkbauer.tinyscript.standalone.tests/pom.xml

**Core dependency references** — `versions:set` only updates the project's own
`<version>`, not dependency versions. Manually set the `de.mkbauer.tinyscript`
dependency version to `X.Y.Z` in:

- `de.mkbauer.tinyscript.repl/pom.xml`
- `de.mkbauer.tinyscript.webdemo/pom.xml`

**REPL MANIFEST.MF** — the repl module is outside the Tycho reactor, so
`tycho-versions-plugin` does not touch its `META-INF/MANIFEST.MF`. Update
`Bundle-Version` manually to `X.Y.Z.qualifier` in:

- `de.mkbauer.tinyscript.repl/META-INF/MANIFEST.MF`


## 2. Verify no old version remains

    grep -r "OLD_VERSION" --include="*.xml" --include="MANIFEST.MF" . \
        | grep -v "target/" | grep -v ".git/" | grep -v "bin/"


## 3. Write release notes

For every release that is more than a small fix, write release notes and publish
them as a GitHub release for the tag (step 4). Structure (see v0.19.0 as example):

- one sentence on what the release is about;
- **Incompatible changes** first: everything that can break existing scripts, each
  with what to write instead;
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

    ./mvnw org.eclipse.tycho:tycho-versions-plugin:set-version \
        -DnewVersion=X.Y.(Z+1)-SNAPSHOT
    ./mvnw versions:set -DnewVersion=X.Y.(Z+1)-SNAPSHOT -DgenerateBackupPoms=false \
        -f de.mkbauer.tinyscript.repl/pom.xml
    ./mvnw versions:set -DnewVersion=X.Y.(Z+1)-SNAPSHOT -DgenerateBackupPoms=false \
        -f de.mkbauer.tinyscript.webdemo/pom.xml
    ./mvnw versions:set -DnewVersion=X.Y.(Z+1)-SNAPSHOT -DgenerateBackupPoms=false \
        -f de.mkbauer.tinyscript.standalone.tests/pom.xml

Then manually update the `de.mkbauer.tinyscript` dependency version in
`repl/pom.xml` and `webdemo/pom.xml` to `X.Y.(Z+1)-SNAPSHOT`.

Also update `Bundle-Version` in `de.mkbauer.tinyscript.repl/META-INF/MANIFEST.MF`
to `X.Y.(Z+1).qualifier` (tycho-versions-plugin does not touch the REPL module).

    git add -A
    git commit -m "Bump to X.Y.(Z+1)-SNAPSHOT"
    git push


## Notes

- **Use the Maven wrapper:** run `./mvnw`, not a system `mvn`. The wrapper
  pins Maven 3.9.16; Maven 3.10+ fails on the Tycho build with
  `Invalid Collect Request: null`.
- **Java 25 required for Maven:** all modules compile for Java 25 (and
  `tycho-versions-plugin` needs at least Java 21). If your shell's default `java`
  is not 25, prefix every `./mvnw` command
  with `export JAVA_HOME=$(/usr/libexec/java_home -v 25)` (macOS) or set `JAVA_HOME`
  appropriately before running any Maven command in this procedure.
- **GitHub secrets required:** the Docker workflow reads `DOCKERHUB_USERNAME`
  and `DOCKERHUB_TOKEN` from repository secrets (Settings → Secrets and
  variables → Actions → Repository secrets).
- **CI workflow** (`.github/workflows/ci.yml`) runs the full test suite on every
  push to master and on pull requests — no action needed.
- **Docker base image:** `eclipse-temurin:25-jre-noble` (Ubuntu 24.04; `openjdk` images are deprecated). The plain `25-jre` tag is Ubuntu 26.04, whose image ships `pebble` with a vulnerable Go runtime (CVE-2026-97054).
- See `docs/build.md` for a full description of the build system.
