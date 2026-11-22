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

    1. ~~**No error handling.**~~ (done 2026-11-22: the minimal
       `try { … } catch (e) { … }`, optionally `catch { … }`, no `finally`).
       Decided 2026-11-22: `catch` gets only what a script can expect, errors
       thrown with `throw` and failed `std:fs` input / output (an `Error` named
       `IOError`); `TypeError`s, failed `assert`s, the stack overflow and
       resource limits end the script. `throw` takes only `Error` objects
       (breaking: `throw "text";` is a `TypeError`), so `e` is always an `Error`
       with `name` and `message`. `throw e;` throws a caught error again with
       its place and stack trace. Node: the generated `catch` rethrows
       `TypeError`, `RangeError`, `ReferenceError`, `SyntaxError` and failed
       asserts; the generated `std:fs` throws the same `IOError`s. Not chosen: a
       protected call as in Lua (`attempt(() => …)` with `r.ok`, `r.value`,
       `r.error`), return values instead of errors. Possible later: `finally`,
       error classes with `instanceof` (`TypeError`, `IOError`), patterns in
       `catch ({ message })`.
    2. ~~**`undefined` as a name**~~ (done 2026-11-21: a literal like `null`).
    3. **No input.** Only files and `System.args`; nothing reads standard input,
       so interactive scripts are not possible. Candidate: `readLine()` in a
       `std:` module (not sandboxed).
    4. **Arrays and objects cannot shrink.** Done 2026-11-21: `splice`,
       `reverse` / `toReversed`, `lastIndexOf`, `findLast` / `findLastIndex`,
       `flat`, `flatMap`; `Object.values`, `entries`, `fromEntries`, `assign`;
       `delete obj.key` for objects only (on an array a `TypeError` that points
       to `splice`).
    5. **Numbers.** Done 2026-11-21: `Math.pow`, `trunc`, `sign`, `exp`, `log`,
       `log2`, `log10`, `cbrt`, `hypot`, the trigonometric functions, `Math.E`;
       `parseFloat`, `Number.isFinite`, `MAX_SAFE_INTEGER` / `MIN_SAFE_INTEGER`;
       `Math.round` fixed (large numbers, `NaN`). `log2` and `cbrt` are ports of
       FreeBSD's msun, as V8 uses them (bit-exact with node); the others use
       `StrictMath` (in rare cases one unit in the last digit off). Deferred
       (2026-11-21): the operator `**` (rather not: `Math.pow` and `1 << n` do
       it, and it brings its own precedence rules) and spread in calls
       (`Math.max(...values)`; `f.apply(null, values)` does it). Formatting is
       done: `toFixed` and `toString(radix)` (2026-11-21; not now:
       `toPrecision`, `toExponential`; not at all: `toLocaleString`,
       `printf`-like format strings).
    6. **Objects with methods are verbose.** The mechanics are complete
       (`Object.create`, `prototype`, `instanceof`); what is missing is a
       comfortable spelling. Tendency (2026-11-20): **no inheritance for now**,
       in this order:

       Examples (2026-11-22): `examples/oo/expressions.tys` (symbolic
       expressions) and `examples/oo/orders.tys` (orders through an event bus),
       written with `class` and `interface` as planned, next to
       `expressions_today.tys` / `orders_today.tys` with prototypes;
       `OoExamplesTest` checks each pair against the same output (the planned
       versions once enabled).

       1. ~~**Method shorthand**~~ (done 2026-11-22; functions as values of
          properties are named after their key in stack traces)
          `{ greet() { … } }` for `greet: function() { … }`: grammar only, the
          generator passes it through. It also makes factory functions with
          closures readable (`return { inc() { n++; }, get() { return n; } };`),
          which give private state without `this`.
       2. **`class` without inheritance:** short for a constructor function plus
          methods on its prototype; no `extends`, no `super` (the expensive
          part), no static members, no getters. Decided 2026-11-22 after writing
          the examples:

          - **No `static`** for now: functions and constants in the module do
            the same (the module is the namespace); can be added later without
            breaking anything.
          - **No class header** `class Add(left, right) { … }` (tried in the
            examples, dropped): it saves about four lines per small data class,
            but the `constructor` form is plain Javascript, needs no rule
            "header or constructor" and nothing new to learn.
          - **No field declarations with an initial value** (`open = new Map();`
            in the class body): without a header the constructor is the one
            place where fields get their values.
          - **No value semantics** for data classes (`==` by value, objects as
            keys of `Map` / `Set`, frozen fields): `==` would mean two things,
            and node would need its own `Map` / `Set`; keys stay strings.

          Decided 2026-11-22 for `class` and `interface` (also step 3):

          - **Only at the top level** of a module (not in blocks or functions).
          - **Usable from the declaration on**, as in Javascript: methods and
            functions may mention a later class (they run later), but
            `new Num(1)` at the top level before `class Num` is an error (from
            the validator where it is clear, otherwise at runtime: "Num is used
            before its declaration (line …)"). Interfaces are declarations only:
            for `implements` the order does not matter.
          - **Modules:** `export class`, `export interface`;
            `import EventListener from "./events"` works for `implements` (the
            validator has to read the other file for it; the generator drops the
            interface from the import).
          - **An interface does not exist at runtime** (see step 3): its name is
            only allowed after `implements`; anywhere else it is a compile-time
            error ("EventListener is an interface: it can only be used after
            implements"). A class is a function (`typeof Num == "function"`).
          - **A class without `new`** (`Num(3)`) is a `TypeError`: "Num is a
            class: use new Num(…)".
          - **Validator:** duplicate methods in a class, `return` with a value
            in a constructor, missing interface methods or too many parameters
            (see step 3), a class, interface and a function / variable with the
            same name. Not checked: whether `this.x` exists (fields stay
            dynamic).
          - **Printing an instance** without `toString` shows the class name and
            the fields, as node does: `Num {value: 3}` (today objects with a
            prototype print as `{value: 3}`; check what changes).
          - Methods live on the prototype and are not enumerable (not in
            `Object.keys`, not printed); `this` in a method taken off its object
            (`const f = obj.m; f();`) is `undefined`, as in strict Javascript.
          - **Open: freeze the prototype of a class?** (and the class itself;
            the generator would add `Object.freeze(Num.prototype)`, a write is a
            `TypeError`). For it: everything a class can do is in its body, no
            methods added from elsewhere, no inheritance through the back door
            (`Dog.prototype = Object.create(…)`), a clear message at the write.
            Against: a class is then more than a short spelling of constructor
            function plus prototype, the generator no longer passes it through
            unchanged, one more difference to Javascript. Since interfaces are
            only checked statically (step 3), nothing at runtime relies on the
            methods of a class staying as declared. Tendency (2026-11-22): do
            not freeze; tinyscript is strict where Javascript has traps one
            falls into by accident, and nobody writes `Num.prototype.area = 5`
            by accident.

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
          bodies. Decided 2026-11-22: **a static declaration only, erased as in
          TypeScript.** The validator checks a class that says
          `implements Shape` before the script runs (a missing method or one
          with too many parameters is an error at the class, also in the Eclipse
          editor); the generator drops `interface` and `implements`, and nothing
          of them exists at runtime. A class can implement several interfaces
          (`implements EventListener, Reporting`). **Number of parameters**
          (decided 2026-11-22): a method may have fewer parameters than the
          interface says, not more (`onEvent()` implements `onEvent(event)`), as
          in TypeScript and like calls, where missing arguments are `undefined`
          and extra ones are ignored; more parameters are the mistake, since no
          caller passes them. A rest parameter (`...args`) is allowed and does
          not count. If two interfaces require the same method, the smaller
          number holds. Object literals and objects of constructor functions
          stay equal to instances of classes: nothing asks at runtime whether an
          object "is" a `Shape`; a function that wants to check its argument
          tests for the method (`typeof listener.onEvent == "function"`, as
          `orders.tys` does).

       - Dropped (2026-11-22, after a review):
         - **`x instanceof Shape` with an interface** (the former step 4;
           nominal interfaces at runtime): the examples hardly need it, an
           object literal with the right method would not be an `EventListener`
           (against the method shorthand and factory functions with closures),
           and it needs an interface as a value (`typeof`, `print`, `new`,
           `Symbol.hasInstance` in the generated Javascript).
         - **Class names after parameters**
           (`subscribe(listener: EventListener)`, the former step 5): looks like
           TypeScript and means something else (checked at runtime), `n: number`
           would not work, and return types, fields and `null` are the next
           questions.
       - Not pursued: structural interfaces as in Go (only checkable at
         runtime); a `std:` module with `extend` / `_new` helpers (a convention
         nobody knows); a library function that checks for methods at runtime
         (works with object literals, but loses the check before the run).

    7. Smaller: spread in calls (`Math.max(...values)`), renaming in imports
       (`import a as x from …`), assignment with a pattern (`[a, b] = [b, a];`).

    - Deliberately not gaps: `switch`, template strings, labels, `do … while`,
      regular expressions, `Date`. The messages for them are good, and the
      language stays small.
    - **Idea (2026-11-21): regular expressions without regex literals.** If
      ever, keep `/…/` out of the grammar and make regexes values of the
      library:
      1. ~~First, maybe enough for reading input: a `scanf`-like helper in
         `std:string`~~ (done 2026-11-22 as `parse` / `findAll`, with the fields
         of Python's `parse` library: `{}`, `{:d}`, `{:f}`, `{:g}`, `{:w}`,
         `{:l}`, `{:S}`; the draft below used other placeholders),
         `const [px, py, vx, vy] = scan("p={},{} v={},{}", line);` (readable
         without regex knowledge, no backtracking). Worked out:
         - Placeholders: `{}` an integer with an optional sign (a number), `{n}`
           a decimal number (`3.5`, `-1e3`; a number), `{w}` a word up to the
           next whitespace (a string), `{s}` any text up to the next literal
           part or the end (a string); `{{` / `}}` literal braces. Typed
           placeholders instead of guessing: no silent conversion.
         - `scan(pattern, text)` must cover the whole text (trailing whitespace
           allowed) and gives an array of the values, or `null` if the text does
           not fit, so formats can be tried: `scan(A, line) ?? scan(B, line)`.
           `scanAll(pattern, text)` gives all matches in a longer text:
           `for (const [a, b] : scanAll("mul({},{})", memory))` (AoC day 3).
         - A space in the pattern matches one or more spaces / tabs, other
           characters only themselves. Placeholders take from left to right as
           much as their type allows, `{s}` up to the first occurrence of the
           next literal part: no backtracking, linear time, safe in the sandbox.
           An unknown placeholder (`{x}`) is a `TypeError`.
         - AoC 2024 examples: `scan("{}|{}", "47|53")` (day 5),
           `scan("Button A: X+{}, Y+{}", line)` (day 13),
           `scan("{}: {s}", line)` then `integers(rest)` (day 7),
           `scan("{w} {w} {w} -> {w}", "x00 AND y00 -> z00")` (day 24).
         - Compared with `integers(text)`: that takes all numbers anywhere and
           checks no format; `scan` checks the structure and also gives words.
         - Implementation: in tinyscript itself, in `std:string.tys` like
           `words` and `integers`, so it runs the same in the interpreter and in
           node and is covered by differential tests; the pattern is split into
           parts once, then one pass over the text. About half a day with tests
           and docs.
         - Open: the name (`scan` or `parse`); `null` or an error for a text
           that does not fit (`null` favoured); `{}` only integers (favoured) or
           also decimals; groups in `scanAll` (`({},{})`; not at first).
      2. Real regexes: `new RegExp("…")` as in Javascript (or `std:regex`),
         which the generator passes through. Regex objects immutable, only
         stateless operations (`re.test(s)`, `re.match(s)`, `re.matchAll(s)` as
         an array; `split` / `replace` / `replaceAll` accept a regex) — no
         `lastIndex` / `g`-flag trap.
      3. Against `"\\d+"`: raw strings `r"(\d+)-(\d+)"` (a lexical change only;
         the generator writes a normal string). Not: keeping unknown escapes
         like `"\d"`, which would silently differ from Javascript.
      4. Java and Javascript regexes differ: accept only the common subset
         (reject e.g. possessive quantifiers, inline flags) so that differential
         tests hold.
      5. Sandbox: catastrophic backtracking (`(a+)+$`) cannot be interrupted in
         `java.util.regex`; wrap the text in a `CharSequence` whose `charAt`
         checks the limits of the `ResourceMonitor`.

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
    - ~~**Half truthiness.**~~ (decided and done 2026-11-22: option 1, plus a
      compile-time error for an operand of `&&` / `||` that can never be a
      condition, so `options || {}` says to use `??`.) In conditions `null` /
      `undefined` are false and objects are true (`if (node)`, `while (node)`),
      but `&&` and `||` always give a boolean. So the Javascript habit
      `const cfg = options || {};` silently yields `true`; numbers and strings
      are caught by a `TypeError`, objects are not. Options: (1) keep it and
      name the trap in the language reference (`??` is the way:
      `options ?? {}`), or (3) conditions accept only booleans, which is the
      most uniform but costs `while (node)` and `if (o.child)` (write
      `node != null`). Open, tendency (2026-11-20): 1 or 3. Not pursued: (2) a
      `TypeError` for an object as operand of `&&` / `||`.
  - **Suggested order:** ~~the bugs~~ (all fixed 2026-11-21; also `toFixed` /
    `toString(radix)`); ~~`undefined`~~ (done); ~~minimal `try` / `catch`~~
    (done); a ~~library batch~~ (done, also `range(…).reverse()` and `delete`);
    ~~method shorthand~~ (done), then `class` and `interface` (see 6.), a more
    comfortable REPL, standard input.

## Interpreter and code quality

- ~~**`JvmScriptEngineRunner` needs Graal.js**~~ (removed 2026-11-22). It ran
  the generated Javascript with GraalJS or Nashorn via JSR-223, had no users and
  no tests left. Decided: node stays the reference for the differential tests
  (GraalJS is a different implementation, e.g. of `Math`), and embedding in Java
  means `TinyscriptEngine` with its sandbox (a transpiled script would lose
  tinyscript's checks, its error locations and the resource limits). If speed
  matters, first make the interpreter faster (see Speed); an optional
  `tinyscript-graal` module ("fast mode without sandbox", only with the Graal
  JIT) would be a decision of its own, based on measurements.

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
