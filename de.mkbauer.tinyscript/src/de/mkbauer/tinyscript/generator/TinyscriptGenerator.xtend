
package de.mkbauer.tinyscript.generator

import de.mkbauer.tinyscript.ts.Tinyscript
import de.mkbauer.tinyscript.ts.VariableStatement
import de.mkbauer.tinyscript.ts.BinaryExpression
import de.mkbauer.tinyscript.ts.BinaryOperator
import de.mkbauer.tinyscript.ts.UnaryOperator
import de.mkbauer.tinyscript.Operators
import de.mkbauer.tinyscript.ts.Identifier
import de.mkbauer.tinyscript.ts.Reference
import de.mkbauer.tinyscript.ts.AssertStatement
import de.mkbauer.tinyscript.ts.BreakStatement
import de.mkbauer.tinyscript.ts.ContinueStatement
import de.mkbauer.tinyscript.ts.ClassDeclaration
import de.mkbauer.tinyscript.ts.InterfaceDeclaration
import de.mkbauer.tinyscript.ts.Pattern
import de.mkbauer.tinyscript.ts.ArrayPattern
import de.mkbauer.tinyscript.ts.SpreadElement
import de.mkbauer.tinyscript.ts.ObjectPattern
import de.mkbauer.tinyscript.ts.ThrowStatement
import de.mkbauer.tinyscript.ts.TryStatement
import de.mkbauer.tinyscript.ts.ConditionalExpression
import de.mkbauer.tinyscript.ts.WhileStatement
import de.mkbauer.tinyscript.ts.StringLiteral
import de.mkbauer.tinyscript.ts.BlockStatement
import de.mkbauer.tinyscript.ts.Block
import de.mkbauer.tinyscript.TinyscriptModelUtil
import de.mkbauer.tinyscript.scoping.TinyscriptQualifiedNameProvider
import de.mkbauer.tinyscript.ts.NumberLiteral
import de.mkbauer.tinyscript.ts.ExpressionStatement
import de.mkbauer.tinyscript.ts.FunctionDefinition
import de.mkbauer.tinyscript.ts.ReturnStatement
import de.mkbauer.tinyscript.ts.CallSuffix
import de.mkbauer.tinyscript.ts.ComputedPropertyAccessSuffix
import de.mkbauer.tinyscript.ts.DotPropertyAccessSuffix
import de.mkbauer.tinyscript.ts.CallOrPropertyAccess
import de.mkbauer.tinyscript.ts.CallOrPropertyAccessSuffix
import de.mkbauer.tinyscript.ts.PropertyName
import de.mkbauer.tinyscript.ts.BooleanLiteral
import de.mkbauer.tinyscript.ts.Unary
import de.mkbauer.tinyscript.ts.NewExpression
import de.mkbauer.tinyscript.ts.GroupingExpression
import de.mkbauer.tinyscript.ts.IfStatement
import de.mkbauer.tinyscript.ts.FunctionDeclaration
import de.mkbauer.tinyscript.ts.ObjectInitializer
import de.mkbauer.tinyscript.ts.PropertyAssignment
import de.mkbauer.tinyscript.ts.ArrayInitializer
import de.mkbauer.tinyscript.ts.ForStatement

import com.google.inject.Inject
import de.mkbauer.tinyscript.ts.IterableForExpression
import de.mkbauer.tinyscript.ts.ArrowFunction
import de.mkbauer.tinyscript.ts.ArrowExpressionBody
import de.mkbauer.tinyscript.ts.NullObject
import de.mkbauer.tinyscript.ts.UndefinedLiteral
import de.mkbauer.tinyscript.interpreter.ModuleLoader
import de.mkbauer.tinyscript.interpreter.Modules
import de.mkbauer.tinyscript.interpreter.TSNumber
import de.mkbauer.tinyscript.interpreter.TinyscriptReferenceError
import de.mkbauer.tinyscript.TinyscriptRuntimeException
import de.mkbauer.tinyscript.ts.ImportStatement
import java.util.ArrayDeque
import java.util.HashMap
import java.util.LinkedHashMap
import org.eclipse.emf.common.util.URI
import de.mkbauer.tinyscript.ts.Expression

class TinyscriptGenerator   {
	
	@Inject
	TinyscriptQualifiedNameProvider nameProvider;
	
	// Counter for unique names of temporary variables (loop bounds, iterables, indexes)
	int tempCounter = 0;
	
