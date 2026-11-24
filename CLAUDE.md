# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with
code in this repository.

## Project Overview

Tinyscript is a proof-of-concept JavaScript-like scripting language built on
Eclipse Xtext. It supports dynamic typing, prototypal inheritance, closures, and
sandboxed execution. The project targets multiple deployment targets: Eclipse
IDE plugin, standalone REPL, and Spring Boot web app; it can be embedded in Java
applications via `TinyscriptEngine`.

## Build Commands

This is a Maven multi-module project using Eclipse Tycho for OSGi/Eclipse plugin
builds.

Always build with the Maven wrapper (`./mvnw`), which pins Maven 3.9.16. Maven
3.10+ fails on the core module with `Invalid Collect Request: null` because its
resolver rejects the `system`-scoped p2 dependencies that Tycho 5.0.x injects
(verified with Tycho 5.0.2; not re-tested with 5.0.4). `JAVA_HOME` must point to
a JDK 25.

```bash
# Build everything (runs from repo root)
./mvnw clean install

# Build only the REPL fat JAR
./mvnw clean package -f de.mkbauer.tinyscript.repl/pom.xml

# Build only the web demo fat JAR
./mvnw clean package -f de.mkbauer.tinyscript.webdemo/pom.xml

# Skip tests during build
./mvnw clean install -DskipTests
```

## Running Tests

### After any code change — always run the standalone tests first

After changing anything in the interpreter (`TinyscriptEngine`) or the generator
(`TinyscriptGenerator`), always run:

```bash
./mvnw test -pl de.mkbauer.tinyscript.standalone.tests
```

This is the fastest feedback loop (< 5 s, no OSGi startup) and catches both
interpreter regressions and transpiler/interpreter divergence. If node.js is not
on PATH the differential tests skip cleanly (in CI, where `CI` is set, they fail
instead); the interpreter reference tests always run.

### Test modules

**`de.mkbauer.tinyscript.standalone.tests`** — plain Maven/JUnit 6 (Jupiter)
module, no OSGi:

Tests run in this order (alphabetical class order enforced by Surefire):

1. `InterpreterReferenceTest` — 29
   `assertEquals(expected, interpreterOutput(tsCode))` tests; one per language
   feature; failure shows the exact wrong value produced by the interpreter.
2. `TranspilerTest` — differential tests; `assertSameOutput(tsCode)` runs each
   snippet through both the interpreter and `node`, diffs stdout; generated JS
   is shown on failure so it can be pasted into `node` directly.
3. `TranspilerVerifyTest` — integration tests;
   `assertTranspilerCorrectFromFile(filename)` runs complete `.tys` programs
   through both interpreter (must not throw) and node (must exit 0).

Whole programs: `AdventOfCode2024Test` (examples/aoc2024),
`AdventOfCode2023Test` (ten days in examples/aoc2023), `AdventOfCode2022Test`
(ten days in examples/aoc2022), `AdventOfCode2021Test` (examples/aoc2021, ported
from TypeScript), `OoExamplesTest` (examples/oo: classes before `class`, and
programs written with `class` next to the same with prototypes) and
`DemoExamplesTest` (every example of the web demo, from
`de.mkbauer.tinyscript.webdemo/frontend/public`) compare the interpreter with
node. The webdemo's `ExamplesTests` run every example as the demo does
(sandboxed) and check that `Toolbar.vue` lists exactly the example files; a new
example needs no new test.

Base class `TinyscriptTranspilerTestBase` provides:

- `interpreterOutput(String tsCode)` — runs snippet through a fresh
  `TinyscriptEngine`, returns captured stdout; use with JUnit's `assertEquals`
  for targeted interpreter tests
- `assertSameOutput(String tsCode)` — Level 1 differential assertion
- `assertTranspilerCorrect(String)` / `assertTranspilerCorrectFromFile(String)`
  — Level 2 integration assertions
- `assertInterpreterThrows(Class, String)` — expected-exception helper, only for
  what node cannot do alike (the sandbox); errors otherwise use:
- `assertSameFailure([Class,] String)` — both backends fail at the same point
  (same output up to the error) and in the same category for `catch`; class and
  message differences are only written to `target/backend-differences.txt`
