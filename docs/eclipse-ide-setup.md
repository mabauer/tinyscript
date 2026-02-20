# Eclipse IDE Setup

These instructions apply to the `eclipse_2025_12` branch and later, which targets Xtext 2.41.0, Eclipse 2025-12, and Java 21.

## 1. Install/update the host Eclipse IDE

You need **Eclipse IDE for RCP and RAP Developers** (or Plug-in Developers) from **Eclipse 2025-12** or later. Download from eclipse.org/downloads, or update an existing Eclipse via Help → Check for Updates.

## 2. Install Xtext SDK 2.41.0 into the host Eclipse

The host IDE needs Xtext tooling (editor, Xtend compiler, MWE2 runner) — this is separate from the target platform.

- Help → Install New Software
- Add site: `https://download.eclipse.org/modeling/tmf/xtext/updates/releases/2.41.0/`
- Install: **Xtext SDK** (includes Xtend IDE and MWE2)

## 3. Add Java 21 to Eclipse

- Window → Preferences → Java → Installed JREs
- Add a Java 21 JDK (e.g. OpenJDK 21 from Homebrew at `/opt/homebrew/opt/openjdk@21` on macOS)
- Make it the default, or at minimum ensure it is available

## 4. Open the workspace and reload the target platform

- Open the `tinyscript` workspace in Eclipse
- Window → Preferences → Plug-in Development → Target Platform
- Select `de.mkbauer.tinyscript.target` and click **Reload**
- Eclipse will download bundles from the updated P2 sites (Eclipse 2025-12, Xtext 2.41.0, Orbit 2025-12) — this takes a few minutes
- Click Apply and Close

## 5. Regenerate Xtext sources

The `src-gen/` folders must be regenerated with the installed Xtext version:

- In the Project Explorer, right-click `de.mkbauer.tinyscript/src/de/mkbauer/tinyscript/GenerateTinyscript.mwe2`
- Run As → MWE2 Workflow
- Wait for it to complete (regenerates `src-gen/` across core, ide, ui, and tests)

## 6. Clean and rebuild

- Project → Clean → Clean all projects
- Check the Problems view — expect no errors

## Running tests

Launch the JUnit tests from the `de.mkbauer.tinyscript.tests` project as you would any Eclipse plug-in test. The 12 tests in `JvmScriptEngineRunnerTest` will appear as **skipped** (not failed) on standard OpenJDK 21, which has no built-in JavaScript engine (Nashorn was removed in Java 15; Graal.js is only available in GraalVM distributions). All other tests should pass.