	def generateBuiltins() '''

		// A failed assert is a programming error: catch does not get it (see __ts_catchable)
		class __ts_AssertionError extends Error {}
		__ts_AssertionError.prototype.name = "AssertionError";

		function assert(condition) {
			if (!condition) {
				throw new __ts_AssertionError("assert: condition is false");
			}
		}

		// Like the interpreter: only Error objects can be thrown
		function __ts_throwable(value) {
			if (!(value instanceof Error)) {
				throw new TypeError("throw needs an Error object, e.g. throw new Error(\"...\"), not " + (value === null ? "object" : typeof value));
			}
			return value;
		}

		// Like the interpreter: catch gets only the errors thrown with throw and failed input / output
		// (an Error named IOError); programming errors (TypeError, RangeError such as a stack overflow,
		// ReferenceError ...) and failed asserts go on and end the script
		function __ts_catchable(error) {
			if (!(error instanceof Error) || error instanceof TypeError || error instanceof RangeError
					|| error instanceof ReferenceError || error instanceof SyntaxError || error instanceof __ts_AssertionError) {
				throw error;
			}
		}
		
		var System = {};
		System.currentTimeMillis = Date.now;
		// Like the interpreter: the arguments of the script
		System.args = typeof process !== "undefined" ? process.argv.slice(2) : [];
		
		// Like the interpreter (Display): the text of a value for print, + with a string and String(x).
		// A string stays as it is; inside arrays and objects, strings are quoted as by JSON.stringify,
		// a cycle is [Circular], nesting deeper than 50 levels [Object] / [Array], and an object with
		// a toString of its own shows what it returns.
		const __ts_JsString = globalThis.String;
		// Like the interpreter: o.toString() and a.toString() give the same text as print
		// (Javascript: "[object Object]", "1,2"); also join uses it for the elements
		for (const prototype of [Object.prototype, Array.prototype]) {
			Object.defineProperty(prototype, "toString", {
				value: function toString() { return __ts_display(this, [], true); },
				writable: true, configurable: true, enumerable: false
			});
		}
		// Like the interpreter (and the entries of a Map): an array of [index, element] pairs
		Object.defineProperty(Array.prototype, "entries", {
			value: function entries() { return this.map((item, i) => [i, item]); },
			writable: true, configurable: true, enumerable: false
		});
		const __ts_builtinToStrings = new globalThis.Set([Object.prototype.toString, Array.prototype.toString,
			Function.prototype.toString, Error.prototype.toString, __ts_JsString.prototype.toString]);

		function __ts_str(value) {
			if (typeof value === "string") {
				return value;
			}
			return __ts_display(value, []);
		}

		// A class of the script (written with class, not a constructor function)
		function __ts_isClass(f) {
			return typeof f === "function" && /^class[\s{]/.test(Function.prototype.toString.call(f));
		}

		// own: the value itself, without its toString (for Object.prototype.toString)
		function __ts_display(value, path, own) {
			if (typeof value === "string") {
				return __ts_JsJSON.stringify(value);
			}
			if (value === null || (typeof value !== "object" && typeof value !== "function")) {
				return __ts_JsString(value);
			}
			if (path.includes(value)) {
				return "[Circular]";
			}
			const toString = value.toString;
			if (!own && typeof toString === "function" && !__ts_builtinToStrings.has(toString) && !(value instanceof __ts_Range)) {
				const text = toString.call(value);
				if (typeof text === "string") {
					return text;
				}
			}
			if (path.length >= 50) {
				return Array.isArray(value) ? "[Array]" : "[Object]";
			}
			path.push(value);
			try {
				const show = item => __ts_display(item, path);
				if (Array.isArray(value)) {
					return "[" + Array.from(value, show).join(", ") + "]";
				}
				if (value instanceof globalThis.Map) {
					return "Map {" + Array.from(value, ([k, v]) => show(k) + " => " + show(v)).join(", ") + "}";
				}
				if (value instanceof globalThis.Set) {
					return "Set {" + Array.from(value, show).join(", ") + "}";
				}
				if (value instanceof __ts_Range || value instanceof Error) {
					return value.toString();
				}
				if (value instanceof __ts_JsString) {
					return value.valueOf();
				}
				const properties = Object.keys(value).map(key => key + ": " + show(value[key]));
				if (typeof value === "function") {
					const name = __ts_isClass(value) ? "[class " + value.name + "]" : "[Function" + (value.name ? ": " + value.name : "") + "]";
					return properties.length > 0 ? "{ " + name + " " + properties.join(", ") + " }" : name;
				}
				// An instance of a class shows its name first: Num {value: 3}
				const prototype = Object.getPrototypeOf(value);
				const prefix = prototype !== null && Object.hasOwn(prototype, "constructor") && __ts_isClass(prototype.constructor)
					&& prototype.constructor.prototype === prototype ? prototype.constructor.name + " " : "";
				return prefix + "{" + properties.join(", ") + "}";
			}
			finally {
				path.pop();
			}
		}

		// Like the interpreter: String(x) converts as print does
		var String = (function (JsString) {
			const S = function String(value) {
				const text = arguments.length === 0 ? "" : __ts_str(value);
				return new.target ? new JsString(text) : text;
			};
			S.prototype = JsString.prototype;
			S.fromCharCode = JsString.fromCharCode;
			return S;
		})(__ts_JsString);

		if (typeof print !== "function") {
			// Like the interpreter: all arguments, separated by spaces, on one line. Node's console
			// itself, since a script may declare its own console (the webdemo's oo.tys does)
			// (a method, so that, as in the interpreter, new print() is an error)
			const __ts_console = globalThis.console;
			globalThis.print = {
				print(...args) {
					__ts_console.log(args.map(__ts_str).join(" "));
				}
			}.print;
		}

		function __ts_add(obj1, obj2) {
			if (typeof obj1 === "string" || typeof obj2 === "string") {
				return __ts_str(obj1) + __ts_str(obj2);
			}
			if (Array.isArray(obj1) || Array.isArray(obj2)) {
				if (!(Array.isArray(obj1) && Array.isArray(obj2))) {
					throw new TypeError("'+' can only add an array to another array or to a string");
				}
				return obj1.concat(obj2);
			}
			return obj1 + obj2;
		}
		
		// Like the interpreter: an interface is a value with the names of its methods; a class that
		// implements it is checked when it is declared
		class __ts_Interface {
			constructor(name, methods) {
				this.name = name;
				this.methods = methods;
			}
			toString() { return "[interface " + this.name + "]"; }
		}
		function __ts_implements(cls, className, anInterface, interfaceName) {
			if (!(anInterface instanceof __ts_Interface)) {
				const what = __ts_isClass(anInterface) ? "a class" : anInterface == null ? String(anInterface)
					: (typeof anInterface === "object" ? "an " : "a ") + typeof anInterface;
				throw new TypeError(className + " implements " + interfaceName + ", but " + interfaceName + " is " + what + ", not an interface");
			}
			const missing = anInterface.methods.filter(method => typeof cls.prototype[method] !== "function");
			if (missing.length > 0) {
				throw new TypeError(className + " implements " + anInterface.name + " but has no method" + (missing.length > 1 ? "s " : " ") + missing.join(", "));
			}
		}

		// Like the interpreter: JSON has only parse, which needs a string; a text that is no JSON is
		// an Error named SyntaxError, which catch gets (a SyntaxError of node would end the script)
		const __ts_JsJSON = globalThis.JSON;
		var JSON = {
			parse(text) {
				if (typeof text !== "string") {
					throw new TypeError("JSON.parse() needs a string, not " + (text === null ? "object" : typeof text));
				}
				try {
					return __ts_JsJSON.parse(text);
				}
				catch (e) {
					const error = new Error("JSON.parse: " + e.message);
					error.name = "SyntaxError";
					throw error;
				}
			}
		};

		// Like the interpreter: keys(), values() and entries() of Maps and Sets return arrays
		var Map = (function (JsMap) {
			return class Map extends JsMap {
				keys() { return Array.from(super.keys()); }
				values() { return Array.from(super.values()); }
				entries() { return Array.from(super.entries()); }
			};
		})(globalThis.Map);
		var Set = (function (JsSet) {
			return class Set extends JsSet {
				values() { return Array.from(super.values()); }
			};
		})(globalThis.Set);

		// Like the interpreter: range(end), range(start, end[, step]) is a lazy range of integers
		// with an exclusive end
		class __ts_Range {
			constructor(start, end, step) {
				this.start = start;
				this.end = end;
				this.step = step;
			}
			get length() {
				return Math.max(0, Math.ceil((this.end - this.start) / this.step));
			}
			*[Symbol.iterator]() {
				for (let i = this.start; this.step > 0 ? i < this.end : i > this.end; i += this.step) {
					yield i;
				}
			}
			toArray() {
				return Array.from(this);
			}
			map(f) {
				return Array.from(this).map((value, index) => f(value, index, this));
			}
			forEach(f) {
				let index = 0;
				for (const value of this) {
					f(value, index++, this);
				}
			}
			// Like the interpreter: the same integers backwards, again a lazy range
			reverse() {
				const last = this.start + (this.length - 1) * this.step;
				return new __ts_Range(last, this.start - this.step, -this.step);
			}
			toString() {
				return "range(" + this.start + ", " + this.end + (this.step !== 1 ? ", " + this.step : "") + ")";
			}
		}

		// A property of the global object, so that a script can declare its own range (a method, so
		// that, as in the interpreter, new range(3) is an error)
		globalThis.range = { range(...args) {
			if (args.length < 1 || args.length > 3) {
				throw new TypeError("range() needs 1 to 3 arguments, not " + args.length);
			}
			for (const arg of args) {
				if (!Number.isInteger(arg)) {
					throw new TypeError("range() needs integers, not " + (typeof arg === "number" ? arg : typeof arg));
				}
			}
			const [start, end, step] = args.length > 1 ? [args[0], args[1], args[2] ?? 1] : [0, args[0], 1];
			if (step === 0) {
				throw new TypeError("range() needs a step other than 0");
			}
			return new __ts_Range(start, end, step);
		} }.range;

		// Like the interpreter, what the iterable for loop goes through: an array item by item (seeing
		// the items the loop adds), the characters of a string, a copy of the entries of a Map or Set,
		// a range lazily
		function __ts_iterate(value) {
			if (typeof value === "string" || value instanceof String) {
				return String(value).split("")[Symbol.iterator]();
			}
			if (value instanceof globalThis.Map || value instanceof globalThis.Set) {
				return Array.from(value)[Symbol.iterator]();
			}
			if (Array.isArray(value) || value instanceof __ts_Range || (value !== null && typeof value === "object" && value.__ts_lines === true)) {
				return value[Symbol.iterator]();
			}
			throw new TypeError("for needs an array, a string, a Map, a Set or a range");
		}

		// Like the interpreter: bitwise operators on integers up to 53 bits (Javascript's own work on 32 bits)
		function __ts_bitwise(op, a, b) {
			const operand = x => {
				if (!Number.isSafeInteger(x)) {
					throw new TypeError("'" + op + "' needs integers up to 2^53");
				}
				return BigInt(x);
			};
			let result;
			if (op === "~") {
				result = ~operand(a);
			}
			else if (op === "<<" || op === ">>") {
				if (!Number.isInteger(b) || b < 0 || b > 53) {
					throw new TypeError("'" + op + "' needs a shift count from 0 to 53");
				}
				result = op === "<<" ? operand(a) << BigInt(b) : operand(a) >> BigInt(b);
			}
			else {
				const x = operand(a);
				const y = operand(b);
				result = op === "&" ? x & y : op === "|" ? x | y : x ^ y;
			}
			const number = Number(result);
			if (!Number.isSafeInteger(number)) {
				throw new TypeError("'" + op + "' gives more than 53 bits");
			}
			return number;
		}

		// Like the interpreter: delete removes a property of an object, not an element of an array
		function __ts_delete(object, key) {
			if (Array.isArray(object)) {
				throw new TypeError("Arrays are lists without gaps: delete cannot remove an element, use splice: a.splice(index, 1)");
			}
			if (object instanceof globalThis.Map || object instanceof globalThis.Set) {
				throw new TypeError("Maps and Sets have no properties: use delete(…)");
			}
			delete object[key];
			return true;
		}

		// Like the interpreter: ...items in a call or an array literal takes what the for loop goes
		// through (the characters of a string, the entries of a Map), nothing else
		function __ts_spread(value) {
			if (typeof value === "string" || value instanceof String) {
				return String(value).split("");
			}
			if (Array.isArray(value) || value instanceof globalThis.Map || value instanceof globalThis.Set || value instanceof __ts_Range) {
				return value;
			}
			throw new TypeError("... needs an array, a string, a Map, a Set or a range");
		}

		// Like the interpreter: an array pattern [a, b] needs an array with at least as many elements
		function __ts_pattern(value, count) {
			if (!Array.isArray(value) || value.length < count) {
				throw new TypeError("an array pattern needs an array with at least " + count + " elements");
			}
			return value;
		}

		// Tinyscript's ==: no type coercion, but null == undefined; String objects equal their strings
		function __ts_eq(obj1, obj2) {
			if (obj1 == null || obj2 == null) {
				return obj1 == obj2;
			}
			if ((typeof obj1 === "string" || obj1 instanceof String) && (typeof obj2 === "string" || obj2 instanceof String)) {
				return String(obj1) === String(obj2);
			}
			return obj1 === obj2;
		}
		
	'''
	
