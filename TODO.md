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
AoC 2024, ten days of 2023 and porting 15 days of 2021 from TypeScript
(`examples/aoc20xx`).

## Language

1. **Spread in calls and rest elements in patterns** (`Math.max(...values)`,
   `push(...xs)`, `const [first, ...rest] = items;`): missed in all three AoC
   rounds (2021: 4 times each). Deferred 2026-11-21 because
   `f.apply(null, values)`, `min` / `max` of `std:array` and `items.slice(1)` do
   it; to decide. The messages already say what to write instead.
2. **Template strings.** `"" + x` occurs 18 times in the examples, e.g.
   `("" + (i + 1)).padStart(4)`; the most everyday addition, and
   Javascript-compatible. The lexer for `${…}` is real work in Xtext.
3. **Strictness for the script's own code.** Builtins check type and number of
   their arguments, the script's own classes and functions nothing:
   `new Point(1, 2, 3)` runs, and `this.m = 1` in a constructor silently hides
   the method `m`. Classes and function declarations are known before the run,
   so the validator could cheaply reject too many arguments for `new Class(…)`
   and calls of declared functions, and a field set in a constructor with the
   name of a method. Not intended: too few arguments (they are `undefined`).
4. **Smaller:** assignment with a pattern (`[a, b] = [b, a];`), renaming in
   imports (`import a as x from …`), computed keys and object spread
   (`{ [k]: 1 }`, `{ ...o }`), `Array.from`.
5. **Library:** a priority queue in a `std:` module (days 16 of 2024 and 17 of
   2023 have a binary heap of their own); `findAll` with the positions of the
   matches; a single-character field in `parse` (`a<2006`); possibly `readAll()`
   in `std:io`, `toPrecision` / `toExponential`.
6. **Regular expressions as a library**, if ever (`parse` / `findAll` of
   `std:string` cover reading input so far): `new RegExp("…")` as in Javascript,
   no `/…/` literals; immutable, only stateless operations (`test`, `match`,
   `matchAll` as an array; `split` / `replace` accept a regex), no `lastIndex`;
   only the subset common to Java and Javascript, so differential tests hold;
   raw strings `r"(\d+)"` against `"\\d+"`; in the sandbox, catastrophic
   backtracking must stay interruptible (a `CharSequence` whose `charAt` checks
   the `ResourceMonitor`).
7. **Possibly later:** `finally`; error classes with `instanceof` (`TypeError`,
   `IOError`); patterns in `catch ({ message })`.

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
  means `TinyscriptEngine` with its sandbox and error locations.
- **Inheritance between classes** (`extends`, `super`): code is shared by
  composition; objects with the same methods are used alike
  (`examples/oo/expressions.tys`).
- **More class syntax** (2026-11-22): no `static` (the module is the namespace),
  no getters, no class header `class Add(left, right)`, no field declarations
  (the constructor is the one place where fields get their values;
  `constructor(x = 0)` covers default values), no value semantics (`==` would
  mean two things), no frozen prototypes.
- **`interface` / `implements`: parked** (2026-11-22; reserved words with a
  hint). Without types at the places of use and without validation across files
  an interface only checks that a class of the same file has all methods, and a
  missing method is reported when it is called. Look at it again if checking
  across files exists (the language server). Dropped: `instanceof` with
  interfaces, class names after parameters (`f(l: Listener)`).
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
