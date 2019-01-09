tinyscript -- a simple Javascript like language
===============================================

This project is about the implementation of a simple Javascript like language, named *tinyscript*.

What is tinyscript?
-------------------

*tinyscript* is a small Javascript like language. Here is a piece of *tinyscript* code:

	// Compute fibonacci numbers

	function fibonacci(n) {
		var h = 1, g = 1, f = 1;
		for (var i = 2, n) {
			h = g;
			g = f;
			f = g + h;
		}
		return f;
	}

	print(fibonacci(7));  // 21
	print(fibonacci(50)); // 20365011074


You can play with it online in your browser [here](https://home.mkbauer.de/tinyscript). 

*tinyscript* offers quite a complete set of interesting language features:

* [Basic expressions](de.mkbauer.tinyscript.webdemo/src/main/resources/public/expressions.ts)

* [Handling of strings](de.mkbauer.tinyscript.webdemo/src/main/resources/public/strings.ts)

* [Control-flow structures](de.mkbauer.tinyscript.webdemo/src/main/resources/public/primes.ts): `for`- and `if`-statements

* [Functions](de.mkbauer.tinyscript.webdemo/src/main/resources/public/fibonacci.ts), including [recursion](de.mkbauer.tinyscript.webdemo/src/main/resources/public/fibonacci_recursive.ts) and *arrow* functions

* [Arrays](de.mkbauer.tinyscript.webdemo/src/main/resources/public/arrays.ts)

* [Closures](de.mkbauer.tinyscript.webdemo/src/main/resources/public/closures.ts)

* [Objects and Prototypes](de.mkbauer.tinyscript.webdemo/src/main/resources/public/oo.ts)

Around Christmas time 2015, I've designed *tinyscript* and implemented an interpreter for it in Java for a couple of reasons:

- I believe every software engineer should and some point design and implement his own language just for the fun of it. I wanted to go a bit further than the usual toy examples taught in compiler construction classes, so I came up with *tinyscript*.

- At that time in my office we were looking into embedding a scripting language into our SaaS business software offering. We were looking for a way to allow for customor / tenant specific business logic that could be run on both our backend servers (mainly written in Java) and in client apps (e.g. written in C# and Xamarin). For such a use case, some kind of sandboxing and resource monitoring is necessary to avoid that ill-behaving, tenant specific code can harm other tenants or the SaaS plattform as a whole. For that purpose we needed a test "vehicle".   

- I wanted to have a look at the [Xtext framework](https://www.eclipse.org/Xtext/) for language engineering. So I used tinyscript as an example project to get my hands dirty with that.

What did I do?
--------------

-  Definition of *tinyscript*'s grammar. Immplementation of the corresponding parser including an *abstract syntax tree (AST)* using *Xtext* (project [`de.mkbauer.tinyscript`](de.mkbauer.tinyscript), package `tinyscript`). 

- Design and impementation of a „high-level“ interpreter in Java (project [`de.mkbauer.tinyscript`](de.mkbauer.tinyscript), package `tinyscript.interpreter`). Specifically have a look at class `ExecutionVisitor`and *tinyscript*'s runtime (package `tinyscript.runtime`).

- Integration of *tinyscript*'s interpreter into a demo web app ((project [`de.mkbauer.tinyscript.webdemo`](de.mkbauer.tinyscript.webdemo)) based on *SpringBoot*. That's the same [web app](https://home.mkbauer.de/tinyscript) I've mentioned above when introducing *tinyscript*'s language features.

- With *Xtext* it's easy to provide an editor plugin for *Eclipse* (project [`de.mkbauer.tinyscript.ui`](de.mkbauer.tinyscript.ui)).

- Implementation of a very rudimentary code generator to generate valid Javascript code from *tinyscript*'s AST (package `tinyscript.generator`). This code can be executed either with the *Nashorn* engine (shipped with JDK8) or within a native iOS app. For the latter, I've provided a proof-of-concept implementation in branch `ios_demo`.



Limitations of *tinyscript*
---------------------------

Certainly, *tinyscript* is not perfect. It's just a proof-of-concept (but a usable one!). There are many things to improve. For example the concept of *equality* and the conversion of primitive datatypes in Javascript is a mine-field on its own.

*tinyscript* differs from Javascript in a couple of ways. Some are limitations, some are design decisions because I wanted *tinyscript* to be different. 

Examples:

- The rules for ending statements with `;` are roughly the same as in Java.
- Blocks create real *scopes*, they limit the visibility and validity of local variables.
- Control structures always need blocks: `if (cond) { stmts; } else { stmts; }`
- Elements can be appended to arrays using the operator `+`: : `var arr = [„hallo"]; arr = arr + „welt“; -> arr = [„hallo“, „welt“];`
- There are two types of `for`-loops: a numeric `for`-loop (inspired by *Lua*) and one to iterate over arrays.
- There is no support for *regular expressions*.
- *tinyscript* supports *arrow* functions. These are part of Javascript starting with Ecmascript 6 :-)

Apart form that, *tinyscript* behaves like Javascript in *strict mode*, i.e. all variables have to be declared before they can be used.


Performance
-----------

The runtime performance of *tinyscript*'s interpreter is rather poor, especially when compared with the highly-optimized Javascript engines. Usually,  *tinyscript* is slower by a factor 10 (*node.js*, *V8*), or factor 5-7 (*Nashorn*) langsamer. 

But: *tinyscript* is often twice as fast as the Java based Javascript engine [dynJS](http://dynjs.org/). :-)

Examples: 

- An iterative implementation of Fibonacci numbers [Ian Bull's benchmark](https://eclipsesource.com/blogs/2014/11/17/highly-efficient-java-javascript-integration/): 100000 iterations: *tinyscript*: 6195ms, *Nashorn*: 1021ms, *DynJS*: 29047ms

- `People.js` (see demo web app): N=100000, R=10: *tinyscript*: 37000ms, *DynJS*: 140000ms.


Sandboxing
----------

I've implemented a couple of strategies to monitor (and limit) the computing and memory resources when *tinyscript*'s interpreter is embedded into a server side Java application. These can also be tested in the demo web app -- the demo web app also reports the corresponding resource consumption measurements.

I've included some very simple measurement strategies (execution time (*time*), number of executed statements (*stmts*), and the depth of the callgraph (*calldepth*)). Additionally I've experimented with a few more complex ones:

- Using the consumed CPU time (*cpu*) and the memory allocated by the interpreter (*malloc*) via Java's  `ThreadMXBeans`.

- Tracking the creation (*creates*) and destruction (by the garbage collector) of objects in *tinyscript*, including a rough estimation of the corresponding memory consumption from a user's perspective (*objs*, *umem*).

You can experiment with these features in the demo web app. You could use the last program in the *Examples* drop down menu for this purpose or you could compute the fibonacci numbers > 27. In the demo web app I've picked rather large limits because I wanted to check if my implementation is robust/scalable.  


Setup
-----

When you're done with playing around with the online demo and want to istall your own version of *tinyscript* or start hacking on it, here are a few steps to get started. As *xtext* is a part of the Eclipse project, it is best developed with Eclipse. As of 2018, the current version is developed using *Eclipse Photon* and xtext 2.14.0. The project can be build using *Maven*. *tinyscript* consists of a number of seperate projects:

- The *tinyscript* core language in `de.mkbauer.tinyscript` and the corresponding plugins for an Eclipse editor in `de.mkbauer.tinyscript.ide`, `de.mkbauer.tinyscript.ui` and some Eclipse related helper projects in `de.mkbauer.tinyscript.sdk` and `de.mkbauer.tinyscript.updatesite`

- The *tinyscript* interpreter application in `de.mkbauer.tinyscript.repl` which depends on the core language. This application can be used in an intercative mode (*REPL*) or it can execute *tinyscript* files.

- The *tinyscript* web demo in `de.mkbauer.tinyscript.webdemo`, also depending on the core language.

To build:

- Build and install the core language in the top level project first:

		mvn clean install

- Then build and package the other projects (interpreter appication and webdemo). This will create the *fat jars* for both applications (see `target` folders).

		cd de.mkbauer.tinyscript.repl
		mvn clean package
		cd ..
	
		cd de.mkbauer.tinyscript.webdemo
		mvn clean package
		cd ..

To run:

- The interpreter:

		java -jar de.mkbauer.tinyscript.repl-<version>.jar example.ts

- The webdemo as a *Spring Boot* application:

		java -jar de.mkbauer.tinyscript.webdemo-<version>.jar


License
-------

I did write the code on my own, mostly in my spare time around the turn of the year 2105/2016. Initially, it was intended as a test bed for embedding a scripting language into our SaaS offering at work. However, since we decided to go for another approach, I decided to put the project on github. Maybe it is of interest to somebody or it can serve as an inspiration, if you want to design and implement your own programming language. It is a lot of fun!

I decided to release the code under the GPL license. In case you want to do something with the code that GPL does not allow, feel free to contact me.


Open isues, ideas to implement
------------------------------

As of 2018, if I had some time, I'd like to work on the following topics:

- ~~Update the project to a newer version of Eclipse and Xtext~~. I fear that this would mean quite a bit of work, but it would allow for a couple of cool features (such as language server support for Visual Studio Code).

- Improve the documentation.

- Improve the test cases.

- Clean up the interpreter, excpecially when working with expressions and builtin types.

- Add some more functions to the runtime library.

- Add some support for *modules*.