	def generate(Tinyscript script, boolean includeBuiltins) {
		generate(script, includeBuiltins, null)
	}
	
	/**
	 * Generates one Javascript program from {@code script} and the modules it imports
	 * (resolved with {@code loader}; without a loader, imports are an error). Each module
	 * becomes an immediately invoked function in dependency order, so it runs once and
	 * its private names stay private; it returns its frozen exports.
	 */
	def generate(Tinyscript script, boolean includeBuiltins, ModuleLoader loader) {
		tempCounter = 0
		modules = new LinkedHashMap
		moduleVariables = new HashMap
		importedModules = new HashMap
		val loading = new ArrayDeque<URI>
		loading.push(script.eResource.URI)
		collectModules(script, loader, loading)
		generateScript(script, includeBuiltins)
	}
	
	// The modules in dependency order, their variable names, and the module of each import
	LinkedHashMap<URI, Tinyscript> modules
	HashMap<URI, String> moduleVariables
	HashMap<ImportStatement, URI> importedModules
	
	def private void collectModules(Tinyscript script, ModuleLoader loader, ArrayDeque<URI> loading) {
		for (importStatement : script.imports) {
			try {
				val uri = Modules.resolve(loader, importStatement)
				if (loading.contains(uri))
					throw Modules.cyclicImport(loading, uri)
				if (!modules.containsKey(uri)) {
					// Native modules (std:fs, std:io) have no script; they are emitted as Javascript directly
					val module = if (Modules.isNative(uri)) null else Modules.load(script.eResource.resourceSet, uri)
					if (module !== null) {
						loading.push(uri)
						collectModules(module, loader, loading)
						loading.pop
					}
					modules.put(uri, module)
					moduleVariables.put(uri, "__ts_mod" + modules.size)
				}
				val exported = if (Modules.isNative(uri)) Modules.nativeExports(uri).toSet
					else TinyscriptModelUtil.exportedIdentifiers(modules.get(uri)).map[name].toSet
				for (name : importStatement.names) {
					if (!exported.contains(name.name))
						throw new TinyscriptReferenceError(Modules.displayName(uri) + " does not export \"" + name.name + "\"")
				}
				importedModules.put(importStatement, uri)
			}
			catch (TinyscriptRuntimeException e) {
				e.attachNodeIfAbsent(importStatement)
				throw e
			}
		}
	}
	
