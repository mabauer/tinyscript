# TODO

Open issues and ideas for tinyscript. Each item is meant to become its own PR,
with tests first.

## Next

**Goal: tinyscript is good enough to solve an Advent of Code** (decided
2026-11-20). A probe of typical building blocks showed what is missing.
tinyscript stays deliberately stricter than Javascript; every addition is
Javascript-compatible where it exists there, so the generator passes it through
and differential tests run against node.

0. ~~**Conversions & strictness.**~~ (done) One consistent rule before adding
   builtins:
   - Arithmetic `-`, `*`, `/`, `%` only with numbers (today `1 - "a"`, `"a" * 2`
     are silently `NaN`) → `TypeError`.
   - `+` only number + number, string + value, array + array (today `1 + true`,
     `1 + null`, `1 + undefined` are silently `NaN`) → `TypeError`.
   - Unary `-`: `-NaN` is `NaN` (today a `TypeError` whose message shows a Java
     object); other non-numbers → `TypeError` with a readable message.
   - `undefined` / `null` print and concatenate as `undefined` / `null` (today
     `Undefined` / `Null`), as in Javascript.
   - String conversion (`"x" + o`, `String(o)`) uses a user-defined `toString`
     (today the internal `{toString: [Function: ]}` format).
   - Reading `"abc"[1]` gives `"b"` (today `undefined`); writing is a
     `TypeError`.
   - Builtins do not convert silently: an argument of the wrong type is a
     `TypeError` (today `Math.sqrt("4")` is `2`). Conversion is explicit
     (`Number`, `parseInt`, `String`, see 1).
   - **Conditions** (`if`, `while`, `?:`, `assert`, `!`, `&&`, `||`) accept a
     boolean, `null` / `undefined` (false) or an object (true); numbers and
     strings are a `TypeError` (the traps `0`, `""`, `NaN`). So `while (node)`,
     `if (o.x)` for objects keep working, `if (count)` must be written
     `count > 0`.
   - A user-defined `valueOf` is deliberately not supported (it would be
     implicit conversion in operators).
   - Kept on purpose (document): calling a user function with too few arguments
     gives `undefined` for the rest, extra arguments are ignored; reading a
     missing property gives `undefined`.
   - Update the scripts that rely on the old behavior; document everything in
     the README ("Design decisions").
1. ~~**Strings and numbers:**~~ (done, plus `padEnd`) `split`, `trim`,
   `startsWith`, `endsWith`, `includes`, `replace` / `replaceAll` (strings only,
   no regex), `slice`, `lastIndexOf`, `repeat`, `padStart`, `toUpperCase` /
   `toLowerCase`, `charCodeAt`, `String.fromCharCode`; `Number(s)` (trims, `""`
   and non-numbers are `NaN`), `parseInt(s[, radix])` (lenient like Javascript),
   `isNaN`, `Number.isInteger`.
2. ~~**Arrays:**~~ (done; also `join()` defaults to `","`, callbacks get
   `(item, index, array)`) `sort` (with comparison function), `slice`, `reduce`,
   `forEach`, `some`, `every`, `find`, `findIndex`, `includes`, `fill`,
   `new Array(n)`.
3. ~~**Syntax:**~~ (done) `continue`, `cond ? a : b`, `+=` / `-=` / `*=` / `/=`
   / `%=`; `++` / `--` only as statements (`i++;`, not inside expressions).
4. ~~**`Map` and `Set`**~~ (done) as in Javascript (keys of any type, `has`
   returns a boolean); no `delete` operator.
   - ~~**Arrow functions (bug, found with Map/Set):**~~ (fixed) the generator
     emits every arrow body as `return <body>`, so in a `{ … }` body only the
     first statement runs in node; the interpreter returns the value of the last
     statement of a `{ … }` body (Javascript: `undefined` without `return`). The
     AST must distinguish `x => expr` from `x => { … }`; also the generator
     drops rest parameters (`...args`).
