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

**Goal** (2026-11-24): code generation for node (`tinyscript --node`,
experimental) as a second backend of equal rank next to the interpreter; what
"equal" means is in `docs/architecture.md` §15. State: the generated code runs
in strict mode and checks builtins, operators and conditions as the interpreter
does; many mistakes are errors before the run, for both backends; errors show
the tinyscript line (source maps). 275 error tests require both backends to fail
alike; 8 still go on in node (`assertKnownGap`): 4 properties (`a.x = 5` on an
array and others, see phase 2.1), 4 builtins with too many arguments. Of the
messages, 161 differ (157 only in the text, 4 `TypeError` against `RangeError`;
`target/backend-differences.txt`). `BuiltinMatrixTest` calls every builtin with
edge values in both backends (3105 cases); 2 known differences are left
(`target/builtin-matrix.txt`). `BuiltinMembersTest` compares the members of the
builtins and typical values in both directions.

**Plan** (2026-11-24), in this order: phase 1, then 2, 3 and 5 (phase 2.1, the
depth of recursion, is done: a worker with a large stack); phase 4 is put back
for now. `--node` stops being experimental after phases 1 and 2 (no known gaps
left, or only documented ones that phase 3 finds before the run); phase 3 then
makes it stricter and faster.

1. **Phase 1: equality proven by tests.** Left: the members that only node has
   (decided 2026-11-25: known, not deleted by the prelude; `BuiltinMembersTest`
   lists them): Javascript's methods and constants that tinyscript does not
   have, such as `String#concat`, `match`, `substr`, `Array#concat`, `keys`,
   `values`, `toSorted`, `Array.from`, the `Set` operations (`union` …),
   `Number.isNaN`, `toPrecision`, `Math.cosh`, `Math.LN2`, `Object.freeze`,
   `Object.is`, `valueOf`, `f.bind`, and `e.stack`. A program that uses them
   runs only in node. Decide which ones tinyscript takes over; phase 3 finds the
   others before the run where the kind is known.
2. **Phase 2: the gaps in behavior closed.**
   1. **Properties in node, the rest:** reading a property of a number or
      boolean, `o[key] = x` on an array and any new property of a `Map` or `Set`
      fail in node as in the interpreter (2026-11-25, 0–3 % on the AoC days).
      Left (4 known gaps): `o.name = x` on an array (`a.x = 5`,
      `a.length = "2"`), since a check of every `o.name = x` costs up to 1.7
      times in loops that update fields (measured 2026-11-25); `delete n.x` of a
      number; `let { a } = 42`. With the kinds of phase 3, the check is needed
      only where `o` may be an array.
   2. **Too many arguments for a builtin** where the receiver is only known at
      run time (`s.startsWith("a", 0, 1)`, `a.map(f, {})`,
      `new Map().get(1, 2)`): a `TypeError` in the interpreter, ignored in node
      (4 known gaps). The checked builtins of the prelude do not check the
      maximum, since builtins as callbacks get more arguments (`.map(Number)`);
      a check at the call site of the generated code would tell the two apart.
   3. **`Math.exp` and the other functions of `StrictMath` to the last bit:**
      `Math.exp(1)` is `2.7182818284590455` in the interpreter, `Math.E` in node
      (a known difference of the builtin matrix). Port `exp` (and where needed
      `log`, `sin` …, `pow`) from FreeBSD's msun as `log2` and `cbrt`
      (`FreeBsdMath`), checked against node on 20,000 inputs.
3. **Phase 3: static checks, level 2 (kinds).**
   - A light local inference from literals, `new C(…)`, `range(…)`, the fixed
     result kinds of builtins (`split`, `map` → array, `length` → number) and
     `const` variables: `parts.lenght`, `range(n).filter`, `s.push` on a string,
     `new P().lenth()` on a known class (methods of a class only in its body, so
     this is sound; fields stay dynamic), `new` of a non-function in a `const`;
     errors before the run, for both backends.
   - The generator uses the kinds to leave out more checks (comparisons of
     properties, callbacks that return a comparison: day09 of 2024 takes 1.8
     times the unchecked time today).
   - Literal arguments of the wrong type for builtins other than `Math`
     (`"a".repeat("2")`, `parseInt(42)`): the types are only in the
     implementations, so they need to be declared with `defineBuiltinMethod`.
   - Decide on the way: should the interpreter reject reading an unknown
     property of an array, string, `Map`, `Set` or range (today `undefined`)?
4. **Phase 4 (put back for now): messages as in the interpreter.** The
   interpreter names the expression
   (`'*' needs two numbers, but r.height is undefined`,
   `Cannot read 'c' of o.b: it is undefined`), node does not
   (`not number and undefined`, `Cannot read properties of undefined`). Idea:
   each check site gets a number, and a table of the expressions' texts is read
   only when an error is reported, so it costs nothing on the normal path; align
   the most frequent texts of the 84 differences. Not needed for "equal" (the
   same behavior, not the same diagnostics).
5. **Phase 5: official.** `--node` without the note "experimental"; README and
   language reference describe the second backend; CI runs the example programs
   once through the command line with `--node` (a smoke test) and the
   differential tests with node 20, 22 and 24 (locally node 26; 20 is the
   minimum); then a release.
6. **Warnings, later:** unused variables, parameters and imports; `let` never
   assigned again; fits the language server.

Stays with the interpreter by design: the sandbox (resource limits; generated
code never runs foreign scripts on a server), embedding in Java with objects of
the host, and the interactive REPL. Limits: parameters, fields, return values of
the script's own functions and `JSON.parse` data have no static type; there only
the checks of the generated code at run time help. The price of a second
backend: every language feature, builtin and native module is written and tested
twice.

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