	// A program (with the builtins) runs in strict mode, as the interpreter (this in a plain call is
	// undefined, assigning to Math.PI is an error), and the script is a block of its own, so that its
	// declarations hide the builtins of the prelude (const Map = …; instead of a SyntaxError for
	// declaring Map twice). Without the builtins, only the code of the script (for tests)
	def private generateScript(Tinyscript script, boolean includeBuiltins) '''
		«IF includeBuiltins»
			"use strict";
			«generateBuiltins»
		«ENDIF»
		«FOR entry : modules.entrySet.filter[value === null]»
			var «moduleVariables.get(entry.key)» = «generateNativeModule(entry.key)»;
		«ENDFOR»
		«FOR entry : modules.entrySet.filter[value !== null]»
			var «moduleVariables.get(entry.key)» = (function () {
				«generateImports(entry.value)»
				«entry.value.generate»
				return Object.freeze({«FOR id : TinyscriptModelUtil.exportedIdentifiers(entry.value) SEPARATOR ', '»«id.name»: «id.generate»«ENDFOR»});
			})();
		«ENDFOR»
		«IF includeBuiltins»
			{
				«generateImports(script)»
				«script.generate»
			}
		«ELSE»
			«generateImports(script)»
			«script.generate»
		«ENDIF»
	'''
	
