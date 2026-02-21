How to create a release for *tinyscript*
========================================

Releases are version-tagged in Git. Pushing a `vX.Y.Z` tag automatically
triggers the GitHub Actions Docker workflow, which builds and pushes
`mkbauer/tinyscript:X.Y.Z` and `:latest` to Docker Hub — no manual Docker
steps required.

## 1. Update version numbers

**OSGi modules** — updates the root `pom.xml`, all child `pom.xml` files, every
`META-INF/MANIFEST.MF` (`Bundle-Version`), and `feature.xml` in one shot:

    mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=X.Y.Z

**Plain Maven modules** — these are outside the Tycho reactor and must be updated
separately:

    mvn versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false \
        -f de.mkbauer.tinyscript.repl/pom.xml
    mvn versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false \
        -f de.mkbauer.tinyscript.webdemo/pom.xml
    mvn versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false \
        -f de.mkbauer.tinyscript.standalone.tests/pom.xml

**Core dependency references** — `versions:set` only updates the project's own
`<version>`, not dependency versions. Manually set the `de.mkbauer.tinyscript`
dependency version to `X.Y.Z` in:

- `de.mkbauer.tinyscript.repl/pom.xml`
- `de.mkbauer.tinyscript.webdemo/pom.xml`


## 2. Verify no old version remains

    grep -r "OLD_VERSION" --include="*.xml" --include="MANIFEST.MF" . \
        | grep -v "target/" | grep -v ".git/" | grep -v "bin/"


## 3. Commit, tag, push

    git add -A
    git commit -m "Release version X.Y.Z"
    git tag vX.Y.Z
    git push && git push --tags

Pushing the `vX.Y.Z` tag triggers `.github/workflows/docker.yml`, which:

- Builds the Tycho reactor (skip tests)
- Builds the webdemo fat JAR (Vitest still runs; Spring Boot tests skipped)
- Pushes `mkbauer/tinyscript:X.Y.Z` and `:latest` to Docker Hub

Monitor progress at: https://github.com/mabauer/tinyscript/actions


## 4. Bump to next SNAPSHOT

    mvn org.eclipse.tycho:tycho-versions-plugin:set-version \
        -DnewVersion=X.Y.(Z+1)-SNAPSHOT
    mvn versions:set -DnewVersion=X.Y.(Z+1)-SNAPSHOT -DgenerateBackupPoms=false \
        -f de.mkbauer.tinyscript.repl/pom.xml
    mvn versions:set -DnewVersion=X.Y.(Z+1)-SNAPSHOT -DgenerateBackupPoms=false \
        -f de.mkbauer.tinyscript.webdemo/pom.xml
    mvn versions:set -DnewVersion=X.Y.(Z+1)-SNAPSHOT -DgenerateBackupPoms=false \
        -f de.mkbauer.tinyscript.standalone.tests/pom.xml

Then manually update the `de.mkbauer.tinyscript` dependency version in
`repl/pom.xml` and `webdemo/pom.xml` to `X.Y.(Z+1)-SNAPSHOT`.

    git add -A
    git commit -m "Bump to X.Y.(Z+1)-SNAPSHOT"
    git push


## Notes

- **GitHub secrets required:** the Docker workflow reads `DOCKERHUB_USERNAME`
  and `DOCKERHUB_TOKEN` from repository secrets (Settings → Secrets and
  variables → Actions → Repository secrets).
- **CI workflow** (`.github/workflows/ci.yml`) runs the full test suite on every
  push to master and on pull requests — no action needed.
- **Docker base image:** `eclipse-temurin:21-jre` (`openjdk` images are deprecated).
- See `docs/build.md` for a full description of the build system.
