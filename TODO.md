TODO
====

Open issues and ideas for tinyscript. Each item is meant to become its own PR, with tests first.


Next
----

**Goal: tinyscript is good enough to solve an Advent of Code** (decided 2026-11-20). A probe of typical building blocks showed what is missing. tinyscript stays deliberately stricter than Javascript; every addition is Javascript-compatible where it exists there, so the generator passes it through and differential tests run against node.

0. ~~**Conversions & strictness.**~~ (done) One consistent rule before adding builtins:
   - Arithmetic `-`, `*`, `/`, `%` only with numbers (today `1 - "a"`, `"a" * 2` are silently `NaN`) → `TypeError`.
   - `+` only number + number, string + value, array + array (today `1 + true`, `1 + null`, `1 + undefined` are silently `NaN`) → `TypeError`.
   - Unary `-`: `-NaN` is `NaN` (today a `TypeError` whose message shows a Java object); other non-numbers → `TypeError` with a readable message.
   - `undefined` / `null` print and concatenate as `undefined` / `null` (today `Undefined` / `Null`), as in Javascript.
   - String conversion (`"x" + o`, `String(o)`) uses a user-defined `toString` (today the internal `{toString: [Function: ]}` format).
   - Reading `"abc"[1]` gives `"b"` (today `undefined`); writing is a `TypeError`.
   - Builtins do not convert silently: an argument of the wrong type is a `TypeError` (today `Math.sqrt("4")` is `2`). Conversion is explicit (`Number`, `parseInt`, `String`, see 1).
   - **Conditions** (`if`, `while`, `?:`, `assert`, `!`, `&&`, `||`) accept a boolean, `null` / `undefined` (false) or an object (true); numbers and strings are a `TypeError` (the traps `0`, `""`, `NaN`). So `while (node)`, `if (o.x)` for objects keep working, `if (count)` must be written `count > 0`.
   - A user-defined `valueOf` is deliberately not supported (it would be implicit conversion in operators).
   - Kept on purpose (document): calling a user function with too few arguments gives `undefined` for the rest, extra arguments are ignored; reading a missing property gives `undefined`.
   - Update the scripts that rely on the old behavior; document everything in the README ("Design decisions").
1. ~~**Strings and numbers:**~~ (done, plus `padEnd`) `split`, `trim`, `startsWith`, `endsWith`, `includes`, `replace` / `replaceAll` (strings only, no regex), `slice`, `lastIndexOf`, `repeat`, `padStart`, `toUpperCase` / `toLowerCase`, `charCodeAt`, `String.fromCharCode`; `Number(s)` (trims, `""` and non-numbers are `NaN`), `parseInt(s[, radix])` (lenient like Javascript), `isNaN`, `Number.isInteger`.
2. ~~**Arrays:**~~ (done; also `join()` defaults to `","`, callbacks get `(item, index, array)`) `sort` (with comparison function), `slice`, `reduce`, `forEach`, `some`, `every`, `find`, `findIndex`, `includes`, `fill`, `new Array(n)`.
3. ~~**Syntax:**~~ (done) `continue`, `cond ? a : b`, `+=` / `-=` / `*=` / `/=` / `%=`; `++` / `--` only as statements (`i++;`, not inside expressions).
4. ~~**`Map` and `Set`**~~ (done) as in Javascript (keys of any type, `has` returns a boolean); no `delete` operator.
4a. ~~**Arrow functions (bug, found with Map/Set):**~~ (fixed) the generator emits every arrow body as `return <body>`, so in a `{ … }` body only the first statement runs in node; the interpreter returns the value of the last statement of a `{ … }` body (Javascript: `undefined` without `return`). The AST must distinguish `x => expr` from `x => { … }`; also the generator drops rest parameters (`...args`).
5. ~~**Robustness:**~~ (done) deep recursion must not crash the REPL with a Java `StackOverflowError` (run the interpreter in a thread with a large stack, report an overflow as a tinyscript error); a call of a missing method says so ("String has no method 'split'" instead of "Calls are only allowed for function objects"); script arguments after `--` in `System.args` (`tinyscript day1.tys -- input.txt`).
6. **Practice:** solve two or three real Advent of Code days with the example inputs from the puzzle texts as integration tests; find what is still missing and whether the interpreter is fast enough.


Language
--------