	// std:fs for node, with the same behavior as FileSystemModule (UTF-8; a failed read or write is
	// an Error named IOError with the same message, wrong arguments a TypeError). The functions of
	// the native modules are methods, so that new is an error for them, as in the interpreter
	def private generateNativeModule(URI module) {
		switch (module.opaquePart) {
			case "fs": '''
				(function () {
					var nodefs = require("fs");
					var reasons = { ENOENT: "not found", EACCES: "permission denied", EPERM: "permission denied", ENOTDIR: "not a directory" };
					function io(what, path, action) {
						try {
							return action();
						}
						catch (e) {
							if (e instanceof TypeError || !e.code) { throw e; }
							var error = new Error(what + " \"" + path + "\": " + (reasons[e.code] || e.message));
							error.name = "IOError";
							throw error;
						}
					}
					function read(path) { return io("Cannot read file", path, function () { return nodefs.readFileSync(path, "utf8"); }); }
					function write(path, text) { io("Cannot write file", path, function () { nodefs.writeFileSync(path, text, "utf8"); }); }
					return Object.freeze({
						exists(path) { return nodefs.existsSync(path); },
						readFile(path) { return read(path); },
						readLines(path) {
							var text = read(path);
							if (text === "") { return []; }
							var lines = text.split(/\r?\n/);
							if (lines[lines.length - 1] === "") { lines.pop(); }
							return lines;
						},
						writeFile(path, text) { write(path, text); },
						writeLines(path, lines) { write(path, lines.map(function (line) { return line + "\n"; }).join("")); },
						appendFile(path, text) { io("Cannot write file", path, function () { nodefs.appendFileSync(path, text, "utf8"); }); },
						listDir(path) { return io("Cannot list directory", path, function () { return nodefs.readdirSync(path).sort(); }); }
					});
				})()'''
			// std:io like IoModule: lines of the standard input (UTF-8, split at \n, a \r before it
			// removed), read synchronously, so that a script reads as in the interpreter; null at the end
			case "io": '''
				(function () {
					var nodefs = require("fs");
					var chunk = Buffer.alloc(65536);
					var pending = Buffer.alloc(0);
					var ended = false;
					function strip(line) { return line.endsWith("\r") ? line.slice(0, -1) : line; }
					function read(prompt) {
						if (prompt !== "") { process.stdout.write(prompt); }
						while (true) {
							var newline = pending.indexOf(10);
							if (newline >= 0) {
								var line = pending.subarray(0, newline).toString("utf8");
								pending = pending.subarray(newline + 1);
								return strip(line);
							}
							if (ended) {
								if (pending.length === 0) { return null; }
								var rest = pending.toString("utf8");
								pending = Buffer.alloc(0);
								return strip(rest);
							}
							var count;
							try {
								count = nodefs.readSync(0, chunk, 0, chunk.length, null);
							}
							catch (e) {
								if (e.code === "EAGAIN") {
									// A non-blocking standard input without data yet: wait a little
									Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 10);
									continue;
								}
								if (e.code !== "EOF") {
									var error = new Error("Cannot read the standard input: " + e.message);
									error.name = "IOError";
									throw error;
								}
								count = 0;
							}
							if (count === 0) { ended = true; }
							else { pending = Buffer.concat([pending, chunk.subarray(0, count)]); }
						}
					}
					return Object.freeze({
						readLine(prompt) {
							if (prompt !== undefined && typeof prompt !== "string") {
								throw new TypeError("io.readLine: prompt must be a string, not " + typeof prompt);
							}
							return read(prompt === undefined ? "" : prompt);
						},
						lines() {
							return {
								__ts_lines: true,
								[Symbol.iterator]: function* () {
									for (var line = read(""); line !== null; line = read("")) { yield line; }
								},
								toString: function () { return "[Lines]"; }
							};
						}
					});
				})()'''
			default: throw new IllegalArgumentException("Not a native module: " + module)
		}
	}
	
	def private generateImports(Tinyscript script) '''
		«FOR importStatement : script.imports»
			«val module = moduleVariables.get(importedModules.get(importStatement))»
			«IF importStatement.namespace !== null»
				const «importStatement.namespace.generate» = «module»;
			«ENDIF»
			«FOR name : importStatement.names»
				const «name.generate» = «module».«name.name»;
			«ENDFOR»
		«ENDFOR»
	'''
			
	def dispatch generate(Tinyscript script) '''
		«FOR stmt: script.global.statements»
			«stmt.generate»
		«ENDFOR»
	'''

	def dispatch generate(BlockStatement stmt) '''
		{
			«stmt.block.generate»
		}
	'''
	
	def dispatch generate(Block block) '''
		«FOR stmt: block.statements»
			«stmt.generate»
		«ENDFOR»
	'''

	def dispatch generate(ExpressionStatement stmt) '''
		«stmt.expr.generate»«IF Operators.increment(stmt) !== null»«Operators.increment(stmt).literal»«ENDIF»;
	'''
	
	// tinyscript's var and let are block-scoped: Javascript's let
	def String declarationKeyword(String kind) {
		if (kind == "const") "const" else "let"
	}

	def dispatch generate(VariableStatement stmt) '''
		«FOR vardecl: stmt.vardecls BEFORE declarationKeyword(stmt.kind) + ' ' SEPARATOR ', ' AFTER ';'»
			«vardecl.generate»«ENDFOR»
	'''
	
