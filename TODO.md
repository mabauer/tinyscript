TODO
====

Open issues and ideas for tinyscript. Each item is meant to become its own PR, with tests first.


Next
----

Nothing scheduled right now; pick the next items from the sections below.


Language
--------

- **Modules.** Keep it simple: one file is one module, an import always binds the whole module to one name; no named imports, no re-exports, no cycles.

  ```js
  // geometry.ts
  export function area(r) { return Math.PI * r * r; }
  export var unit = "cm";
  var helper = 2;              // not exported, private

  // main.ts
  import geo from "geometry.ts";
  print(geo.area(2), geo.unit);
  ```

  - Semantics: a module runs once on first import, in its own context that only sees the builtins. The result is a frozen object of its exports, cached per file. Cyclic imports are an error.
  - No cross-file linking in Xtext needed: `geo` is a local name, `geo.area` a normal property access at runtime. But the global scope (`TinyscriptGlobalScopeProvider`) must be restricted to the builtins; today every file sees the top-level names of all other files in the resource set.
  - Loading via a `ModuleLoader` interface passed to the engine: file system loader for the REPL (relative to the importing file, no escaping via `..`), standard library from the classpath only for the webdemo. Resource limits apply across all modules.
  - Grammar: `ImportStatement: 'import' id=Identifier 'from' path=STRING ';'` and an optional `export` before `function` / `var`. Validator: imports only at the top, exports only at top level, no duplicate export names.
  - Generator: bundle into one JS script, each module as `var geo = (function () { …; return { area: area, unit: unit }; })();`, so the differential tests keep using a single `node` run.
  - Suggested PRs: grammar + interpreter, then generator, then optionally a standard library with `fs` as a module (`import fs from "std:fs";`), which the sandbox simply does not provide.

- **Conversions.** Builtins convert their arguments with `TSObject.toNumber` / `toInteger` / `toString`. Today `undefined` and non-numeric strings become `0` (Javascript: `NaN`), a user-defined `valueOf` is ignored, and some code uses `TSValue.asString()` where `TSObject.toString(engine, value)` (which calls a user-defined `toString`) would be right. Decide how much of Javascript's conversion rules tinyscript wants, then write tests.

- **Global `this`.** At top level `this` is the global object. In Javascript's strict mode (which tinyscript otherwise follows) it is `undefined`.

- **More functions in the runtime library.** For example `Array.prototype.indexOf` and `concat` (the language reference listed them, but they never existed).


Interpreter and code quality
----------------------------

- **`JvmScriptEngineRunner` needs Graal.js**, so its 12 tests are always skipped. Either add the GraalJS dependency or remove it (decision deferred). Its error handling is minimal.


Build and operations
--------------------

- **Drop the CVE-2026-114938 workaround** (`-XX:-UseBASE64Intrinsics` in the webdemo `Dockerfile`) once the `eclipse-temurin:25-jre-noble` image ships 25.0.5.1.

- **Webdemo: CPU time limit** (`maxMXCpuTime`) as a second safety net besides the statement limit.

- **Re-test Maven 3.10+ with Tycho 5.0.4.** The wrapper pins Maven 3.9.16 because 3.10 failed with Tycho 5.0.2.

- **Language server support for Visual Studio Code** (idea from the original roadmap; the Xtext update that makes it possible is done).


Not planned for now
-------------------

- **Bytecode compiler.** Deliberately not pursued at the moment; the tree-walking interpreter stays.
