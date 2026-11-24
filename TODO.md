# TODO

Open work for tinyscript, and the big points decided against. Each item is meant
to become its own PR, with tests first. What is done and decided is described in
the [README](README.md#design-decisions) (the language),
[docs/architecture.md](docs/architecture.md) (the implementation) and
[docs/language-reference.md](docs/language-reference.md).

**Goal: tinyscript is fit for Advent of Code and similar tasks** (decided
2026-11-20). It stays deliberately stricter than Javascript; every addition is
Javascript-compatible where it exists there, so the generator passes it through
and differential tests run against node. The priorities come from solving all of
AoC 2024, ten days each of 2023 and 2022 and porting 15 days of 2021 from
TypeScript (`examples/aoc20xx`).

## Language

1. **Template strings.** `"" + x` occurs 18 times in the examples, e.g.
   `("" + (i + 1)).padStart(4)`; the most everyday addition, and
   Javascript-compatible. The lexer for `${…}` is real work in Xtext.
2. **Smaller:** assignment with a pattern (`[a, b] = [b, a];`), renaming in
   imports (`import a as x from …`), computed keys (`{ [k]: 1 }`), `Array.from`.
3. **Library:** a priority queue in a `std:` module (days 16 of 2024 and 17 of
   2023 have a binary heap of their own); `findAll` with the positions of the
   matches; a single-character field in `parse` (`a<2006`); possibly `readAll()`
   in `std:io`, `toPrecision` / `toExponential`; `JSON.stringify` (as far as it
   fits tinyscript's values: `undefined` and functions, `Map` / `Set`, cycles,
   indentation).
4. **Regular expressions as a library**, if ever (`parse` / `findAll` of
   `std:string` cover reading input so far): `new RegExp("…")` as in Javascript,
   no `/…/` literals; immutable, only stateless operations (`test`, `match`,
   `matchAll` as an array; `split` / `replace` accept a regex), no `lastIndex`;
   only the subset common to Java and Javascript, so differential tests hold;
   raw strings `r"(\d+)"` against `"\\d+"`; in the sandbox, catastrophic
   backtracking must stay interruptible (a `CharSequence` whose `charAt` checks
   the `ResourceMonitor`).
5. **Possibly later:** `finally`; error classes with `instanceof` (`TypeError`,
   `IOError`); patterns in `catch ({ message })`.

## Checking scripts, and code generation as a second backend

**Goal** (2026-11-24): keep the option open to run scripts by generating
Javascript for node, as a second backend of equal rank next to the interpreter.
Today the generator mainly keeps the interpreter honest (differential tests); as
a backend it must keep tinyscript's semantics, errors included. A probe of 41
typical mistakes: 1 was found before the run, 38 only by the interpreter or not
at all; the generated code checks hardly any of them (`"a" - 1` is `NaN`,
`if (1)` runs, `[10, 9, 1].sort()` sorts as text). Modules (not found, cyclic,
missing exports, invalid imported files) are checked the same way in both.

1. **Fixes first (small PR):** emit `"use strict"` (today `this` in a plain call
   is `globalThis` in node but `undefined` in tinyscript, and `Math.PI = 3` is
   ignored silently); members of builtin objects read-only in both
   (`Math.PI = 3` works in the interpreter); `new` of an arrow function an error
   in the interpreter too (node: `TypeError`).
2. **Same errors, tested:** `assertSameError(code)`: both backends fail with the
   same error class and message. Move the error tests
   (`assertInterpreterThrows`, about 100 places that throw a `TypeError`) to it
   step by step; the number of cases that still differ measures the gap.
3. **Static checks, level 1 (sound, no type inference):**
   - builtins (`Math`, `JSON`, `Number`, `parseInt` …): unknown members with a
     suggestion (`Math.sqr`, `Math.PIE`, which gives `undefined` today), the
     number of arguments in both directions (too few is a
     `TinyscriptArgumentError` with a weak message today), literal arguments of
     the wrong type; the signatures come from the builtin objects of the runtime
     (`defineBuiltinMethod` knows minimum and maximum), not a second list;
   - operators and conditions on literals or obvious types (`"a" - 1`,
     `[1] + 1`, `1 < "2"`, `if (1)`, `if (x = 2)`, `x == NaN`,
     `typeof x == "numbr"`), generalizing the check for `options || {}`;
   - calls and structure: a known class without `new`, `new` of an arrow
     function or a non-function, `throw` of anything but `new Error(…)`,
     duplicate keys in object literals, code after `return` / `break` /
     `continue` / `throw`, too short array literals in patterns.
4. **Checked code in the generator** where types are not proven: `__ts_` helpers
   for `-`, `*`, `<`, conditions, `&&` / `||` (as `__ts_add` and `__ts_eq`
   already do); builtins replaced in the prelude to check as the interpreter
   does (argument counts, integers for `substring` / `at` / `range`, `sort()`
   without comparison only for strings, no properties on arrays). Costs speed in
   node; measured against item 2.
5. **Static checks, level 2 (kinds):** a light local inference from literals,
   `new C(…)`, `range(…)`, the fixed result kinds of builtins (`split`, `map` →
   array, `length` → number) and `const` variables: `parts.lenght`,
   `range(n).filter`, `s.push` on a string, `new P().lenth()` on a known class
   (methods of a class only in its body, so this is sound; fields stay dynamic).
   The interpreter should then also reject reading an unknown property of an
   array, string, `Map`, `Set` or range (today `undefined`). The generator uses
   the kinds to leave out the checks of item 4.
6. **Warnings, later:** unused variables, parameters and imports; `let` never
   assigned again; fits the language server.

Stays with the interpreter by design: the sandbox (resource limits) and the
exact error locations (source maps later, if needed). Limits: parameters,
fields, return values of the script's own functions and `JSON.parse` data have
no static type; there only the runtime checks of item 4 help.

## Speed

The tree-walking interpreter is fast enough for AoC: most days of 2023 and 2024
take a few seconds on real inputs (2024 day 6 part 2, a simulation of 20–30
million steps, about a minute), but 50–290 times slower than node, most for code
that calls small functions in its inner loops (AoC 2021 day 19: 53 s, day 20: 45
s). Profile (JFR, 2026-11-20): dispatch 34–59 %, variable lookup 5–15 %,
creating contexts (a `HashMap` per block / loop iteration) 6–16 %, objects /
properties 6–14 %, operators 5–13 %. A call of a builtin method such as `push`
costs about 1.2 µs, ten times an index access; a call of the script's own
functions about 0.35 µs.

- **If a program is too slow:** first lighter contexts that stay name-based (a
  small array of names and values with linear search instead of a `HashMap`;
  expected 5–15 %, only `ExecutionContext` changes), then the cost of builtin
  method calls.
- **`AdventOfCode2021Test` takes about 26 s** (on the examples), most of the
  time of the standalone tests; find out which days dominate.

## Tools, build and operations

- **Web demo:** mark the place of an error in the editor (line and column are
  known); a CPU time limit (`maxMXCpuTime`) as a second safety net besides the
  statement limit.
- **IDE:** the validator could check while typing that an imported file exists
  and exports the names (fits the language server).
- **VS Code, stage 2: a language server** (about 1–2 days): validation while
  typing, completion, go to definition, references, rename, outline come from
  Xtext. A fat jar from core + `de.mkbauer.tinyscript.ide` with
  `org.eclipse.xtext.ide.server.ServerLauncher` as main class (a plain Maven
  module like the REPL; needs `xtext.ide` and `lsp4j`); a small TypeScript
  extension starts it via `vscode-languageclient`. CI: build the jar, a smoke
  test that validates a file with an error.
- **VS Code:** `package.json` overrides `minimatch` for `@vscode/vsce` with
  `^11.0.1` (Dependabot alert #57); remove it once a vsce release depends on
  minimatch 11.
- **Re-test Maven 3.10+ with Tycho 5.0.4.** The wrapper pins Maven 3.9.16
  because 3.10 failed with Tycho 5.0.2.

## Decided against

- **Variable slots, compiling the tree to closures, a bytecode compiler**
  (2026-11-20): an engine that looks names up in a chain of contexts, as scoping
  is explained, is worth more than the expected 15–25 %; slots add an analysis
  pass and a class of silent errors. Compiling only as a deliberate architecture
  decision, based on measurements.
- **Running the generated Javascript in the JVM** (GraalJS / Nashorn, removed
  2026-11-22): node is the reference of the differential tests, and embedding
  means `TinyscriptEngine` with its sandbox and error locations. Generating code
  for node as a second backend is a different matter (see above).
- **Inheritance between classes** (`extends`, `super`): code is shared by
  composition; objects with the same methods are used alike
  (`examples/oo/expressions.tys`).
- **More class syntax** (2026-11-22): no `static` (the module is the namespace),
  no getters, no class header `class Add(left, right)`, no field declarations
  (the constructor is the one place where fields get their values;
  `constructor(x = 0)` covers default values), no value semantics (`==` would
  mean two things), no frozen prototypes.
- **Spread and rest for objects** (`{ ...o }`, `const { a, ...others } = o;`;
  2026-11-23, when spread in calls and array literals and rest elements in array
  patterns came): `{ ...point }` is no longer a `Point`, and which properties
  are copied is a question of its own; `Object.assign({}, o)` does it, and AoC
  never needed it.
- **Checking the number of arguments at run time** (2026-11-23, when the
  validator started to reject too many arguments for known functions and
  classes): callbacks often get more arguments than they use (`map` passes
  `(item, index, array)`, and the script's own higher-order functions do the
  same), and nothing tells a callback call from a direct one. Methods stay
  unchecked; assigning to a method on an object (`p.len = 5`) stays allowed.
- **Statements and operators:** `switch`, labels, `do … while`, `**` (`Math.pow`
  and `1 << n` do it, and it brings its own precedence rules). The messages say
  what to write instead.
- **Conversions:** a user-defined `valueOf` (implicit conversion in operators),
  `toLocaleString`, `printf`-like format strings.
- **Conditions accept only booleans** (2026-11-22): would cost `while (node)`
  and `if (o.child)`; instead `options || {}` is a compile-time error that
  points to `??`.
- **`from` and `as` as names** (2026-11-22): they stay reserved; the message
  says why and suggests other names.
- **`Date`**: `System.currentTimeMillis()` is enough.
- **A protected call as in Lua** (`attempt(() => …)` with `r.ok`, `r.value`)
  instead of `try` / `catch`.