	def dispatch generate(FunctionDeclaration func) '''
		function «IF (func.id !== null)»«func.id.generate»«ENDIF»(«func.generateParams») {
			«func.parameterPrologue»«func.block.generate»
		}
		«func.id.keepName»
		
	''' 
	
	// A Javascript class: the same constructor and methods (tinyscript's classes have no extends);
	// implements becomes a check right after the class, as in the interpreter
	def dispatch generate(ClassDeclaration cls) '''
		class «cls.id.generate» {
			«FOR method : cls.methods»
				«method.name.generate»(«method.function.generateParams») {
					«method.function.parameterPrologue»«method.function.block.generate»
				}
			«ENDFOR»
		}
		«cls.id.keepName»
		«FOR reference : cls.interfaces»
			__ts_implements(«cls.id.generate», "«cls.id.name»", «reference.generate», "«reference.id.name»");
		«ENDFOR»
		
	'''

	// interface Shape { area(); }: a value with the names of the methods
	def dispatch generate(InterfaceDeclaration declaration) '''
		const «declaration.id.generate» = new __ts_Interface("«declaration.id.name»", [«FOR method : declaration.methods SEPARATOR ', '»"«TinyscriptModelUtil.propertyKey(method.name)»"«ENDFOR»]);
		
	'''

	// A Javascript arrow function, so that this is the one of the surrounding function
	def dispatch generate(ArrowFunction func) {
		val body = func.block
		if (body instanceof ArrowExpressionBody && !func.hasArrayPatternParameter)
			'''(«func.generateParams») => («(body.statements.head as ExpressionStatement).expr.generate»)'''
		else if (body instanceof ArrowExpressionBody) '''
			(«func.generateParams») => {
				«func.parameterPrologue»return («(body.statements.head as ExpressionStatement).expr.generate»);
			}'''
		else '''
			(«func.generateParams») => {
				«func.parameterPrologue»«body.generate»
			}'''
	}

	// An array pattern as parameter becomes a temporary parameter, taken apart at the start of the
	// body with the same check as in the interpreter; object patterns stay as they are. A default
	// stays in the parameter list, unless there is such a pattern: then it is set in the body,
	// after the patterns before it, which it may use
	def generateParams(FunctionDefinition func)
		'''«FOR i: 0 ..< func.params.size SEPARATOR ', '»«func.generateParam(i)»«ENDFOR»«IF func.rest !== null»«IF !func.params.empty», «ENDIF»...«func.rest.generate»«ENDIF»'''

	def CharSequence generateParam(FunctionDefinition func, int i) {
		val param = func.params.get(i)
		if (param instanceof ArrayPattern)
			'''__ts_param«i»'''
		else if (param instanceof Identifier && (param as Identifier).defaultValue !== null && !func.hasArrayPatternParameter)
			'''«param.generate» = «(param as Identifier).defaultValue.generate»'''
		else
			param.generate
	}

	def boolean hasArrayPatternParameter(FunctionDefinition func) {
		func.params.exists[it instanceof ArrayPattern]
	}

	def CharSequence parameterPrologue(FunctionDefinition func)
		'''«IF func.hasArrayPatternParameter»«FOR i: 0 ..< func.params.size»«IF func.params.get(i) instanceof ArrayPattern»let «func.params.get(i).generate» = __ts_pattern(__ts_param«i», «(func.params.get(i) as ArrayPattern).names.size»);
		«ELSEIF func.params.get(i) instanceof Identifier && (func.params.get(i) as Identifier).defaultValue !== null»if («func.params.get(i).generate» === undefined) «func.params.get(i).generate» = «(func.params.get(i) as Identifier).defaultValue.generate»;
		«ENDIF»«ENDFOR»«ENDIF»'''

	// A method of an object literal stays a method ({ area() { … } }): as in the interpreter, it is
	// no constructor. A renamed function keeps its name for print (function await() { … })
	def dispatch generate(FunctionDefinition func) {
		val code = '''
			«IF TinyscriptModelUtil.isMethodShorthand(func)»«ELSE»function «IF (func.id !== null)»«func.id.generate»«ENDIF»«ENDIF»(«func.generateParams») {
				«func.parameterPrologue»«func.block.generate»
			}'''
		if (func.id !== null && getName(func.id) != func.id.name)
			'''Object.defineProperty(«code», "name", { value: "«func.id.name»" })'''
		else
			code
	}
		
	def dispatch generate(ReturnStatement stmt) '''
		return«IF stmt.expr !== null» «stmt.expr.generate»«ENDIF»;
	'''
	
	def dispatch generate(WhileStatement stmt) '''
		while («stmt.cond.generate») {
			«stmt.^do.generate»
		}
	'''

	def dispatch generate(BreakStatement stmt) '''
		break;
	'''

	def dispatch generate(ContinueStatement stmt) '''
		continue;
	'''

	def dispatch generate(ThrowStatement stmt) '''
		throw __ts_throwable(«stmt.expr.generate»);
	'''

	def dispatch generate(TryStatement stmt) {
		val error = if (stmt.handler.param !== null) stmt.handler.param.generate else "__ts_error"
		'''
			try {
				«stmt.block.generate»
			}
			catch («error») {
				__ts_catchable(«error»);
				«stmt.handler.block.generate»
			}
		'''
	}