- `assertKnownGap([Class,] String)` — the interpreter fails, node goes on (a
  known gap of the generated code); fails once node fails too, so the list stays
  true

```bash
# Run standalone tests only (fast, use after interpreter or generator changes)
./mvnw test -pl de.mkbauer.tinyscript.standalone.tests

# Run a single test class
./mvnw test -pl de.mkbauer.tinyscript.standalone.tests -Dtest=InterpreterReferenceTest

# Run a single test method
./mvnw test -pl de.mkbauer.tinyscript.standalone.tests -Dtest=InterpreterReferenceTest#testRecursiveFibonacci
```

**`de.mkbauer.tinyscript.tests`** — OSGi/Eclipse plugin tests via
`tycho-surefire-plugin`:

Tests the interpreter and language features via JUnit 6 (Jupiter) with Xtext's
`InjectionExtension` / `@InjectWith`. Slower (requires OSGi container startup).
Run after confirming the standalone tests pass.

`tycho-surefire-plugin` runs in the `integration-test` phase, so these tests
need `verify` (or `install`) — `./mvnw test` builds the module but runs no
tests.

```bash
# Run all OSGi tests
./mvnw verify -pl de.mkbauer.tinyscript.tests

# Run a specific test class
./mvnw verify -pl de.mkbauer.tinyscript.tests -Dtest=ExpressionsTest

# Run a specific test method
./mvnw verify -pl de.mkbauer.tinyscript.tests -Dtest=ExpressionsTest#testIntAddition
```

Test helper base class `TinyscriptInterpreterTestHelper` provides:

- `evaluateSimpleExpression(String expr)` — evaluates a single expression,
  returns `TSValue`
- `executeScriptFromString(String script)` — executes inline script
- `executeScriptFromFile(String filename)` — loads `.tys` file from `resources/`
- `engine` — direct access to `TinyscriptEngine`

### Full test run

```bash
./mvnw clean verify
```

## Architecture

### Module Layout

| Module                                   | Purpose                                                                     |
| ---------------------------------------- | --------------------------------------------------------------------------- |
| `de.mkbauer.tinyscript`                  | Core: grammar, interpreter, runtime, validation, scoping                    |
| `de.mkbauer.tinyscript.tests`            | OSGi/Eclipse plugin tests (JUnit 6, `InjectionExtension`)                   |
| `de.mkbauer.tinyscript.standalone.tests` | Standalone (no-OSGi) tests: interpreter reference + transpiler differential |
| `de.mkbauer.tinyscript.ide`              | Language server / IDE support (generated by Xtext)                          |
| `de.mkbauer.tinyscript.ui`               | Eclipse editor UI plugin                                                    |
| `de.mkbauer.tinyscript.repl`             | Standalone REPL app                                                         |
| `de.mkbauer.tinyscript.webdemo`          | Spring Boot web sandbox demo                                                |
| `de.mkbauer.tinyscript.target`           | Eclipse target platform definition                                          |
| `de.mkbauer.tinyscript.vscode`           | VS Code extension: syntax highlighting (npm, not Maven; `npm test`)         |

### Execution Pipeline

```
Source text
  → Xtext parser (generated from Tinyscript.xtext)
  → AST (EMF model objects)
  → TinyscriptValidator (semantic checks)
  → TinyscriptEngine (tree-walking interpreter)
  → TSValue result
```

### Key Classes in `de.mkbauer.tinyscript`

**Interpreter:**

- `interpreter/TinyscriptEngine.java` — main tree-walking evaluator; handles all
  AST node types via visitor-style `switch`/`instanceof` dispatch
- `interpreter/ExecutionContext.java` — per-call-frame variable scope; forms a
  chain for closure lookup
- `interpreter/GlobalExecutionContext.java` — global scope; initializes built-in
  objects at startup
- `interpreter/TSValue.java` — sealed interface for all runtime values:
  `TSUndefined`, `TSNull`, records `TSBoolean` / `TSNumber` / `TSString`, and
  `TSObject` (objects, arrays, functions); create primitives with
  `TSValue.of(…)`
- `interpreter/TSObject.java` — property map + prototype reference; backing
  store for all objects and functions