5. ~~**Robustness:**~~ (done) deep recursion must not crash the REPL with a Java
   `StackOverflowError` (run the interpreter in a thread with a large stack,
   report an overflow as a tinyscript error); a call of a missing method says so
   ("String has no method 'split'" instead of "Calls are only allowed for
   function objects"); script arguments after `--` in `System.args`
   (`tinyscript day1.tys -- input.txt`).
6. ~~**Practice:**~~

   - Days 1–4 of 2024 (in `examples/aoc2024`, tested in the interpreter and in
     node). Findings:
     - ~~**Bug: builtins ignore extra arguments.**~~ (fixed: Javascript's
       optional arguments, otherwise a `TypeError`; `words()` in `std:string`)
       `"abc".startsWith("b", 1)` is `false` (Javascript: `true`); the same for
       `endsWith(s, end)`, `includes(s, start)` of strings and arrays,
       `split(sep, limit)`. Either support these arguments like Javascript or
       make too many arguments a `TypeError`.
     - Speed is fine: each day takes under a second on inputs of real size
       (1,000 lines, a 140×140 grid), JVM start included.
     - Missing but bearable: ~~`??`~~ (done, with `?.`), ~~a way to stop with an
       error message (`throw`)~~ (done, with a minimal `Error`), ~~destructuring
       (`pair[0]`, `pair[1]`)~~ (done, flat patterns in `var` and `for`).
       Decided 2026-11-21: `throw` (with a minimal `Error`, no `try` / `catch`),
       then destructuring limited to flat patterns in `var` and `for`
       (`var [a, b] = pair;`, `for (var [key, value] : map)`,
       `var { row, col } = p;`); an array that is too short is a `TypeError`.
     - ~~**Generator bug (found with destructuring):**~~ (fixed: `let`, and a
       hidden counter in numeric `for` loops) it emits `var`, which is
       function-scoped in Javascript, while tinyscript's variables are
       block-scoped. Shadowing in a nested block and closures over loop
       variables (`for (var x : a) { fs.push(() => x); }`) behave differently in
       node. Emit `let` instead (check: redeclarations, the loop variables of
       the generated `for` loops).
     - The inclusive numeric `for` makes `for (let i = 0, a.length - 1)` the
       common case — see item 8.
   - Days 5 and 6 added.
     - **Speed** is the next limit: part 2 of day 6 (a simulation, 20–30 million
       steps for a real input) takes about a minute; on a test grid with 8
       million steps, tinyscript needs 19 s, CPython 1.9 s. A profile (JFR)
       shows the costs of the tree walk: `dispatch` / `execute` with `eClass()`
       per node, `HashMap` lookups of variables in the execution contexts,
       string comparisons of operators. Ideas: resolve variables to slots once,
       dispatch on cached node kinds or precompiled closures, ~~operators as
       enums~~ (done 2026-11-20: operators are enums in the syntax tree; about 3
       % faster, 20.0 s → 19.4 s on the test grid, as expected — the big costs
       are the dispatch and the variable lookups). Profile 2026-11-20 (JFR, AoC
       days 9, 14, 20, 22 with large inputs): dispatch (`execute` → `dispatch` →
       `eClass()` → switch) 34–59 %, variable lookup 5–15 %, creating contexts
       (a `HashMap` per block / loop iteration) 6–16 %, objects / properties
       6–14 %, operators 5–13 %; GC hardly matters. ~~Node kind without
       `eClass()`~~ (done: a `ClassValue` by implementation class, 5–15 %
       faster; a Java pattern switch over the node interfaces was not faster).
       Decided 2026-11-20: **no variable slots** for now — an understandable
       engine (a name is looked up in a chain of contexts, just as scoping is
       explained) is worth more than the expected 15–25 %; slots would add an
       analysis pass and a class of silent errors (a wrong index reads the wrong
       variable). If a concrete program is ever too slow: first lighter contexts
       that stay name-based (a small array of names and values with linear
       search instead of a `HashMap` per block / iteration; expected 5–15 %,
       only `ExecutionContext` changes). Compiling the tree to closures only as
       a deliberate architecture decision. Measured 2026-11-21 (fixing `reverse`
       of `std:array`): a call of a builtin method such as `push` costs about
       1.2 µs, ten times an index access (`a[i]`, `r[i] = x`, about 0.13 µs);
       worth a look before the contexts if method calls dominate a profile.
   - Days 7–10 (2026-11-21): all four ran correctly on the first attempt.
     Findings:
     - Fixed: the generator crashed on a `for` loop with a pattern or over an
       existing variable nested inside another loop (shadowing check assumed one
       loop variable).
     - ~~Missing: **object literal shorthand** `{ row, col }`~~ (done; on the
       way: object properties now keep their insertion order, like Javascript)
       for `{ row: row, col: col }`, written again and again in days 6, 8, 9 and
       10; the counterpart of the object patterns. Cheap: grammar, interpreter,
       the generator passes it through.
     - Speed: day 7 (up to 3^11 operator combinations per line) takes about 6 s,
       day 9 (a linear search of the free spans for each of 10,000 files) about
       10 s.
   - Days 11–15 (2026-11-21): all correct on the first run once written (checked
     against a Python reference); findings:
     - ~~Missing: **destructuring in parameters**~~ (done),
       `robots.map(({ x, y, vx, vy }) => …)` — the natural Javascript idiom,
       excluded so far (flat patterns only in `var` / `for`). Flat patterns in
       function and arrow parameters would be the consistent next step.
     - The parser's messages for it are confusing: "mismatched input '=>'
       expecting ')'" plus a cascade of follow-up errors ("Couldn't resolve
       reference …"). Better: one clear message for known unsupported
       constructs, or only the first syntax error.
     - ~~Missing: `Infinity`~~ (done, with `NaN`; `Number.MAX_SAFE_INTEGER` etc.
       still missing); "no limit" had to be written `1 / 0`.
     - ~~Reading numbers out of text~~ (done: `integers(text)` in `std:string`)
       without regular expressions needs a helper (`integers(line)` in
       `examples/aoc2024/aoc.tys`, used by days 13 and 14); a candidate for
       `std:string`.
     - Speed: all days within seconds (day 14 worst case 6.6 s, day 11 3.2 s).
   - Days 16–20 (2026-11-21): all correct (checked against a Python reference),
     each within seconds (day 20: 6.6 s). Findings:
     - ~~**Bug:**~~ (fixed: they stay reserved, decided 2026-11-21, with the
       message "'from' is a reserved word in tinyscript and cannot be used as a
       name"; all keywords are fine as property names, `range.from`) `from` and
       `as` are reserved words (because of `import … from` / `* as m`), but in
       Javascript they are ordinary names: `const from = 1;` fails with
       "Unexpected 'from'".
     - ~~**Bug:**~~ (fixed: messages for rest elements in patterns and spread in
       array and object literals) for `const [first, ...rest] = items;` the
       message says "... is only allowed for rest parameters: to call a function
       with an array, use f.apply(null, array)" — about calls, although it is a
       pattern.
     - ~~Missing: **bitwise operators**~~ (done) (`^`, `&`, `|`, `<<`, `>>`) and
       **powers** (`**` or `Math.pow`); day 17 emulates XOR and shifts with
       arithmetic.
     - Missing: **spread in calls** (`Math.min(...values)`) — reached for it
       twice; and **rest elements in array patterns** (`[first, ...rest]`).
     - Missing: a **priority queue** (day 16 has a binary heap of its own); a
       candidate for a std module.
     - The new error messages helped: `queue[next++]` was answered with "++ and
       -- are statements, not expressions: write i++; on its own".
   - Days 21–25 (2026-11-21): all of Advent of Code 2024 is solved; all days
     checked against a Python reference. Findings:
     - ~~**Bitwise operators**~~ (done, on 53 bits, decided 2026-11-20; day 22
       now takes 6.8 s) are the clearest gap: day 22 (12 million XORs) takes 34
       s because XOR has to be done with a table of bytes.
     - ~~Cosmetic:~~ (fixed; it also lost the callers whose call is in such a
       block) in a stack trace the innermost call is named after the block it is
       in (`at if(…)`) instead of the function (`valueOf`).
     - The message for reserved words caught
       `function movesBetween(keypad, from, to)` right away.

7. **Error reporting** (plan, decided 2026-11-21; a catalog of 15 typical
   mistakes, mostly Javascript habits, showed cascades of follow-up errors and
   raw ANTLR messages). Messages stay in English.
   1. ~~**Only the errors that matter:**~~ (done) after a syntax error only the
      first syntax error (no follow-up syntax, linking and validation errors);
      without syntax errors all errors in source order. One place for REPL, web
      demo, module loading and tests (`TinyscriptDiagnostics`);
      `file:line:column` and, in the REPL, the source line with a `^` marker.
      Still open: the web demo editor could mark the column.
   2. ~~**Understandable syntax errors:**~~ (done; also `for (const x of …)` /
      `in`, and a missing `;` names the line) an own
      `ISyntaxErrorMessageProvider`: translate the ANTLR patterns ("missing ';'
      at 'print'" → "Missing ';' at the end of the statement before 'print'"),
      and recognize known Javascript constructs with a concrete hint: C-style
      `for (let i = 0; i < n; i++)`, `===`, `if (x) stmt;` without a block,
      template strings, default parameters, `i++` inside an expression, `...` in
      calls, regular expressions. Reserve `try`, `catch`, `switch`, `case`,
      `class` as keywords to report them as "not supported in tinyscript".
   3. ~~**Unknown names:**~~ (done) "Unknown name 'valeus' (did you mean
      'values'?)" — an own linking diagnostic message provider with suggestions
      from the visible names (edit distance).
   4. ~~**Runtime errors with context:**~~ (done; open: the web demo editor
      could use line and column to mark the place)
      `o.lst is undefined, cannot read 'length'` instead of "Cannot access
      property of undefined or null"; review the other runtime messages; column
      and source excerpt as for syntax errors.
   - Each step extends `ErrorMessagesTest` (the catalog: mistake → expected
     message). The Eclipse editor gets 2 and 3 for free.
8. ~~**The numeric `for` loop**~~ (done, decided 2026-11-20: a global, lazy
   `range(end)` / `range(start, end[, step])` with exclusive end as in Python,
   for `for (const i : range(n))`; replaces `std:array.range`; the Lua-style
   numeric loop stays) annoys in practice: with the inclusive end,
   `for (let i = 0, a.length - 1)` is the common case, and Javascript habits
   (`for (let i = 0; i < n; i++)`) fail. Options to discuss: an exclusive form
   or a `range(n)` that the iterable loop can use (`for (const i : range(n))`,
   already in `std:array`), supporting the C-style loop, or at least a precise
   error message (see 7.2).

## Language

- **Modules** (detailed plan, decided 2026-11-20). One file is one module;
  `export` before top-level `function` / `var`; no re-exports, no cycles.

  ```js
  // geometry.tys
  export function area(r) { return Math.PI * r * r; }
  export const unit = "cm";
  const helper = 2;                          // not exported, private

  // main.tys
  import area, unit from "./geometry";     // named imports
  import * as geo from "./geometry.tys";   // the whole module as an object
  print(area(2), unit, geo.area(1));
  ```

  - **Syntax.** `import a, b from "path";` binds the named exports;
    `import * as m from "path";` binds the frozen exports object. No braces as
    in Javascript; renaming (`import a as x from …`) is left for later (can be
    added without breaking anything).
  - **Resolving "path".**
    - Relative (`"./x"`, `"../lib/y"`): relative to the _importing file_, not
      the working directory.
    - Bare (`"x"`, `"math/vector"`): searched in the _module path_, a list of
      directories (like a class path). REPL: `--module-path dir1:dir2` or the
      environment variable `TINYSCRIPT_PATH`; default is the directory of the
      main file (interactive: the working directory).
    - `"std:…"`: standard library from the classpath (later, see PR 4).
    - The extension `.tys` may be omitted; it is appended if the path has none.
    - A module's identity is its canonical absolute path (`toRealPath`), so
      different relative paths or a symlink to the same file are one module.
    - Errors name the import line and what was searched:
      `Module "geometry" not found (searched: …)`.
    - Webdemo (sandbox): no file access, only `std:` modules; any other import
      is an error.
  - **Repeated imports in the import graph.** Each module is executed exactly
    once (cache by identity); every importer gets the same exports object, so
    the same values and object instances (diamonds are fine). Each module has
    its own scope: two modules importing the same name do not conflict. Within
    one module, importing a name twice (also from different modules) or a clash
    with a local declaration is a "Duplicate identifier" (validator), like
    duplicate `var`.
  - **Bindings are read-only snapshots.** The exports object is frozen after the
    module has run; assigning to an imported name is a compile-time error.
    Imported objects stay mutable, and module-internal state via closures works.
    Simpler than Javascript's live bindings.
  - **Errors.** Cyclic imports name the chain
    (`Cyclic import: main.tys → m1.tys → m2.tys → m1.tys`); importing a name
    that is not exported: `"m1.tys" does not export "id2"`.
  - **Execution.** A module runs in its own context that only sees the builtins;
    one engine, so resource limits apply across all modules. Loading via a
    `ModuleLoader` interface passed to the engine (file system loader with
    module path; classpath loader for `std:`); without a loader, `import` is an
    error.
  - **PRs.**
    1. ~~Restrict the global scope to the builtins~~ (done:
       `TinyscriptGlobalScopeProvider` only sees resources marked with
       `SharedGlobals`, i.e. the builtins and the lines of an interactive REPL
       session; files are isolated, the REPL runs each file with a fresh
       engine).
    2. ~~Core~~ (done: grammar, validator, interpreter with `ModuleLoader` /
       `FileSystemModuleLoader` and `ModuleExports`, REPL `--module-path` /
       `TINYSCRIPT_PATH`; `ModulesTest`).
    3. ~~Generator~~ (done: bundles a program and its modules into one script;
       differential tests in `ModulesTest`).
    4. ~~Standard library and webdemo~~ (done: `std:array` written in tinyscript
       (`std:math` was dropped again: `Math` stays a global, as in Javascript),
       `StandardLibraryModuleLoader`; the webdemo imports only `std:` modules
       and has a "Modules" example).
    5. `fs` as a module (target, decided 2026-11-20): ~~`std:fs` as a native
       module~~ (done: `FileSystemModule`, generator wrapper for node; the
       sandbox does not provide it), ~~then remove the global `fs`~~ (done;
       breaking: scripts must `import * as fs from "std:fs";`). The new API
       (strict: errors end the script, since tinyscript has no try/catch; UTF-8;
       relative paths against the working directory):
       - `exists(path)` → boolean, never fails
       - `readFile(path)` → string; `readLines(path)` → array of lines (split at
         `\n` / `\r\n`, a final line break adds no empty line)
       - `writeFile(path, text)`, `writeLines(path, lines)` (each line ends with
         `\n`), `appendFile(path, text)` → `undefined`; text / lines must be
         strings (`TypeError` otherwise); `readLines` after `writeLines` gives
         the same array
       - `listDir(path)` → sorted array of names
       - no delete / mkdir (dangerous, no need so far)
    6. Optional, IDE: the validator checks while typing that the imported file
       exists and exports the names (fits the VS Code language server).

- **More functions in the runtime library.** (`concat` is not needed: `+`
  concatenates arrays.) See the review below for the concrete list.

- **Language review** (2026-11-20; grammar, language reference and about 70
  probe scripts against the REPL of 0.21.1-SNAPSHOT). The goal stays a small
  language that is practical for experiments. The bugs it found are under
  "Interpreter and code quality".

  - **Gaps**, by weight:

    1. **No error handling.** Without `try` / `catch`, `throw` only ends the
       script: `fs.readFile` on a missing file cannot be handled, and libraries
       cannot report errors in a useful way. The biggest gap for practical use.
       Revisits the decision of 2026-11-21. `throw` and all runtime errors are
       already Java exceptions in the interpreter (like `break` and `return`),
       so catching is cheap. Decided 2026-11-20: **the minimal `try` /
       `catch`**. The variants:
       - **Minimal `try { … } catch (e) { … }`** (chosen), no `finally`:
         grammar, one case in the interpreter, the generator passes it through;
         `try` / `catch` are already reserved.
       - **A protected call as in Lua:**
         `const r = attempt(() => fs.readFile(p));` with `r.ok`, `r.value`,
         `r.error`. One builtin, no grammar change; the generator needs a small
         helper for node.
       - **Return values instead of errors** (`fs.readFile` gives `null` for a
         missing file): no new mechanism, but only works for single functions
         and does not help libraries written in tinyscript.
       - Still to decide: _what is caught_ — everything, or only expected errors
         (thrown values, input / output) while a `TypeError` stays a programming
         error that ends the script (stricter, and it avoids comparing runtime
         messages that differ between the interpreter and node); _what `e` is_ —
         simplest: always an `Error` with `name` and `message`, any other thrown
         value as it is; _sandbox_ — resource limit violations and the stack
         overflow must never be catchable.
    2. ~~**`undefined` as a name**~~ (done 2026-11-21: a literal like `null`).
    3. **No input.** Only files and `System.args`; nothing reads standard input,
       so interactive scripts are not possible. Candidate: `readLine()` in a
       `std:` module (not sandboxed).
    4. **Arrays and objects cannot shrink.** Arrays: no `splice` (insert /
       remove in the middle), no `reverse` method (only `std:array`). Objects:
       no `delete`, no `Object.values` / `Object.entries`.
    5. **Numbers.** `Math` has nine members: powers (`**` or `Math.pow`),
       `trunc`, `sign`, `log`, the trigonometric functions are missing;
       formatting is done: `toFixed` and `toString(radix)` (2026-11-21; not now:
       `toPrecision`, `toExponential`; not at all: `toLocaleString`,
       `printf`-like format strings).
    6. **Objects with methods are verbose.** The mechanics are complete
       (`Object.create`, `prototype`, `instanceof`); what is missing is a
       comfortable spelling. Tendency (2026-11-20): **no inheritance for now**,
       in this order:

       1. **Method shorthand** `{ greet() { … } }` for
          `greet: function() { … }`: grammar only, the generator passes it
          through. It also makes factory functions with closures readable
          (`return { inc() { n++; }, get() { return n; } };`), which give
          private state without `this`.
       2. **`class` without inheritance:** short for a constructor function plus
          methods on its prototype; no `extends`, no `super` (the expensive
          part), no static members, no getters.
       3. **`interface` / `implements`** (spelling as in TypeScript), to say
          that two classes implement the same protocol:

          ```js
          interface Shape { area(); describe(); }

          class Circle implements Shape {
              constructor(r) { this.r = r; }
              area() { return Math.PI * this.r * this.r; }
              describe() { return "circle " + this.r; }
          }
          ```

          An interface names methods and their number of parameters, without
          bodies. Nominal: only a class that says `implements Shape` is a
          `Shape`, so the validator checks it before the script runs (a missing
          method or a wrong number of parameters is an error at the class, also
          in the Eclipse editor). The generator drops `interface` and
          `implements`. Limit: without types on parameters nothing checks that
          an argument is a `Shape`; `shape.area()` stays dynamic.

       4. If needed: `x instanceof Shape` at runtime (the class remembers its
          interfaces; the generator needs a helper for node, Javascript has no
          interfaces).

       - Not pursued: structural interfaces as in Go (only checkable at
         runtime); a `std:` module with `extend` / `_new` helpers (a convention
         nobody knows); a library function that checks for methods at runtime
         (works with object literals, but loses the check before the run).

    7. Smaller: spread in calls (`Math.max(...values)`), renaming in imports
       (`import a as x from …`), assignment with a pattern (`[a, b] = [b, a];`).

    - Deliberately not gaps: `switch`, template strings, labels, `do … while`,
      regular expressions, `Date`. The messages for them are good, and the
      language stays small.

  - **Design questions:**
    - **Counting loops.** Three ways side by side: the Lua-style loop (inclusive
      end), `range` (exclusive end) and `while`; plus `var` as a synonym of
      `let`. Favoured (2026-11-20): a lazy `reverse()` on ranges, so that
      counting backwards over indexes reads
      `for (const i : range(a.length).reverse())` instead of
      `range(a.length - 1, -1, -1)`. Done 2026-11-21: `range(…).reverse()`; the
      four backward loops of the AoC solutions use it, so they have no Lua-style
      loop left, and the web demo has a loops example without one. Decided and
      done 2026-11-21: the Lua-style loop and `var` are removed (no deprecation,
      there are no other users); `var` stays a reserved word, and both get a
      syntax error that says what to write instead.
    - **Half truthiness.** In conditions `null` / `undefined` are false and
      objects are true (`if (node)`, `while (node)`), but `&&` and `||` always
      give a boolean. So the Javascript habit `const cfg = options || {};`
      silently yields `true`; numbers and strings are caught by a `TypeError`,
      objects are not. Options: (1) keep it and name the trap in the language
      reference (`??` is the way: `options ?? {}`), or (3) conditions accept
      only booleans, which is the most uniform but costs `while (node)` and
      `if (o.child)` (write `node != null`). Open, tendency (2026-11-20): 1
      or 3. Not pursued: (2) a `TypeError` for an object as operand of `&&` /
      `||`.
  - **Suggested order:** ~~the bugs~~ (all fixed 2026-11-21; also `toFixed` /
    `toString(radix)`); ~~`undefined`~~ (done); minimal `try` / `catch`; a
    library batch (`splice`, `reverse`, `Object.values` / `entries`, powers,
    `Math.trunc`; `range(…).reverse()` is done); method shorthand, a more
    comfortable REPL, standard input.

## Interpreter and code quality

- **`JvmScriptEngineRunner` needs Graal.js**, so its 12 tests are always
  skipped. Either add the GraalJS dependency or remove it (decision deferred).
  Its error handling is minimal.

## Build and operations

- **Webdemo: CPU time limit** (`maxMXCpuTime`) as a second safety net besides
  the statement limit.

- **Re-test Maven 3.10+ with Tycho 5.0.4.** The wrapper pins Maven 3.9.16
  because 3.10 failed with Tycho 5.0.2.

- **Visual Studio Code support** (idea from the original roadmap; the Xtext
  update that makes it possible is done).
  - File extension: decided and done, tinyscript files use `.tys` (`.ts` belongs
    to TypeScript in VS Code).
  - ~~Stage 1, syntax highlighting only~~ (done 2026-11-21:
    `de.mkbauer.tinyscript.vscode`, a TextMate grammar following the Xtext
    grammar, `language-configuration.json`, grammar tests with
    `vscode-tmgrammar-test` and the `.vsix` package in CI). Not done: a "Run
    tinyscript" task (would need extension code; the REPL in a terminal does
    it).
  - Stage 2, language server (about 1–2 days): validation while typing,
    completion, go to definition, references, rename, outline come from Xtext.
    Build a fat jar from core + `de.mkbauer.tinyscript.ide` with
    `org.eclipse.xtext.ide.server.ServerLauncher` as main class (a plain Maven
    module like the REPL; needs `xtext.ide` and `lsp4j`). A small TypeScript
    extension starts it via `vscode-languageclient`
    (`java -jar tinyscript-ls.jar`, Java 25 required, path configurable).
    Package with `vsce` as `.vsix`; later optionally Marketplace / Open VSX.
  - CI: build the server jar (the `.vsix` is already built); a smoke test that
    starts the server and validates a file with an error.

## Not planned for now

- **Bytecode compiler.** Deliberately not pursued at the moment; the
  tree-walking interpreter stays.
