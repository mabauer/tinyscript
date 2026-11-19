TODO
====

Open issues and ideas for tinyscript. Each item is meant to become its own PR, with tests first.


Next
----

Nothing scheduled right now; pick the next items from the sections below.


Language
--------

- **`typeof` on arbitrary expressions.** Today `typeof` only accepts a name with optional `.property` suffixes (`TypeOfExpression` in the grammar). Plan:
  - Drop `TypeOfExpression` and make `typeof` a unary operator: `Unary: op=('-' | OP_NOT | 'typeof') expr=Primary`. This also gives JavaScript precedence (`typeof 1 + "x"` is `"numberx"`).
  - Interpreter: add `case "typeof"` to `caseUnary`, delete `caseTypeOfExpression`.
  - Generator: emit `typeof (…)` in `generate(Unary)`. The generator currently has no case for `typeof` at all, so the transpiler fails on it ("Unhandled parameter types: TypeOfExpressionImpl"). Then add the differential test to `ValuesTest.testTypeOf`.
  - Consequences: `typeof undeclared` becomes a linking error (consistent, since all names are known statically) and `typeof a.b.c` with `a.b` undefined becomes a `TypeError` (as in JavaScript).
  - Tests: `typeof f()`, `typeof a[0]`, `typeof (1 + 2)`, `typeof o.x`, precedence, plus differential tests.

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

- **Remaining syntax restrictions** (decide consciously): computed keys like `[a > b]` only allow additions, no `!!x`.

- **More functions in the runtime library.**


Interpreter and code quality
----------------------------

- **Triage the TODO comments in the code** (about 40). Also: the grammar rule `RangeExpression` is unused; there is an empty validator check.

- **`JvmScriptEngineRunner` needs Graal.js**, so its 12 tests are always skipped. Either add the GraalJS dependency or remove it.

- **Generator: `==` / `!=` are emitted unchanged**, so the generated Javascript coerces types (`1 == "1"` is `true` there, `false` in the interpreter). Emit `===` / `!==`, except for comparisons with `null`, which must keep `null == undefined`.

- **To verify** (noticed while reading the code):
  - The generator ignores `for (x : arr)` without `var` and evaluates the iterable three times.
  - `new Foo` without parentheses returns the function itself.


Build and operations
--------------------

- **Drop the CVE-2026-114938 workaround** (`-XX:-UseBASE64Intrinsics` in the webdemo `Dockerfile`) once the `eclipse-temurin:25-jre` image ships 25.0.5.1.

- **Webdemo: CPU time limit** (`maxMXCpuTime`) as a second safety net besides the statement limit.

- **Re-test Maven 3.10+ with Tycho 5.0.4.** The wrapper pins Maven 3.9.16 because 3.10 failed with Tycho 5.0.2.

- **Language server support for Visual Studio Code** (idea from the original roadmap; the Xtext update that makes it possible is done).


Not planned for now
-------------------

- **Bytecode compiler.** Deliberately not pursued at the moment; the tree-walking interpreter stays.
