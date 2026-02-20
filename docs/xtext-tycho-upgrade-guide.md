# Xtext / Tycho / Eclipse Upgrade Guide

A concise checklist for upgrading the toolchain. Each section describes what to change; the **Gotchas** section captures everything that went wrong during the 2023-09 → 2025-12 migration so you don't have to rediscover it.

## Current versions (as of eclipse_2025_12 branch)

| Component | Version |
|---|---|
| Xtext | 2.41.0 |
| MWE2 | 2.24.0 |
| Tycho | 5.0.2 |
| Eclipse platform | 2025-12 |
| Orbit bundles | 2025-12 |
| BREE (all bundles) | JavaSE-21 |

---

## Prerequisites

- **Java 21** as the Maven runtime (Tycho 5+ hard requirement). Set `JAVA_HOME` if needed:
  ```bash
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21
  java -version && mvn --version
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
<executionEnvironment>JavaSE-21</executionEnvironment>

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

- `Bundle-RequiredExecutionEnvironment` — set to the minimum Java version required by the Eclipse platform release (was JavaSE-21 for Eclipse 2025-12 due to Lucene 10).
- Check `Require-Bundle` against what the new Xtext/Orbit actually ships — bundle IDs and version ranges can change between releases (see Gotchas).

---

## Build and verify

```bash
# 1. Install target first (other modules resolve against the local repo copy)
mvn clean install -pl de.mkbauer.tinyscript.target

# 2. Full build, skip tests
mvn clean install -DskipTests

# 3. Run tests (eclipse-test-plugin runs in integration-test phase — use verify, not test)
mvn clean verify

# 4. Smoke-test REPL
mvn clean package -pl de.mkbauer.tinyscript.repl
echo 'print(1 + 1);' | java -jar de.mkbauer.tinyscript.repl/target/tinyscript-repl-*-shade.jar

# 5. Smoke-test webdemo
mvn clean package -pl de.mkbauer.tinyscript.webdemo
java -jar de.mkbauer.tinyscript.webdemo/target/tinyscript-webdemo-*.jar &
curl -s -X POST http://localhost:8080/execute -H 'Content-Type: text/plain' --data 'print(1 + 1);'
kill %1
```

Expected test result: all interpreter tests pass; `JvmScriptEngineRunnerTest` shows 12 **skipped** (not failed) on standard OpenJDK without Graal.js/Nashorn.

---

## Eclipse IDE after upgrading

1. Install Eclipse IDE matching the new release train (e.g. 2025-12 for RCP/RAP Developers).
2. Install Xtext SDK into the host Eclipse (Help → Install New Software, use the Xtext release P2 URL).
3. Add Java 21 JDK: Window → Preferences → Java → Installed JREs → Add. Then map it: Java → Installed JREs → **Execution Environments** → select **JavaSE-21** → tick the Java 21 JDK. Without this mapping Eclipse reports "release 21 is not found in the system".
4. In Window → Preferences → Plug-in Development → Target Platform, reload `de.mkbauer.tinyscript.target`.
5. Add the ANTLR generator JAR to the build path (needed for the MWE2 workflow; Maven fetches it automatically but Eclipse does not):
   - Download `antlr-generator-3.2.0-patch.jar` from `https://download.itemis.com/antlr-generator-3.2.0-patch.jar`
   - Right-click `de.mkbauer.tinyscript` → Properties → Java Build Path → Libraries → **Add JARs…** (or Add External JARs) → select the downloaded JAR
6. Right-click `GenerateTinyscript.mwe2` → Run As → MWE2 Workflow to regenerate `src-gen/`.
7. Project → Clean all projects. Verify Problems view is error-free.

---

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

### Tycho 5 test phase change
`eclipse-test-plugin` tests now run in the `integration-test` lifecycle phase, not `test`. Use `mvn clean verify` to run them. Using `mvn clean test` silently skips the tests. An explicit `<execution><id>default-test</id>...</execution>` must be present in the tests module `pom.xml` for Tycho to bind the goal.

### `${additionalTestArguments}` undefined in Tycho 5
If the surefire `<argLine>` references an undefined Maven property (like the old `${additionalTestArguments}`), Tycho 5 passes the literal string to the JVM, which immediately exits with code 1. Use only properties that are defined (e.g. `${tycho.testArgLine}`, `${platformSystemProperties}`).

### Target module must be installed before other modules
When debugging target resolution issues, run `mvn install -pl de.mkbauer.tinyscript.target` first. Other modules resolve the target platform against the version in the local Maven repository, not the workspace file.

### MWE2 bundle-version is a minimum constraint
`org.eclipse.emf.mwe2.launch;bundle-version="2.12.1"` means "at least 2.12.1". This is satisfied by MWE2 2.24.x — do not change it to 2.24.0, as that would require exactly that version or higher and may break older setups unnecessarily.

### No JavaScript engine on standard OpenJDK 21
Nashorn was removed from the JDK in Java 15. Graal.js is only available in GraalVM distributions. `JvmScriptEngineRunnerTest` uses `Assume.assumeNotNull()` to skip gracefully. This is expected and correct — the 12 skipped tests are not failures.

### Eclipse IDE: JavaSE-21 execution environment must be explicitly mapped
Adding Java 21 to Installed JREs is not enough. Eclipse also requires the **Execution Environments** mapping (Window → Preferences → Java → Installed JREs → Execution Environments → JavaSE-21 → tick the JDK) to be set, otherwise projects with `Bundle-RequiredExecutionEnvironment: JavaSE-21` fail to compile with "release 21 is not found in the system".

### Eclipse IDE: ANTLR generator JAR is not available as an OSGi bundle
The `XtextGenerator` MWE2 component requires `antlr-generator-3.2.0-patch.jar` on the classpath. In Maven this is provided as a dependency of `exec-maven-plugin`. In Eclipse, it is not part of any OSGi bundle in the target platform, so it must be added manually to the Java Build Path of `de.mkbauer.tinyscript` (project Properties → Java Build Path → Add JARs). Download from `https://download.itemis.com/antlr-generator-3.2.0-patch.jar`. Without it the MWE2 workflow fails with: `It is required to use ANTLR's parser generator`.
