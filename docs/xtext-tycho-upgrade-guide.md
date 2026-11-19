# Xtext / Tycho / Eclipse Upgrade Guide

A concise checklist for upgrading the toolchain. Each section describes what to change; the **Gotchas** section captures everything that went wrong during the 2023-09 → 2025-12 migration so you don't have to rediscover it.

## Current versions

| Component | Version |
|---|---|
| Xtext | 2.44.0 |
| MWE2 | 2.27.0 |
| Tycho | 5.0.4 |
| Eclipse platform | 2026-09 |
| Orbit bundles | 2026-09 |
| Maven (wrapper) | 3.9.16 |
| BREE (all bundles) | JavaSE-25 |

---

## Prerequisites

- **Java 25** as the Maven runtime (all bundles compile for JavaSE-25; Tycho 5 itself needs at least 21). Set `JAVA_HOME` if needed:
  ```bash
  export JAVA_HOME=$(/usr/libexec/java_home -v 25)
  java -version && ./mvnw --version
  ```
- **Maven 3.9.9+**
- Create a working branch before touching anything.

---

## Files to update

### `pom.xml` (root)

```xml
<xtext.version>NEW</xtext.version>
<mwe2Version>NEW</mwe2Version>
<tycho-version>NEW</tycho-version>
```

Also ensure these are present (required by Tycho 5):
```xml
<!-- in target-platform-configuration -->
<executionEnvironment>JavaSE-25</executionEnvironment>

<!-- in tycho-surefire-plugin argLine — must use defined properties only -->
<argLine>${platformSystemProperties} ${systemProperties} ${moduleProperties} ${tycho.testArgLine}</argLine>
```

### `de.mkbauer.tinyscript.target/*.target`

Update the four P2 repository URLs:

| Location | URL pattern |
|---|---|
| Eclipse releases | `https://download.eclipse.org/releases/YYYY-MM` |
| MWE2 | `https://download.eclipse.org/modeling/emft/mwe/updates/releases/X.Y.Z/` |
| Xtext | `https://download.eclipse.org/modeling/tmf/xtext/updates/releases/X.Y.Z/` |
| Orbit | `https://download.eclipse.org/tools/orbit/simrel/orbit-aggregation/YYYY-MM` |

Keep all `<unit>` versions at `0.0.0` to pick up whatever the new Orbit provides.

### `de.mkbauer.tinyscript.tests/pom.xml`

No version changes needed, but ensure the explicit surefire execution is present (Tycho 5 requires it for `eclipse-test-plugin`):
```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-surefire-plugin</artifactId>
    <executions>
        <execution>
            <id>default-test</id>
            <goals><goal>test</goal></goals>
        </execution>
    </executions>
    <configuration>
        <useUIHarness>false</useUIHarness>
        <useUIThread>false</useUIThread>
    </configuration>
</plugin>
```

### `de.mkbauer.tinyscript.repl/pom.xml`

Update `<xtext-version>` (this module is outside the root reactor and has its own local property).

### `de.mkbauer.tinyscript.webdemo/pom.xml`

Update `<xtext-version>`.

### All `META-INF/MANIFEST.MF` files

- `Bundle-RequiredExecutionEnvironment` — set to the minimum Java version required by the Eclipse platform release (was JavaSE-21 for Eclipse 2025-12 due to Lucene 10; now JavaSE-25, the current Java LTS).
- Check `Require-Bundle` against what the new Xtext/Orbit actually ships — bundle IDs and version ranges can change between releases (see Gotchas).

---

## Build and verify

```bash
# 1. Install target first (other modules resolve against the local repo copy)
./mvnw clean install -pl de.mkbauer.tinyscript.target

# 2. Full build, skip tests
./mvnw clean install -DskipTests

# 3. Run tests (eclipse-test-plugin runs in integration-test phase — use verify, not test)
./mvnw clean verify

# 4. Smoke-test REPL
./mvnw clean package -pl de.mkbauer.tinyscript.repl
echo 'print(1 + 1);' | java -jar de.mkbauer.tinyscript.repl/target/tinyscript-repl.jar

# 5. Smoke-test webdemo
./mvnw clean package -pl de.mkbauer.tinyscript.webdemo
java -jar de.mkbauer.tinyscript.webdemo/target/tinyscript-webdemo-*.jar &
curl -s -X POST http://localhost:8080/execute -H 'Content-Type: text/plain' --data 'print(1 + 1);'
kill %1
```