	def dispatch generate(AssertStatement stmt) '''
		assert «stmt.cond.generate»;
	'''
	
	def dispatch generate(IfStatement stmt) '''
		if («stmt.cond.generate») {
			«stmt.then.generate»
		}
		«IF stmt.^else?.elseIf !== null»
		else «stmt.^else.elseIf.generate»
		«ELSEIF stmt.^else !== null»
		else {
			«stmt.^else.^else.generate»
		}
		«ENDIF»
	'''
	
	def dispatch generate(ForStatement stmt) {
		generateIterableForStatement(stmt);
	}
	
	def private String newTempName(String purpose) {
		tempCounter = tempCounter + 1
		return "__ts_" + purpose + tempCounter
	}
	
	def generateIterableForStatement(ForStatement stmt) {
		val IterableForExpression expr=stmt.iterableForExpr; 
		val iterator = newTempName("iterator")
		val step = newTempName("step")
		val loopVarDefinition = if (stmt.id !== null) declarationKeyword(stmt.kind) + " " + stmt.id.generate
			else if (stmt.pattern !== null) declarationKeyword(stmt.kind) + " " + stmt.pattern.generate
			else stmt.ref.generate
		'''
		var «iterator» = __ts_iterate(«expr.iterable.generate»);
		for (var «step» = «iterator».next(); !«step».done; «step» = «iterator».next()) {
			«loopVarDefinition» = «stmt.pattern.generatePatternValue(step + ".value")»;
			«stmt.^do.generate»
		}
		'''
	}
	
	def dispatch generate(BinaryExpression expr) '''
		«IF (expr.left instanceof Pattern)»«expr.left.generate» = «(expr.left as Pattern).generatePatternValue(expr.right.generate)»«ELSEIF expr.op == BinaryOperator.ADD_ASSIGN»«expr.generateHelperAssignment("__ts_add(")»«ELSEIF expr.op.isBitwiseAssignment»«expr.generateHelperAssignment('__ts_bitwise("' + Operators.baseOf(expr.op).literal + '", ')»«ELSEIF Operators.isBitwise(expr.op)»__ts_bitwise("«expr.op.literal»", «expr.left.generate», «expr.right.generate»)«ELSEIF expr.op == BinaryOperator.ADD»__ts_add(«expr.left.generate», «expr.right.generate»)«ELSEIF expr.op == BinaryOperator.EQUALS»__ts_eq(«expr.left.generate», «expr.right.generate»)«ELSEIF expr.op == BinaryOperator.NOT_EQUALS»!__ts_eq(«expr.left.generate», «expr.right.generate»)«ELSE»«expr.left.generate» «expr.op.literal» «expr.right.generate»«ENDIF»'''

	// &=, |=, ^=, <<=, >>=: tinyscript's bitwise operators work on 53 bits, Javascript's on 32,
	// so the generator calls __ts_bitwise for them
	def boolean isBitwiseAssignment(BinaryOperator op) {
		val base = Operators.baseOf(op)
		base !== null && Operators.isBitwise(base)
	}

	// x += y adds like tinyscript's + (e.g. arrays), x ^= y etc. use the 53-bit bitwise operators
	// (helper is the start of the call, e.g. '__ts_add('); a property target is evaluated only once
	// delete o.key: like the interpreter, __ts_delete refuses arrays, Maps and Sets
	def generateDelete(Unary expr) {
		val access = expr.expr as CallOrPropertyAccess
		val property = (access.suffix as CallOrPropertyAccessSuffix).property
		val key = switch property {
			DotPropertyAccessSuffix case property.key.name !== null: '"' + property.key.name + '"'
			DotPropertyAccessSuffix: property.key.expr.generate.toString
			ComputedPropertyAccessSuffix: property.key.generate.toString
		}
		'''__ts_delete(«access.expr.generate», «key»)'''
	}

	def generateHelperAssignment(BinaryExpression expr, String helper) {
		val left = expr.left
		if (left instanceof CallOrPropertyAccess) {
			val suffix = left.suffix as CallOrPropertyAccessSuffix
			val property = suffix.property
			val key = switch property {
				DotPropertyAccessSuffix case property.key.name !== null: '"' + property.key.name + '"'
				DotPropertyAccessSuffix: property.key.expr.generate.toString
				ComputedPropertyAccessSuffix: property.key.generate.toString
			}
			return '''((o, k) => o[k] = «helper»o[k], «expr.right.generate»))(«left.expr.generate», «key»)'''
		}
		'''«left.generate» = «helper»«left.generate», «expr.right.generate»)'''
	}

	def dispatch generate(SpreadElement spread)
		'''...__ts_spread(«spread.expr.generate»)'''

	def dispatch generate(ArrayPattern pattern)
		'''[«FOR name: pattern.names SEPARATOR ', '»«name.generate»«ENDFOR»«IF pattern.rest !== null»«IF !pattern.names.empty», «ENDIF»...«pattern.rest.generate»«ENDIF»]'''

	// { row } takes the property row, also when the generator renames the variable
	def dispatch generate(ObjectPattern pattern)
		'''{ «FOR name: pattern.names SEPARATOR ', '»«IF name.generate != name.name»«name.name»: «ENDIF»«name.generate»«ENDFOR» }'''

