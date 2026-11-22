# Eclipse IDE Setup

These instructions target Xtext 2.44.0, Eclipse 2026-09, and Java 25.

## 1. Install/update the host Eclipse IDE

You need **Eclipse IDE for RCP and RAP Developers** (or Plug-in Developers) from
**Eclipse 2026-09** or later. Download from eclipse.org/downloads, or update an
existing Eclipse via Help → Check for Updates.

## 2. Install Xtext SDK 2.44.0 into the host Eclipse

The host IDE needs Xtext tooling (editor, Xtend compiler, MWE2 runner) — this is
separate from the target platform.

- Help → Install New Software
- Add site:
  `https://download.eclipse.org/modeling/tmf/xtext/updates/releases/2.44.0/`
- Install: **Xtext SDK** (includes Xtend IDE and MWE2)

## 3. Add Java 25 to Eclipse and map the execution environment

**Add the JDK:**

- Window → Preferences → Java → Installed JREs → **Add** → Standard VM
- Directory: `/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home`
  (macOS, `brew install --cask temurin@25`) or wherever your Java 25 JDK lives
- Finish, then tick it as the default

**Map it to the JavaSE-25 execution environment** (required — without this step
Eclipse cannot compile the plugins and reports "release 25 is not found in the
system"):

- Window → Preferences → Java → Installed JREs → **Execution Environments**
- Select **JavaSE-25** in the left list
- Tick the Java 25 JDK in the right list
- Apply and Close

## 4. Open the workspace and reload the target platform

- Open the `tinyscript` workspace in Eclipse
- Window → Preferences → Plug-in Development → Target Platform
- Select `de.mkbauer.tinyscript.target` and click **Reload**
- Eclipse will download bundles from the updated P2 sites (Eclipse 2026-09,
  Xtext 2.44.0, Orbit 2026-09) — this takes a few minutes
- Click Apply and Close

## 5. Regenerate Xtext sources

The `src-gen/` folders must be regenerated with the installed Xtext version:

- In the Project Explorer, right-click
  `de.mkbauer.tinyscript/src/de/mkbauer/tinyscript/GenerateTinyscript.mwe2`
- Run As → MWE2 Workflow
- Wait for it to complete (regenerates `src-gen/` across core, ide, ui, and
  tests)

The MWE2 workflow requires an ANTLR generator JAR that is not part of any OSGi
bundle. The Maven build fetches it automatically via `exec-maven-plugin`
dependencies, but Eclipse needs it added to the build path manually. This is a
one-time step per workspace import:

1. Download `antlr-generator-3.2.0-patch.jar` from
   `https://download.itemis.com/antlr-generator-3.2.0-patch.jar`
2. Place the JAR anywhere convenient (e.g. inside the `de.mkbauer.tinyscript/`
   project folder)
3. Right-click `de.mkbauer.tinyscript` → **Properties → Java Build Path →
   Libraries**
4. **Add JARs…** (or **Add External JARs…** if you placed it outside the
   workspace) → select the JAR → Apply and Close

## 6. Clean and rebuild

- Project → Clean → Clean all projects
- Check the Problems view — expect no errors

## After a version bump (SNAPSHOT → release → next SNAPSHOT)

Whenever the Maven version changes (e.g. after a release cycle bumps master to
`0.11.2-SNAPSHOT`), the new SNAPSHOT artifacts are not yet in your local `~/.m2`
repository. Eclipse m2e will show a red error on the `webdemo` and `repl`
projects:

> Could not find artifact
> de.mkbauer.tinyscript:de.mkbauer.tinyscript:jar:X.Y.Z-SNAPSHOT

**Fix — run the tier 1 install from the terminal** (not from within Eclipse):

```bash
./mvnw clean install -DskipTests
```

This builds and installs all OSGi/Tycho artifacts into `~/.m2` at the new
version. Then back in Eclipse:

- Select all projects (Ctrl+A in Package Explorer)
- **Alt+F5** → Maven → Update Project
- Tick **"Force Update of Snapshots/Releases"**
- Click OK

The errors will clear. The "Force Update" checkbox is required — without it m2e
reuses its cached failed-resolution result for SNAPSHOTs.

## Running tests

Launch the JUnit tests from the `de.mkbauer.tinyscript.tests` project as you
would any Eclipse plug-in test. All tests should pass.
