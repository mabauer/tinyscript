# tinyscript -- a simple Javascript like language

This project is about the implementation of a simple Javascript like language,
named _tinyscript_.

## What is tinyscript?

_tinyscript_ is a small Javascript like language. Here is a piece of
_tinyscript_ code:

    // Compute fibonacci numbers

    function fibonacci(n) {
    	let h = 1, g = 1, f = 1;
    	for (const i : range(2, n + 1)) {
    		h = g;
    		g = f;
    		f = g + h;
    	}
    	return f;
    }

    print(fibonacci(7));  // 21
    print(fibonacci(50)); // 20365011074

You can play with it online in your browser
[here](https://tinyscript.mkbauer.de). The
[language reference](docs/language-reference.md) describes the whole language,
its built-in objects and its differences from Javascript.

_tinyscript_ offers quite a complete set of interesting language features:

- [Basic expressions](de.mkbauer.tinyscript.webdemo/frontend/public/expressions.tys)

- [Handling of strings](de.mkbauer.tinyscript.webdemo/frontend/public/strings.tys)

- [Control-flow structures](de.mkbauer.tinyscript.webdemo/frontend/public/primes.tys):
  `for`, `while` and `if` statements, `break`

- [Loops](de.mkbauer.tinyscript.webdemo/frontend/public/loops.tys) over arrays,
  strings, `Map`s and `Set`s, and counting with lazy ranges:
  `for (const i : range(a.length).reverse())`

- [Functions](de.mkbauer.tinyscript.webdemo/frontend/public/fibonacci.tys),
  including
  [recursion](de.mkbauer.tinyscript.webdemo/frontend/public/fibonacci_recursive.tys)
  and _arrow_ functions

- [Arrays](de.mkbauer.tinyscript.webdemo/frontend/public/arrays.tys)

- [Closures](de.mkbauer.tinyscript.webdemo/frontend/public/closures.tys)

- [Objects and Classes](de.mkbauer.tinyscript.webdemo/frontend/public/oo.tys)

- [Modules](de.mkbauer.tinyscript.webdemo/frontend/public/modules.tys):
  `import area, unit from "./geometry";`, and a small standard library
  (`import sum, reverse from "std:array";`); the web demo can import only the
  standard library

Larger examples: [Advent of Code 2024](examples/aoc2024), solutions of all 25
days, and [ten days of 2023](examples/aoc2023), written with classes; all meant
to be read as examples of tinyscript. [Fifteen days of 2021](examples/aoc2021)
are ported as literally as possible from TypeScript, to show how code written
with Javascript habits looks in tinyscript.

Around Christmas time 2015, I've designed _tinyscript_ and implemented an
interpreter for it in Java for a couple of reasons:

- I believe every software engineer should at some point design and implement
  his own language just for the fun of it. I wanted to go a bit further than the
  usual toy examples taught in compiler construction classes, so I came up with
  _tinyscript_.

- At that time in my office we were looking into embedding a scripting language
  into our SaaS business software offering. We were looking for a way to allow
  for customer / tenant specific business logic that could be run on both our
  backend servers (mainly written in Java) and in client apps (e.g. written in
  C# and Xamarin). For such a use case, some kind of sandboxing and resource
  monitoring is necessary to avoid that ill-behaving, tenant specific code can
  harm other tenants or the SaaS platform as a whole. For that purpose we needed
  a test "vehicle".

- I wanted to have a look at the
  [Xtext framework](https://www.eclipse.org/Xtext/) for language engineering. So
  I used tinyscript as an example project to get my hands dirty with that.

In 2026, more than ten years later, I took the project up again. I used
[Claude Code](https://claude.com/claude-code) to modernize the project and to
add a few language features, among other things modules and classes (including a
handy way to define methods). We also extended the standard library, so that
programming puzzles like [Advent of Code](https://adventofcode.com) can be
solved with _tinyscript_ (see [`examples`](examples)).

## What did I do?

- Definition of _tinyscript_'s grammar. Implementation of the corresponding
  parser including an _abstract syntax tree (AST)_ using _Xtext_ (project
  [`de.mkbauer.tinyscript`](de.mkbauer.tinyscript), package `tinyscript`). A
  validator checks scripts before they run, and syntax errors are explained in
  plain words, with a hint for constructs from Javascript that tinyscript writes
  differently (`var`, `===`, a C-style `for` loop, …).

- Design and implementation of a „high-level“ interpreter in Java (project
  [`de.mkbauer.tinyscript`](de.mkbauer.tinyscript), package
  `tinyscript.interpreter`). Specifically have a look at class
  `TinyscriptEngine` and _tinyscript_'s runtime (package `tinyscript.runtime`).
  The interpreter is also the way to embed _tinyscript_ in a Java application,
  optionally sandboxed with resource limits (see [Sandboxing](#sandboxing)).

- Modules and a small standard library: `std:array`, `std:string`, file access
  with `std:fs` and the standard input with `std:io`
  (`for (const line : lines())`).

- A command line tool (project
  [`de.mkbauer.tinyscript.repl`](de.mkbauer.tinyscript.repl)) that runs scripts
  or starts an interactive session (see [Setup](#setup)).

- Integration of _tinyscript_'s interpreter into a demo web app (project
  [`de.mkbauer.tinyscript.webdemo`](de.mkbauer.tinyscript.webdemo)) based on
  _Spring Boot_ and _Vue_, also available as a Docker image. That's the same
  [web app](https://tinyscript.mkbauer.de) I've mentioned above when introducing
  _tinyscript_'s language features.

- With _Xtext_ it's easy to provide an editor plugin for _Eclipse_ (project
  [`de.mkbauer.tinyscript.ui`](de.mkbauer.tinyscript.ui)). For _Visual Studio
  Code_ there is a small extension with syntax highlighting
  ([`de.mkbauer.tinyscript.vscode`](de.mkbauer.tinyscript.vscode), see its
  README for how to install it).

- A code generator that translates _tinyscript_'s AST to Javascript (package
  `tinyscript.generator`). Today it keeps the interpreter honest: the tests run
  each language feature, every example of the web demo and the
  [Advent of Code solutions](examples/aoc2024) both in the interpreter and,
  translated, in _node.js_, and compare their output. Originally, the generated
  code ran in the _Nashorn_ engine of Java 8 and in a native iOS app (a
  proof-of-concept in [`TinyscriptiOSDemo`](TinyscriptiOSDemo) and branch
  `iosdemo`).

## Design decisions

_tinyscript_ is not meant to be a Javascript clone. It deliberately differs from
Javascript wherever I found Javascript's behavior surprising or wanted to try
something different. For example, the concept of _equality_ and the conversion
of primitive datatypes in Javascript is a mine-field on its own.

- **File extension `.tys`:** tinyscript files end in `.tys` (not `.ts`, which
  belongs to TypeScript).
- **Statements:** the rules for ending statements with `;` are roughly the same
  as in Java.
- **Scoping:** blocks create real _scopes_; variables (`let`, and `const` for
  those never assigned again) are block-scoped and there is no hoisting. There
  is no `var`: it is a reserved word, and the compiler says to use `let`. Like
  Javascript in _strict mode_, all variables have to be declared before they can
  be used. This includes `typeof`: `typeof undeclared` is a compile-time error,
  not `"undefined"`.
- **Blocks are mandatory:** control structures always need blocks:
  `if (cond) { stmts; } else if (cond) { stmts; } else { stmts; }`
- **Loops:** `for` goes through the _values_ of an array
  (`for (const x : arr) { … }`), the characters of a string, the entries of a
  `Map` or `Set`, or the integers of a lazy range: `for (const i : range(n))`
  counts from 0 to n - 1, `range(1, 11)` from 1 to 10, and
  `for (const i : range(a.length).reverse())` goes backwards over the indexes
  (ranges as in Python). There is no counting `for (let i = 0; i < n; i++)`. For
  anything else there is `while (cond) { … }`. `break` leaves the innermost
  loop, `continue` goes on with its next iteration. `i++;` and `i--;` are
  statements, not expressions (no `x = i++`, no `++i`).
- **Equality without coercion:** there are only `==` and `!=`, and they never
  convert types (`1 == "1"` is `false`). The one deliberate exception:
  `null == undefined` is `true`, so `o.x == null` checks for a missing property.
  Numbers compare like in Javascript (`NaN != NaN`). There is no `===` / `!==`.
- **Strict comparisons:** `<`, `<=`, `>` and `>=` compare a number with a number
  or a string with a string; anything else is a `TypeError`. Comparisons cannot
  be chained: `a < b < c` is a syntax error (in Javascript `1 < 3 < 2` is
  `true`).
- **Conditions are booleans:** `if`, `while`, `!`, `&&`, `||` accept a boolean,
  `null` / `undefined` (false) or an object (true). Numbers and strings are a
  `TypeError`, since `0`, `""` and `NaN` being false is a classic trap: write
  `count > 0`. `&&` and `||` short-circuit and return `true` or `false`; for a
  default value use `??` (`counts.get(key) ?? 0`), for null checks in a chain
  `?.` (`node.next?.value`). The Javascript idiom `options || {}` (always `true`
  here) is a compile-time error that points to `??`.
- **No silent conversions:** operators and builtins do not convert their
  operands; `1 + true`, `1 - "a"`, `Math.sqrt("4")` are `TypeError`s instead of
  `NaN`. Convert explicitly with `Number(s)`, `parseInt(s)` or `String(x)`;
  unlike Javascript, `Number("")` is `NaN`, not `0`. `valueOf` is not supported.
  Kept on purpose: missing arguments of your own functions are `undefined`, and
  reading a missing property gives `undefined`. Too many arguments are an error
  before the run where the called function is known (`f(1, 2, 3)` for a declared
  `f(a, b)`, `new Point(…)`); callbacks and methods ignore extra arguments, as
  in Javascript.
- **Globals vs. modules:** what is a standard global in Javascript and needs no
  privileges stays global (`Math`, `Object`, `print`, …); modules are for
  additions Javascript does not have (`std:array`) and for privileged things
  (`std:fs`, `std:io`). Each module runs once; imported names are read-only
  snapshots (simpler than Javascript's live bindings), and files share names
  only via imports.
- **File access is a module:** there is no global `fs`; use
  `import * as fs from "std:fs";` (not available in the sandboxed web demo).
- **Few methods on primitives:** strings have their methods (`length`, `charAt`,
  `at`, ...); numbers only `toFixed` and `toString(radix)`, for formatting;
  booleans none. Any other property access on a number or boolean (`n.length`,
  `flag.x`) is a `TypeError`, so mixing up a number and an array is noticed at
  once.
- **Arrays are lists:** besides their items only `length` can be set; `a.x = 1`
  or `a[-1] = 1` is a `TypeError` instead of silently creating a property. To
  index from the end, use `a.at(-1)`.
- **Arrays can be added:** the operator `+` also works on arrays:
  `let arr = ["hello"]; arr = arr + ["world"];` results in `["hello", "world"]`.
  With a string on either side, `+` concatenates strings (`"arr: " + arr`); an
  array plus anything else is a `TypeError`.
- **Catching errors:** `try { … } catch (e) { … }` catches only what a script
  can expect: errors thrown with `throw` (only `Error` objects can be thrown)
  and failed file access (`e.name == "IOError"`). Programming errors such as a
  `TypeError` or a failed `assert` end the script even inside `try`, so a broad
  `catch` cannot hide them. There is no `finally`.
- **Functions** are declarations, function expressions and arrow functions
  (`x => x * 2`), with rest parameters (`...args`) and default parameters
  (`step = 1`, used for a missing argument or `undefined`). Stricter than
  Javascript, defaults come last and only names have them, not patterns. Spread
  (`Math.max(...values)`, `[...a, x]`) takes what the `for` loop goes through;
  there is no spread in object literals.
- **Destructuring** takes apart arrays and objects in declarations, `for` loops
  and parameters (`const [a, b] = pair;`, `({ x, y }) => x * y`), but only flat
  patterns of names; an array pattern can end with a rest element
  (`[first, ...rest]`). An array that is too short is a `TypeError` instead of
  giving `undefined`.
- **Commas:** a comma after the last element is allowed in array and object
  literals (`[1, 2,]`), not in calls, parameter lists or patterns; arrays have
  no empty elements (`[1, , 2]`).
- **Objects: literals, factory functions and classes, without inheritance.**
  `class Point { constructor(x, y) { … } … }` is short for a constructor
  function with methods on its prototype, as in Javascript; a class can only be
  created with `new`. There is no `extends`, no `static` and no getters: code is
  shared by composition or in functions, and objects with the same methods can
  be used alike. Object literals have the method shorthand `{ area() { … } }`.
  See [Classes and objects](docs/language-reference.md#9-classes-and-objects).
- **Sandboxing:** in sandboxed mode every loop iteration counts against the
  statement limit, so even endless loops cannot exceed the resource limits (see
  [Sandboxing](#sandboxing)).

## Limitations

Certainly, _tinyscript_ is not perfect. It's just a proof-of-concept (but a
usable one!). Some Javascript features are simply missing, for example:

- There is no support for _regular expressions_; to read structured text,
  `std:string` has `parse` (`parse("{:d},{:d}", line)`), `findAll`, `words` and
  `integers`.
- There is no `switch` and no template strings, and classes have no inheritance.

See [TODO.md](TODO.md) for what's planned and what was decided against.

## Performance

_tinyscript_'s interpreter walks the syntax tree directly, so it is slow
compared with the highly-optimized Javascript engines. A simulation of 8 million
steps (part 2 of day 6 of Advent of Code 2024, on a test grid) takes 19 s in
_tinyscript_ and 1.9 s in _CPython_; _node.js_ is faster still. For most scripts
and the Advent of Code puzzles, this is fast enough: most days of 2023 and 2024
take a few seconds on real inputs. Calls of small functions and methods in inner
loops are the biggest cost; such code is up to about 300 times slower than in
node. The engine deliberately stays a plain tree walk that is easy to
understand; measurements and the next ideas are in [TODO.md](TODO.md#speed).

When I measured in 2015 (Java 8), _tinyscript_ was slower than _V8_ by a factor
of 10 and than _Nashorn_ by a factor of 5–7, but often twice as fast as the Java
based Javascript engine [DynJS](http://dynjs.org/). :-)

## Sandboxing

I've implemented a couple of strategies to monitor (and limit) the computing and
memory resources when _tinyscript_'s interpreter is embedded into a server side
Java application. These can also be tested in the demo web app -- the demo web
app also reports the corresponding resource consumption measurements.

I've included some very simple measurement strategies (execution time (_time_),
number of executed statements (_stmts_), and the depth of the callgraph
(_calldepth_)). Additionally I've experimented with a few more complex ones:

- Using the consumed CPU time (_cpu_) and the memory allocated by the
  interpreter (_malloc_) via Java's `ThreadMXBeans`.

- Tracking the creation (_creates_) and destruction (by the garbage collector)
  of objects in _tinyscript_, including a rough estimation of the corresponding
  memory consumption from a user's perspective (_objs_, _umem_).

You can experiment with these features in the demo web app. You could use the
last program in the _Examples_ drop down menu for this purpose or you could
compute the fibonacci numbers > 27. In the demo web app I've picked rather large
limits because I wanted to check if my implementation is robust/scalable.

## Setup

When you're done playing around with the online demo and want to install your
own version of _tinyscript_ or start hacking on it, here is a quick overview.
The project is built with _Maven_ and requires _Java 25_. It is best developed
with _Eclipse 2026-09_ and _Xtext 2.44.0_.

The build has two tiers: a _Tycho_ reactor at the root for the OSGi/Eclipse
plugin modules, and separate plain Maven builds for the runnable applications
(REPL and web demo). Build in this order:

1.  Install the core language and Eclipse plugins into the local Maven
    repository:

        ./mvnw clean install

2.  Build the REPL and web demo fat JARs:

        ./mvnw clean package -f de.mkbauer.tinyscript.repl/pom.xml
        ./mvnw clean package -f de.mkbauer.tinyscript.webdemo/pom.xml

The web demo build also runs the Vue 3 frontend through Vite (npm install → test
→ build) automatically.

To run:

- The interpreter: `de.mkbauer.tinyscript.repl/tinyscript example.tys` runs a
  file, without arguments it starts an interactive session (`--help` shows the
  options), which shows the value of each expression; statements end with `;`
  there as well, and Ctrl-C stops a statement that runs too long;
  `!load file.tys` runs a file in the session to try out its functions, `!help`
  lists the other commands. Tab completes names (after a dot the properties,
  `Math.` → `max`, `min`, …), the input is colored, and a statement with an open
  bracket goes on in the next line and is edited as a whole. Arguments after
  `--` are the script's `System.args` (`tinyscript day1.tys -- input.txt`). The
  script finds Java 25 via `JAVA_HOME` or the `PATH` and can be linked into a
  directory on your `PATH`:

      ln -s "$PWD/de.mkbauer.tinyscript.repl/tinyscript" ~/bin/tinyscript
      tinyscript example.tys

  Or directly:
  `java -jar de.mkbauer.tinyscript.repl/target/tinyscript-repl.jar example.tys`

- The web demo as a _Spring Boot_ application:

      java -jar de.mkbauer.tinyscript.webdemo/target/tinyscript-webdemo-*.jar

- Via Docker:

      docker pull mkbauer/tinyscript:latest
      docker run -p 8080:8080 mkbauer/tinyscript:latest

For a full description of the build system — module taxonomy, build stages,
frontend development workflow, and build outputs — see
[docs/build.md](docs/build.md).

Builds and tests run automatically on GitHub Actions on every push to `master`.
Pushing a `vX.Y.Z` tag triggers the Docker workflow, which publishes the image
to Docker Hub automatically. See [Release-HOWTO.md](Release-HOWTO.md) for the
release process.

## License

I did write the code on my own, mostly in my spare time around the turn of the
year 2015/2016. Initially, it was intended as a test bed for embedding a
scripting language into our SaaS offering at work. However, since we decided to
go for another approach, I decided to put the project on github. Maybe it is of
interest to somebody or it can serve as an inspiration, if you want to design
and implement your own programming language. It is a lot of fun!

I decided to release the code under the GPL license. In case you want to do
something with the code that GPL does not allow, feel free to contact me.

## Roadmap

Open issues and ideas are tracked in [TODO.md](TODO.md).
