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

**What "equal" means** (2026-11-24): the same behavior, not the same
diagnostics. A program without errors prints the same. A program with an error
fails in the same phase (before the run: syntax, validator, modules, the same
front end in both; or while it runs), at the same point (the output up to the
error is the same) and in the same category for `catch` (caught: errors thrown
with `throw`, `IOError`, the `SyntaxError` of `JSON.parse`; not caught:
programming errors, failed `assert`s). So a `TypeError` of the interpreter and a
`RangeError` of node (`"x".repeat(-1)`) are equal: neither is caught. The gap is
a program that fails in one backend and goes on in the other (`"a" - 1` is `NaN`
in node). Error class, message and line are goals for good messages, not part of
equality: the same message only where tinyscript throws the error itself (V8's
own, such as a property of `undefined`, a call of a non-function or a stack
overflow, keep their text: wrapping every access and call would cost the speed
that is the reason for node), the line once there are source maps (item 5). Not
equal by design: the sandbox, the depth of recursion, `Math.random`, time.

1. **Same values, tested:** the AoC programs cover only what works.
   - a matrix of every builtin with edge values (`NaN`, `-0`, `""`, negative,
     not an integer, too few arguments), generated from the builtin objects of
     the runtime (`defineBuiltinMethod`): both backends give the same value or
     the same error;
   - the members of the builtin objects and prototypes compared in both
     directions: node knows more than tinyscript (`flatMap`, `Object.entries`,
     `f.constructor`, `o.__proto__`), and such a program runs only there; the
     prelude deletes what tinyscript does not have;
   - checked 2026-11-24 and equal: the order of keys that are integers, `$1` /
     `$&` in the replacement of `replace`, gaps in arrays (`a[10] = 1`,
     `a.length = …`; printed as `undefined`), `NaN` and `-0` as keys of a `Map`,
     `trim` and `toUpperCase` / `toLowerCase` with Unicode; `includes` with
     `NaN` and `new Number(5)` (fixed 2026-11-24).
2. **Static checks, level 1 (sound, no type inference):**
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
3. **Checked code in the generator** where types are not proven. The gap,
   measured with `assertKnownGap` (each closed gap turns into
   `assertSameFailure`) and item 1: of 174 error tests, 126 fail the same in
   node and 48 go on there (2026-11-24). Done: the builtins are replaced in the
   prelude and check their arguments as the interpreter does (types, integers,
   the minimum number); in a microbenchmark of hot builtins 1.35 times slower,
   on AoC days not measurable. Open: properties of primitives and arrays about
   17 (`n.x`, `a.x = 5`, `a[-1] = 5`), operators about 16 (`1 - "a"`,
   `x += true`, `s++`, `1 < "a"`), conditions about 13 (`if (1)`, `!0`,
   callbacks of `filter` / `find` that return numbers), too many arguments of a
   builtin 2 (`(1).toFixed(1, 2)`; a check at the call, not in the builtin,
   since callbacks get more arguments: item 2).
   - **Operators and conditions, measured 2026-11-24** with the checks generated
     by an experimental switch (34 of the 48 gaps closed, all tests and AoC
     programs unchanged), AoC days with full input, node's start (100 ms)
     subtracted: helpers for every `-` `*` `/` `%` `<` `<=` `>` `>=`, unary `-`,
     `++` / `--`, the compound assignments, `+` of numbers and every condition
     (`if`, `while`, `?:`, `!`, `&&` / `||`, `assert`, the callbacks of `filter`
     / `find` / `some` / `every`) cost 1.05 to 2.25 times the time in node;
     **smart** (no check where the operator already guarantees the kind: a
     comparison, `==`, `!`, `&&` / `||` is a boolean, `-` `*` `/` `%`, unary `-`
     and number literals are numbers; no type inference, variables, properties
     and calls stay checked) 0.99 to 1.9 times (day09 of 2024, with comparisons
     of properties and a `find` callback, 88 → 167 ms; day20 211 → 290 ms; most
     days 1.0 to 1.3). The interpreter stays 4 to 57 times slower than node with
     checks, so they cost a small part of the factor: equal rank is possible.
     Writing the check inline
     (`typeof a === "number" && typeof b === "number" ? a - b : __ts_sub(a, b)`)
     gains nothing measurable over the helpers (V8 inlines them). Next: smart,
     with the helpers; item 4 removes more checks.
   - Not possible at a reasonable price in node: no properties on arrays, an
     error for reading an unknown property (it would need a `Proxy` or a helper
     for every access). These stay with the static checks of item 4.
4. **Static checks, level 2 (kinds):** a light local inference from literals,
   `new C(…)`, `range(…)`, the fixed result kinds of builtins (`split`, `map` →
   array, `length` → number) and `const` variables: `parts.lenght`,
   `range(n).filter`, `s.push` on a string, `new P().lenth()` on a known class
   (methods of a class only in its body, so this is sound; fields stay dynamic).
   The interpreter should then also reject reading an unknown property of an
   array, string, `Map`, `Set` or range (today `undefined`). The generator uses
   the kinds to leave out the checks of item 3.
5. **Error locations in node (source maps):** today an error in the generated
   code points into the generated bundle, not to the `.tys` file. A source map
   (v3, inline as `//# sourceMappingURL=data:…`) maps the generated positions to
   the tinyscript file, line, column and name, and node uses it with
   `process.setSourceMapsEnabled(true)` / `--enable-source-maps`. A handler in
   the prelude (`uncaughtException`) then prints errors as the interpreter does
   (class, message, the line with `^`, the stack without the `__ts_` helpers).
   Side effect: debugging the generated program with breakpoints in the `.tys`
   file. Two ways, to be decided:

   - **Xtext's tracing** (`@Traced`, `TracingSugar`, `CompositeGeneratorNode`):
     each generated piece knows its AST element; clean, but a larger rebuild of
     the templates;
   - **markers:** the generator writes `/*@ts:main.tys:70:5*/` before each
     statement and call, a pass afterwards turns them into the map (Base64 VLQ)
     and removes them; little change to the generator, about a day, as exact as
     the markers are placed.

   Either way: the line with `^` needs the source text (`sourcesContent`); the
   names the generator changed belong into `names`; an error inside a `std:`
   module is reported at the caller, as in the interpreter; with source maps
   enabled, errors that are thrown and caught often get slow (measure).

6. **Warnings, later:** unused variables, parameters and imports; `let` never
   assigned again; fits the language server.

Stays with the interpreter by design: the sandbox (resource limits; generated
code never runs foreign scripts on a server), embedding in Java with objects of
the host, the interactive REPL, and the exact error locations (source maps
later, if needed). Limits: parameters, fields, return values of the script's own
functions and `JSON.parse` data have no static type; there only the runtime
checks of item 3 help. The price of a second backend: every language feature,
builtin and native module is written and tested twice.

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