- **Modules** (detailed plan, decided 2026-11-20). One file is one module; `export` before top-level `function` / `var`; no re-exports, no cycles.

  ```js
  // geometry.tys
  export function area(r) { return Math.PI * r * r; }
  export var unit = "cm";
  var helper = 2;                          // not exported, private

  // main.tys
  import area, unit from "./geometry";     // named imports
  import * as geo from "./geometry.tys";   // the whole module as an object
  print(area(2), unit, geo.area(1));
  ```

  - **Syntax.** `import a, b from "path";` binds the named exports; `import * as m from "path";` binds the frozen exports object. No braces as in Javascript; renaming (`import a as x from …`) is left for later (can be added without breaking anything).
  - **Resolving "path".**
    - Relative (`"./x"`, `"../lib/y"`): relative to the *importing file*, not the working directory.
    - Bare (`"x"`, `"math/vector"`): searched in the *module path*, a list of directories (like a class path). REPL: `--module-path dir1:dir2` or the environment variable `TINYSCRIPT_PATH`; default is the directory of the main file (interactive: the working directory).
    - `"std:…"`: standard library from the classpath (later, see PR 4).
    - The extension `.tys` may be omitted; it is appended if the path has none.
    - A module's identity is its canonical absolute path (`toRealPath`), so different relative paths or a symlink to the same file are one module.
    - Errors name the import line and what was searched: `Module "geometry" not found (searched: …)`.
    - Webdemo (sandbox): no file access, only `std:` modules; any other import is an error.
  - **Repeated imports in the import graph.** Each module is executed exactly once (cache by identity); every importer gets the same exports object, so the same values and object instances (diamonds are fine). Each module has its own scope: two modules importing the same name do not conflict. Within one module, importing a name twice (also from different modules) or a clash with a local declaration is a "Duplicate identifier" (validator), like duplicate `var`.
  - **Bindings are read-only snapshots.** The exports object is frozen after the module has run; assigning to an imported name is a compile-time error. Imported objects stay mutable, and module-internal state via closures works. Simpler than Javascript's live bindings.
  - **Errors.** Cyclic imports name the chain (`Cyclic import: main.tys → m1.tys → m2.tys → m1.tys`); importing a name that is not exported: `"m1.tys" does not export "id2"`.
  - **Execution.** A module runs in its own context that only sees the builtins; one engine, so resource limits apply across all modules. Loading via a `ModuleLoader` interface passed to the engine (file system loader with module path; classpath loader for `std:`); without a loader, `import` is an error.
  - **PRs.**
    1. ~~Restrict the global scope to the builtins~~ (done: `TinyscriptGlobalScopeProvider` only sees resources marked with `SharedGlobals`, i.e. the builtins and the lines of an interactive REPL session; files are isolated, the REPL runs each file with a fresh engine).
    2. ~~Core~~ (done: grammar, validator, interpreter with `ModuleLoader` / `FileSystemModuleLoader` and `ModuleExports`, REPL `--module-path` / `TINYSCRIPT_PATH`; `ModulesTest`).
    3. ~~Generator~~ (done: bundles a program and its modules into one script; differential tests in `ModulesTest`).
    4. ~~Standard library and webdemo~~ (done: `std:array` written in tinyscript (`std:math` was dropped again: `Math` stays a global, as in Javascript), `StandardLibraryModuleLoader`; the webdemo imports only `std:` modules and has a "Modules" example).
    5. `fs` as a module (target, decided 2026-11-20): ~~`std:fs` as a native module~~ (done: `FileSystemModule`, generator wrapper for node; the sandbox does not provide it), ~~then remove the global `fs`~~ (done; breaking: scripts must `import * as fs from "std:fs";`). The new API (strict: errors end the script, since tinyscript has no try/catch; UTF-8; relative paths against the working directory):
       - `exists(path)` → boolean, never fails
       - `readFile(path)` → string; `readLines(path)` → array of lines (split at `\n` / `\r\n`, a final line break adds no empty line)
       - `writeFile(path, text)`, `writeLines(path, lines)` (each line ends with `\n`), `appendFile(path, text)` → `undefined`; text / lines must be strings (`TypeError` otherwise); `readLines` after `writeLines` gives the same array
       - `listDir(path)` → sorted array of names
       - no delete / mkdir (dangerous, no need so far)
    6. Optional, IDE: the validator checks while typing that the imported file exists and exports the names (fits the VS Code language server).

- **Global `this`.** At top level `this` is the global object. In Javascript's strict mode (which tinyscript otherwise follows) it is `undefined`.

- **More functions in the runtime library.** (`concat` is not needed: `+` concatenates arrays.)

- **`?.` and `??`** as in Javascript (`o?.x`, `a ?? b`): shorter null checks.


Interpreter and code quality
----------------------------

- **`JvmScriptEngineRunner` needs Graal.js**, so its 12 tests are always skipped. Either add the GraalJS dependency or remove it (decision deferred). Its error handling is minimal.


Build and operations
--------------------

- **Drop the CVE-2026-114938 workaround** (`-XX:-UseBASE64Intrinsics` in the webdemo `Dockerfile`) once the `eclipse-temurin:25-jre-noble` image ships 25.0.5.1.

- **Webdemo: CPU time limit** (`maxMXCpuTime`) as a second safety net besides the statement limit.

- **Re-test Maven 3.10+ with Tycho 5.0.4.** The wrapper pins Maven 3.9.16 because 3.10 failed with Tycho 5.0.2.

- **Visual Studio Code support** (idea from the original roadmap; the Xtext update that makes it possible is done).
  - File extension: decided and done, tinyscript files use `.tys` (`.ts` belongs to TypeScript in VS Code).
  - Stage 1, syntax highlighting only (about half a day): an extension with `package.json`, a TextMate grammar (keywords as in the webdemo's `tinyscript-lang.ts`) and `language-configuration.json` (comments, brackets); optionally a "Run tinyscript" task that calls the REPL jar.
  - Stage 2, language server (about 1–2 days): validation while typing, completion, go to definition, references, rename, outline come from Xtext. Build a fat jar from core + `de.mkbauer.tinyscript.ide` with `org.eclipse.xtext.ide.server.ServerLauncher` as main class (a plain Maven module like the REPL; needs `xtext.ide` and `lsp4j`). A small TypeScript extension starts it via `vscode-languageclient` (`java -jar tinyscript-ls.jar`, Java 25 required, path configurable). Package with `vsce` as `.vsix`; later optionally Marketplace / Open VSX.
  - CI: build the server jar and the `.vsix`; a smoke test that starts the server and validates a file with an error.


Not planned for now
-------------------

- **Bytecode compiler.** Deliberately not pursued at the moment; the tree-walking interpreter stays.