- `interpreter/ResourceMonitor.java` — sandboxing: enforces limits on wall time,
  CPU time, statement count, call depth, memory, string length and output size

**Runtime built-ins** (`runtime/` subdirectories): `array/`, `string/`, `math/`,
`object/`, `function/`, `system/`; `fs/` and `io/` hold the native modules
`std:fs` (`FileSystemModule`) and `std:io` (`IoModule`, the standard input). The
other `std:` modules (`std:array`, `std:string`, `std:math`) are written in
tinyscript, in `de.mkbauer.tinyscript/src/std/`; errors inside them are reported
at the caller

**Scoping:** a script sees its own declarations and the global scope
(`TinyscriptGlobalScopeProvider`), which contains only resources marked with
`SharedGlobals`: the builtins and the lines of an interactive REPL session.
Files never see each other's top-level names.

**Grammar:** `Tinyscript.xtext` — Xtext grammar defining the full language
syntax. After editing, regenerate with `GenerateTinyscript.mwe2`.

### Language Differences from JavaScript

- Semicolons are required after statements; `if`/`else` bodies must be `{ … }`
  blocks (`else if (…) { … }` chains are allowed)
- Variables are block-scoped and declared with `let`, or `const` if they are
  never assigned again (style for examples, docs, `std:`: `const` where
  possible); `var` is a reserved word (syntax error with a hint)
- Loops: `for (const x : items)` (values, not keys; arrays, strings, `Set`s,
  `Map`s and lazy ranges: `for (const i : range(n))` counts 0 … n - 1,
  `range(n).reverse()` n - 1 … 0) and `while (cond) { … }`; there is no counting
  `for` (the Lua-style `for (let i = 0, n)` was removed in 0.23.0); `break`
  leaves the innermost loop, `continue` goes on with its next iteration. In
  sandboxed mode every loop iteration counts against the statement limit.
- Only `==` / `!=` (no type coercion, except `null == undefined`; `NaN != NaN`;
  there is no `===` / `!==`)