Expected test result: all interpreter tests pass; `JvmScriptEngineRunnerTest` shows 12 **skipped** (not failed) on standard OpenJDK without Graal.js/Nashorn.

---

## Eclipse IDE after upgrading

1. Install Eclipse IDE matching the new release train (e.g. 2025-12 for RCP/RAP Developers).
2. Install Xtext SDK into the host Eclipse (Help → Install New Software, use the Xtext release P2 URL).
3. Add Java 25 JDK: Window → Preferences → Java → Installed JREs → Add. Then map it: Java → Installed JREs → **Execution Environments** → select **JavaSE-25** → tick the Java 25 JDK. Without this mapping Eclipse reports "release 25 is not found in the system".
4. In Window → Preferences → Plug-in Development → Target Platform, reload `de.mkbauer.tinyscript.target`.
5. Add the ANTLR generator JAR to the build path (needed for the MWE2 workflow; Maven fetches it automatically but Eclipse does not):
   - Download `antlr-generator-3.2.0-patch.jar` from `https://download.itemis.com/antlr-generator-3.2.0-patch.jar`
   - Right-click `de.mkbauer.tinyscript` → Properties → Java Build Path → Libraries → **Add JARs…** (or Add External JARs) → select the downloaded JAR
6. Right-click `GenerateTinyscript.mwe2` → Run As → MWE2 Workflow to regenerate `src-gen/`.
7. Project → Clean all projects. Verify Problems view is error-free.

---

## Notes from the 2025-12 → 2026-09 upgrade (Xtext 2.41 → 2.44)

A version bump only — no changes to MANIFEST.MF files, the MWE2 workflow or Java sources were needed.

- **JUnit 4 still works.** Xtext 2.43 dropped JUnit 5 in favour of JUnit 6, but `org.eclipse.xtext.testing` 2.44 still ships `XtextRunner` and requires `org.junit` 4.13.2. Its Jupiter imports are now `[6.0.0,7.0.0)`, which Orbit 2026-09 satisfies.
- **Xtext-Web/GWT removed in 2.44.** Only relevant if the MWE2 workflow contains `web = {…}` or `webSupport = {…}` — `GenerateTinyscript.mwe2` doesn't.
- **LSP4J 1.0.0** since Xtext 2.42; the project does not use LSP4J directly.
- Release notes: https://eclipse.dev/Xtext/releasenotes.html

## Notes from the Java 21 → 25 switch

- Tycho 5.0.4, Xtext 2.44, Xtend and MWE2 build and test fine with `JavaSE-25`; besides the version numbers (BREE in all `MANIFEST.MF` files, `<executionEnvironment>`, `java.version` / `maven.compiler.release` in the plain Maven modules, CI, Docker base image) no build changes were needed.
- Guice 7.0.0 (used by Xtext) calls `sun.misc.Unsafe::staticFieldBase` for its bytecode generation, which prints a "terminally deprecated method" warning on Java 24+. Xtext does not use Guice AOP, so the webdemo and REPL mains set `guice_bytecode_gen_option=DISABLED`, which avoids the call entirely. (`--sun-misc-unsafe-memory-access=allow` would only hide the warning.)
- The sandbox limits in `ResourceMonitor` (statements, call depth, memory via `com.sun.management.ThreadMXBean`) behave identically on 21 and 25.
- Eclipse needs the **JavaSE-25** execution environment mapped to a Java 25 JDK (see below), otherwise every plug-in project fails to compile.

## Gotchas from the 2023-09 → 2025-12 migration