	// The value for a declaration: an array pattern checks its value like the interpreter
	def CharSequence generatePatternValue(Pattern pattern, Object value) {
		if (pattern instanceof ArrayPattern)
			'''__ts_pattern(«value», «pattern.names.size»)'''
		else
			'''«value»'''
	}

	def dispatch generate(ConditionalExpression expr)
		'''(«expr.cond.generate» ? «expr.then.generate» : «expr.^else.generate»)'''

	// Nested unary operators get parentheses: "- -x" must not become the decrement "--x"
	def dispatch generate(Unary expr)
		'''«IF expr.op == UnaryOperator.DELETE»«expr.generateDelete»«ELSEIF expr.op == UnaryOperator.BIT_NOT»__ts_bitwise("~", «expr.expr.generate»)«ELSEIF expr.op == UnaryOperator.TYPEOF || expr.expr instanceof Unary»«expr.op.literal»(«expr.expr.generate»)«ELSE»«expr.op.literal»«expr.expr.generate»«ENDIF»'''
		
	def dispatch generate(NewExpression expr)
		'''new «expr.expr.generate»'''
		
	def dispatch generate(GroupingExpression expr)
		'''(«expr.expr.generate»)'''

	def dispatch generate(CallOrPropertyAccess expr) 
		'''«expr.expr.generate»«IF (expr.suffix !== null)»«expr.suffix.generate»«ENDIF»'''
	
	def dispatch generate(CallOrPropertyAccessSuffix suffix) 
		'''«suffix.property.generate»«IF (suffix.call !== null)»«suffix.call.generate»«ENDIF»'''

	def dispatch generate(CallSuffix suffix)
		'''(«FOR arg: suffix.arguments SEPARATOR ', '»«arg.generate»«ENDFOR»)'''
	
	def dispatch generate(ComputedPropertyAccessSuffix suffix)
		'''«IF suffix.optional»?.«ENDIF»[«suffix.key.generate»]'''
	
	def dispatch generate(DotPropertyAccessSuffix suffix)
		'''«IF suffix.optional»?.«ELSE».«ENDIF»«suffix.key.generate»'''
	
	def dispatch generate(PropertyName expr) 
		'''«IF expr.name !== null»«expr.name»«ELSE»«expr.expr.generate»«ENDIF»'''
	
	def dispatch generate(Identifier id) {
		getName(id);	
	}
	
	def dispatch generate(Reference ref) {
		if (ref.^this)
			"this"
		else
			getName(ref.id);
	}
	
	def dispatch generate(ObjectInitializer expr) {
		'''{«FOR assignment: expr.propertyassignments SEPARATOR ', '»«assignment.generate»«ENDFOR»}'''
	}
	
	// The shorthand { row } becomes row: row, since the generator may rename the variable
	def dispatch generate(PropertyAssignment expr) {
		if (expr.shorthand !== null)
			'''«expr.shorthand.id.name»: «expr.shorthand.generate»'''
		else if (expr.value instanceof FunctionDefinition && TinyscriptModelUtil.isMethodShorthand(expr.value as FunctionDefinition))
			'''«expr.key.generate»«expr.value.generate»'''
		else
			'''«expr.key.generate»: «expr.value.generate»'''
	}

	def dispatch generate(ArrayInitializer expr) {
		'''[«FOR element: expr.values SEPARATOR ', '»«element.generate»«ENDFOR»]'''
	}
	
	def dispatch generate(StringLiteral expr) {
		quote(expr.value)
	}
	
	// A Javascript string literal for value (the parser has already resolved the escapes)
	def private static String quote(String value) {
		val result = new StringBuilder("\"")
		for (c : value.toCharArray) {
			switch (c) {
				case c == '"'.charAt(0): result.append("\\\"")
				case c == '\\'.charAt(0): result.append("\\\\")
				case c == '\n'.charAt(0): result.append("\\n")
				case c == '\r'.charAt(0): result.append("\\r")
				case c == '\t'.charAt(0): result.append("\\t")
				case c < 0x20 || c == 0x2028 || c == 0x2029: result.append(String.format("\\u%04x", c as int))
				default: result.append(c)
			}
		}
		result.append("\"").toString
	}
	
	// The same digits as the interpreter prints (1e+300, not 301 digits)
	def dispatch generate(NumberLiteral expr) {
		TSNumber.format(expr.value)
	}
	
	def dispatch generate(BooleanLiteral expr) {
		String.valueOf(expr.value);	
	}

	def dispatch generate(NullObject expr) '''null'''

	def dispatch generate(UndefinedLiteral expr) '''undefined'''
	
	def String getName(Identifier id) {
		var String name = null;
		if (TinyscriptModelUtil.isShadowing(id))
			name = nameProvider.getFullyQualifiedName(id).toString("_")
		else {
			name = id.name
		}
		return if (JAVASCRIPT_RESERVED.contains(name)) "__ts_" + name else name;
	}

	// Names that tinyscript allows but Javascript (in strict mode) does not; the generator
	// renames them (default becomes __ts_default)
	static val JAVASCRIPT_RESERVED = #{"enum", "default", "do", "in", "void", "with", "super", "extends", "debugger",
		"static", "package", "private", "public", "protected", "yield", "eval", "arguments", "await"}

	// A renamed function or class keeps its name for print: [Function: default]
	def CharSequence keepName(Identifier id) {
		if (id !== null && getName(id) != id.name)
			'''Object.defineProperty(«getName(id)», "name", { value: "«id.name»" });'''
		else
			""
	}
	
}