- No silent conversions: arithmetic needs numbers, `+` adds numbers, arrays, or
  a string and a value; builtins check their argument types and count
  (JavaScript's optional arguments are supported, more is a `TypeError`).
  Conditions accept booleans, `null` / `undefined` (false) and objects (true),
  not numbers or strings; `&&` / `||` give booleans, and a literal operand that
  can never be a condition (`options || {}`) is a compile-time error; `<`, `<=`,
  `>`, `>=` compare number with number or string with string, anything else is a
  `TypeError`
- `+` concatenates two arrays; with a string on either side it concatenates
  strings; an array plus anything else is a `TypeError`
- One text form: `x.toString()`, `String(x)`, `"" + x` and `print` agree
  (`{x: 1}`, `[1, 2]`, `Point {x: 1}`); the generator replaces
  `Object.prototype.toString` / `Array.prototype.toString` so node does too
- A comma after the last element only in array and object literals (`[1, 2,]`),
  not in calls, parameter lists or patterns; arrays have no empty elements
  (`[1, , 2]` is a syntax error)
- `++` / `--` only as statements (`i++;`); compound assignments `+=` … `%=` and
  `cond ? a : b` as in JavaScript
- Arrow functions use `=>` syntax
- Too many arguments are a compile-time error where the callee is known (a
  function declaration, a `const` with a function, `new` of a class of the same
  file); methods and callbacks ignore extra arguments. Function declarations
  cannot be assigned; in a class, `this.x = …` must not hide a method `x`
- Default parameters (`function f(a, step = 1)`) as in Javascript (used for a
  missing argument or `undefined`, evaluated at each call), but only at the end
  of the parameters and only for names, not patterns
- Spread in calls and array literals (`f(...items)`, `[...a, x]`) for what the
  `for` loop goes through; a rest element only at the end of an array pattern
  (`[first, ...rest]`); no spread or rest for objects
- `class` without inheritance (only at the top level; a `constructor` and
  methods, no `extends` / `static` / getters / field declarations); a class
  needs `new`, and its instances print with the class name (`Point {x: 1}`);
  object literals have the method shorthand `{ area() { … } }`.
  `interface Shape { area(); }` names methods; `class Square implements Shape`
  is checked once when the class is declared (by the validator if the interface
  is in the same file), only method names count
- Modules: `export` top-level functions / variables / classes;
  `import a, b from "./m";` or `import * as m from "./m";`. Each module runs
  once; imported names are read-only; files share names only via imports (see
  `docs/language-reference.md`, section 13)
- `try { … } catch (e) { … }` (no `finally`) catches only errors thrown with
  `throw` (which takes only `Error` objects) and failed `std:fs` / `std:io`
  input / output (`e.name == "IOError"`) and `JSON.parse` of invalid JSON
  (`"SyntaxError"`); `TypeError`s, failed `assert`s, stack overflows and
  resource limits are not caught
- `this` as in Javascript's strict mode: set by a method call, `undefined` in a
  plain call and outside functions (`this` there is a compile-time error)
- Builtins (`Math`, `print`, `Infinity`) cannot be assigned (compile-time
  error); read-only properties (`Math.PI`, `f.length`) are a `TypeError` when
  assigned, as in strict mode; `new` only for classes, functions written with
  `function` and builtin constructors, not for arrow functions, methods or other
  builtins
- The generated Javascript runs in strict mode, the script in a block of its own
  (its declarations hide the prelude's `Map`, `String` …); names that only
  Javascript reserves (`default`, `enum`, `yield` …) get the prefix `__ts_`; its
  prelude replaces the builtins by ones that check their arguments as the
  interpreter does

### TODO markers in the code

`TODO.md` is the single list of open work. A `TODO` / `FIXME` comment in the
code is only allowed with a reference to an entry there, e.g.
`// TODO(TODO.md: Number objects): …`. Otherwise fix it right away or delete the
comment.

### Dependency Injection

The core module uses Google Guice (wired by Xtext). Bindings are configured in
`TinyscriptRuntimeModule`. Tests use `@Inject` via
`@ExtendWith(InjectionExtension.class)` +
`@InjectWith(TinyscriptInjectorProvider.class)`.

### Markdown formatting

All Markdown files (except `COPYING.md`) are formatted with Prettier and wrap
prose at 80 columns (`.prettierrc.json`, `.prettierignore`). After editing a
`.md` file, run:

```bash
npx prettier@3.3.3 --write "**/*.md"
```

Prettier also writes `_emphasis_`, `-` list items and `#` headings; tables and
code blocks are not wrapped.

## Running the Applications

```bash
# REPL (after building fat JAR): interactive, or run files; see --help
de.mkbauer.tinyscript.repl/tinyscript [file.tys ...] [-- args ...]
# The same with node (experimental), or only the generated Javascript
de.mkbauer.tinyscript.repl/tinyscript --node file.tys [-- args ...]
de.mkbauer.tinyscript.repl/tinyscript --emit-js file.tys
java -jar de.mkbauer.tinyscript.repl/target/tinyscript-repl.jar [file.tys ...]

# Web demo (after building fat JAR)
java -jar de.mkbauer.tinyscript.webdemo/target/tinyscript-webdemo-*.jar

# Web demo via Docker
cd de.mkbauer.tinyscript.webdemo
docker build -t tinyscript-webdemo .
docker run -p 8080:8080 tinyscript-webdemo
```

The deployed webdemo is monitored by checkmk via `GET /test` (runs a fixed
sample script). Keep this endpoint;
`TinyscriptWebApplicationTests.testHealthCheckEndpoint` guards it.

## Releasing

See `Release-HOWTO.md` for the full procedure. Every non-trivial release gets
release notes (incompatible changes first, then new features and fixes),
published as a GitHub release for the tag. Key commands:

```bash
# 1. Bump version in OSGi modules (pom.xml + MANIFEST.MF + feature.xml)
./mvnw org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=X.Y.Z

# 2. Bump plain Maven modules
./mvnw versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false -f de.mkbauer.tinyscript.repl/pom.xml
./mvnw versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false -f de.mkbauer.tinyscript.webdemo/pom.xml
./mvnw versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false -f de.mkbauer.tinyscript.standalone.tests/pom.xml
# Also manually update the de.mkbauer.tinyscript dependency version in repl and webdemo pom.xml

# 3. Commit, tag, push — Docker workflow triggers automatically
git add -A && git commit -m "Release version X.Y.Z"
git tag vX.Y.Z && git push && git push --tags

# 4. Bump to next SNAPSHOT (repeat steps 1-2 with X.Y.(Z+1)-SNAPSHOT)
# Also manually update Bundle-Version in de.mkbauer.tinyscript.repl/META-INF/MANIFEST.MF
# to X.Y.(Z+1).qualifier
```

## CI/CD

Two GitHub Actions workflows in `.github/workflows/`:

- **`ci.yml`** — runs on every push/PR to `master`: full Tycho build + all
  tests + REPL + webdemo (Vitest + Spring Boot tests), then builds the Docker
  image (not pushed) and scans it with Trivy
- **`docker.yml`** — runs on `vX.Y.Z` tags: builds webdemo JAR and image, scans
  it with Trivy, and only then pushes `mkbauer/tinyscript:X.Y.Z` + `:latest` to
  Docker Hub

Both Trivy scans fail on fixable HIGH/CRITICAL vulnerabilities
(`ignore-unfixed: true`). `.github/dependabot.yml` covers Maven (plain modules
only, not the Tycho reactor), npm, the Docker base image and GitHub Actions.

Requires GitHub repository secrets `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN`.

## Technology Stack

- **Java 25** — all modules compile for Java 25 (BREE `JavaSE-25`); required for
  builds and IDE runs. The webdemo and REPL mains set
  `guice_bytecode_gen_option=DISABLED` so Guice 7 does not call
  `sun.misc.Unsafe` (deprecation warning on Java 24+)
- **Xtext 2.44.0** — language engineering framework
- **Maven + Tycho 5.0.4** — build system for Eclipse plugins
- **Spring Boot 4.1** — web demo backend (no Actuator; version info via
  `/api/version`)
- **Vue 3 + Vite + TypeScript** — web demo frontend
- **Bootstrap 5 + Bootstrap Icons** — web demo UI
- **CodeMirror 6** — web demo editor (with custom Tinyscript StreamLanguage)
- **Vitest + @vue/test-utils** — frontend unit tests
- **JUnit 6 (Jupiter)** — test framework for both OSGi and standalone tests
- **Apache Commons Exec 1.4** — used by `NodeJsRunner` to invoke `node` in
  standalone tests
- **eclipse-temurin:25-jre-noble** — Docker base image (Ubuntu 24.04; the plain
  `25-jre` tag moved to Ubuntu 26.04, whose image ships `pebble` with a
  vulnerable Go runtime)

## Frontend Development (web demo)

The frontend lives in `de.mkbauer.tinyscript.webdemo/frontend/`. The Maven build
runs `npm install`, `npm test`, and `npm build` automatically via
`frontend-maven-plugin` during the `generate-resources` phase, so a normal
`./mvnw package` requires no extra steps.

For iterative frontend work, run the Vite toolchain directly:

```bash
cd de.mkbauer.tinyscript.webdemo/frontend

npm install        # first time only
npm run test       # Vitest unit tests (Output, Toolbar, tinyscript-lang)
npm run build      # rebuild → ../src/main/resources/public/
```

After `npm run build`, trigger an Eclipse project refresh (**F5**) so the
updated files are copied from `src/main/resources/public/` to
`target/classes/public/`, then refresh the browser. Alternatively:

```bash
npm run build && ./mvnw resources:resources -f de.mkbauer.tinyscript.webdemo/pom.xml
```

**Frontend source layout:**

```
frontend/
├── index.html
├── vite.config.ts
├── tsconfig.json
├── package.json
├── public/           ← 13 example .tys scripts (served as static files)
└── src/
    ├── main.ts
    ├── style.css
    ├── types.ts      ← ExecutionResult, Statistics interfaces
    ├── App.vue
    └── components/
        ├── Toolbar.vue
        ├── Editor.vue
        ├── tinyscript-lang.ts   ← CM6 StreamLanguage tokenizer
        ├── Output.vue
        └── __tests__/           ← Vitest specs
```

The Vite build outputs to `src/main/resources/public/` (Spring Boot's static
resource directory). The built assets are committed to the repository so the app
can be started directly from the JAR without running npm.