### Java 21 is required for the BREE too, not just for Maven
Eclipse 2025-12 ships Lucene 10, which declares `Bundle-RequiredExecutionEnvironment: JavaSE-21`. Tycho's target-platform resolution respects BREE and will fail to resolve the Eclipse platform unless `<executionEnvironment>JavaSE-21</executionEnvironment>` is set in the POM **and** all project `MANIFEST.MF` files are updated to `JavaSE-21`. Setting only one of these is not enough.

### Orbit 2025-12 bundle changes
- `javax.inject` — **removed** (superseded by `jakarta.inject.jakarta.inject-api`)
- `io.github.classgraph` — **renamed** to `io.github.classgraph.classgraph`
- `org.apache.commons.logging` — still present, but must be listed **explicitly** in the target (it is a transitive requirement of Xtext SDK bundles but is not pulled in automatically by the planner from the Eclipse releases location)

### Xtext 2.41.0 bundle changes
- `org.eclipse.xtext.generator` — **merged** into `org.eclipse.xtext`; remove from `Require-Bundle`
- `org.eclipse.xtext.junit4` — **removed**; use `org.eclipse.xtext.testing` (which was already the correct import)

### Tycho picks the JUnit 4 provider for Jupiter tests
`org.eclipse.xtext.testing` requires the `org.junit` 4 bundle even when the tests use Jupiter. Tycho's provider auto-detection therefore selects `junit4`, finds no tests, and reports BUILD SUCCESS with "No tests found". The tests module pins `<providerHint>junit6</providerHint>` and sets `<failIfNoTests>true</failIfNoTests>`. Check the log for `Selected test framework junit (6.0.0) with provider junit6` after any Tycho or Xtext upgrade.

### Tycho 5 test phase change
`eclipse-test-plugin` tests now run in the `integration-test` lifecycle phase, not `test`. Use `./mvnw clean verify` to run them. Using `./mvnw clean test` silently skips the tests. An explicit `<execution><id>default-test</id>...</execution>` must be present in the tests module `pom.xml` for Tycho to bind the goal.

### `${additionalTestArguments}` undefined in Tycho 5
If the surefire `<argLine>` references an undefined Maven property (like the old `${additionalTestArguments}`), Tycho 5 passes the literal string to the JVM, which immediately exits with code 1. Use only properties that are defined (e.g. `${tycho.testArgLine}`, `${platformSystemProperties}`).

### Target module must be installed before other modules
When debugging target resolution issues, run `./mvnw install -pl de.mkbauer.tinyscript.target` first. Other modules resolve the target platform against the version in the local Maven repository, not the workspace file.

### MWE2 bundle-version is a minimum constraint
`org.eclipse.emf.mwe2.launch;bundle-version="2.12.1"` means "at least 2.12.1". This is satisfied by MWE2 2.24.x — do not change it to 2.24.0, as that would require exactly that version or higher and may break older setups unnecessarily.

### No JavaScript engine on standard OpenJDK
Nashorn was removed from the JDK in Java 15. Graal.js is only available in GraalVM distributions. `JvmScriptEngineRunnerTest` uses `Assume.assumeNotNull()` to skip gracefully. This is expected and correct — the 12 skipped tests are not failures.

### Eclipse IDE: the JavaSE-NN execution environment must be explicitly mapped
Adding the JDK (currently Java 25) to Installed JREs is not enough. Eclipse also requires the **Execution Environments** mapping (Window → Preferences → Java → Installed JREs → Execution Environments → JavaSE-25 → tick the JDK) to be set, otherwise projects with `Bundle-RequiredExecutionEnvironment: JavaSE-25` fail to compile with "release 25 is not found in the system".

### Eclipse IDE: ANTLR generator JAR is not available as an OSGi bundle
The `XtextGenerator` MWE2 component requires `antlr-generator-3.2.0-patch.jar` on the classpath. In Maven this is provided as a dependency of `exec-maven-plugin`. In Eclipse, it is not part of any OSGi bundle in the target platform, so it must be added manually to the Java Build Path of `de.mkbauer.tinyscript` (project Properties → Java Build Path → Add JARs). Download from `https://download.itemis.com/antlr-generator-3.2.0-patch.jar`. Without it the MWE2 workflow fails with: `It is required to use ANTLR's parser generator`.
