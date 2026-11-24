
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
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.nodemodel.util.NodeModelUtils
import de.mkbauer.tinyscript.ts.Expression
import de.mkbauer.tinyscript.ts.IncrementOperator
import de.mkbauer.tinyscript.ts.PropertyAccessSuffix

class TinyscriptGenerator   {
	
	@Inject
	TinyscriptQualifiedNameProvider nameProvider;
	
	// Checks of operators and conditions in the generated code, as the interpreter makes them, but
	// only where the operator does not already guarantee the kind (smart, no type inference): a
	// comparison, ==, !, && / || are booleans; -, *, /, % and unary - (checked themselves) and
	// number literals are numbers. Variables, properties and calls stay checked.
	def static boolean isBoolean(Expression e) {
		switch e {
			BooleanLiteral: true
			GroupingExpression: isBoolean(e.expr)
			Unary: e.op == UnaryOperator.NOT
			BinaryExpression: switch e.op {
				case LESS, case LESS_OR_EQUAL, case GREATER, case GREATER_OR_EQUAL, case EQUALS, case NOT_EQUALS,
					case INSTANCEOF, case AND, case OR: true
				default: false
			}
			default: false
		}
	}

	def static boolean isNumber(Expression e) {
		switch e {
			NumberLiteral: true
			GroupingExpression: isNumber(e.expr)
			Unary: e.op == UnaryOperator.NEGATE
			BinaryExpression: switch e.op {
				case SUBTRACT, case MULTIPLY, case DIVIDE, case REMAINDER: true
				default: false
			}
			default: false
		}
	}

	// A condition: checked by __ts_cond unless it is a boolean anyway
	def CharSequence condition(Expression e) {
		if (isBoolean(e)) e.generate else '''«mark(e)»__ts_cond(«e.generate»)'''
	}

	// The helper that checks a binary operator, or null for the others
	def static String checkingHelper(BinaryOperator op) {
		switch op {
			case SUBTRACT: "__ts_sub"
			case MULTIPLY: "__ts_mul"
			case DIVIDE: "__ts_div"
			case REMAINDER: "__ts_rem"
			case LESS: "__ts_lt"
			case LESS_OR_EQUAL: "__ts_le"
			case GREATER: "__ts_gt"
			case GREATER_OR_EQUAL: "__ts_ge"
			default: null
		}
	}

	// Counter for unique names of temporary variables (loop bounds, iterables, indexes)
	int tempCounter = 0;
	
	// With source maps: an error that ends the program is printed as by the interpreter, with the
	// tinyscript line (from the source map) and a stack of the tinyscript functions; the frames
	// of the prelude (__ts_ helpers) and of the standard library are left out
	def uncaughtErrorHandler() '''
		// (the checked String.prototype methods of the prelude take no regular expressions: only exec here)
		process.on("uncaughtException", function (error) {
			process.exitCode = 1;
			const name = error instanceof Error ? error.name : "Error";
			const message = error instanceof Error ? error.message : String(error);
			const frames = [];
			for (const line of globalThis.String(error && error.stack || "").split("\n")) {
				const frame = /^\s*at (?:(.*?) \()?(.*?):(\d+):(\d+)\)?$/.exec(line);
				if (frame === null || !frame[2].endsWith(".tys") || frame[2].startsWith("std:")) {
					continue;
				}
				const fn = frame[1] === undefined || frame[1] === "Object.<anonymous>" ? "global"
					: frame[1].startsWith("Object.") ? frame[1].slice(7) : frame[1];
				frames.push({ fn: fn, file: frame[2], line: +frame[3], column: +frame[4] });
			}
			if (frames.length === 0) {
				process.stderr.write((error && error.stack || String(error)) + "\n");
				return;
			}
			// node shows the files of the source map as paths, the map has their URIs
			const path = file => file.startsWith("file:") ? require("url").fileURLToPath(file) : file;
			let excerpt = "";
			try {
				const map = require("module").findSourceMap(__filename);
				const index = map.payload.sources.findIndex(source => path(source) === path(frames[0].file));
				const text = index >= 0 ? map.payload.sourcesContent[index] : null;
				let source = text === null ? undefined : text.split("\n")[frames[0].line - 1];
				if (source !== undefined) {
					source = source.endsWith("\r") ? source.slice(0, -1) : source;
					// Tabs stay, so that the marker lines up (the checked replace of the prelude takes no regex)
					const before = Array.from(source.slice(0, frames[0].column - 1), c => c === "\t" ? "\t" : " ").join("");
					excerpt = "    " + source + "\n    " + before + "^\n";
				}
			}
			catch (e) {
				// Without the source map, no excerpt
			}
			process.stderr.write(name + ": " + message + "\n" + excerpt
				+ frames.map(frame => "    at " + frame.fn + "(" + path(frame.file) + ":" + frame.line + ")\n").join(""));
		});
	'''

	def generateBuiltins() '''

		// A failed assert is a programming error: catch does not get it (see __ts_catchable)
		class __ts_AssertionError extends globalThis.Error {}
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

		// Numbers first, the most frequent case; the error in a function of its own, so that
		// __ts_add stays small enough for V8 to inline it
		function __ts_add(obj1, obj2) {
			if (typeof obj1 === "number" && typeof obj2 === "number") {
				return obj1 + obj2;
			}
			if (typeof obj1 === "string" || typeof obj2 === "string") {
				return __ts_str(obj1) + __ts_str(obj2);
			}
			if (Array.isArray(obj1) || Array.isArray(obj2)) {
				if (!(Array.isArray(obj1) && Array.isArray(obj2))) {
					throw new TypeError("'+' can only add an array to another array or to a string");
				}
				return obj1.concat(obj2);
			}
			return __ts_cannotAdd(obj1, obj2);
		}
		function __ts_cannotAdd(obj1, obj2) {
			throw new TypeError("'+' adds two numbers, two arrays, or a string and a value, not " + __ts_type(obj1) + " and " + __ts_type(obj2));
		}

		// Like the interpreter: - * / % and unary - need numbers; < <= > >= compare two numbers
		// (not NaN) or two strings; a condition (if, while, ?:, !, && / ||, assert, the result of
		// a callback of filter, find ...) is a boolean, null / undefined (false) or an object
		// (true). The generator calls them only where the kind is not certain (see smart checks)
		function __ts_numbers(op, a, b) {
			throw new TypeError("'" + op + "' needs two numbers, not " + __ts_type(a) + " and " + __ts_type(b));
		}
		function __ts_sub(a, b) { if (typeof a !== "number" || typeof b !== "number") __ts_numbers("-", a, b); return a - b; }
		function __ts_mul(a, b) { if (typeof a !== "number" || typeof b !== "number") __ts_numbers("*", a, b); return a * b; }
		function __ts_div(a, b) { if (typeof a !== "number" || typeof b !== "number") __ts_numbers("/", a, b); return a / b; }
		function __ts_rem(a, b) { if (typeof a !== "number" || typeof b !== "number") __ts_numbers("%", a, b); return a % b; }
		function __ts_neg(a) {
			if (typeof a !== "number") {
				throw new TypeError("'-' needs a number, not " + __ts_type(a));
			}
			return -a;
		}
		function __ts_inc(a) {
			if (typeof a !== "number") {
				throw new TypeError("'++' needs a number, not " + __ts_type(a));
			}
			return a + 1;
		}
		function __ts_dec(a) {
			if (typeof a !== "number") {
				throw new TypeError("'--' needs a number, not " + __ts_type(a));
			}
			return a - 1;
		}
		function __ts_comparable(a, b) {
			return (typeof a === "number" && typeof b === "number" && a === a && b === b) || (typeof a === "string" && typeof b === "string");
		}
		function __ts_compare(op, a, b) {
			if (typeof a === "number" && typeof b === "number") {
				throw new TypeError("'" + op + "' cannot compare NaN (e.g. from parseInt or Number of a text that is no number)");
			}
			if (!(__ts_isString(a) && __ts_isString(b))) {
				throw new TypeError("'" + op + "' compares two numbers or two strings, not " + __ts_type(a) + " and " + __ts_type(b));
			}
		}
		function __ts_lt(a, b) { if (!__ts_comparable(a, b)) __ts_compare("<", a, b); return a < b; }
		function __ts_le(a, b) { if (!__ts_comparable(a, b)) __ts_compare("<=", a, b); return a <= b; }
		function __ts_gt(a, b) { if (!__ts_comparable(a, b)) __ts_compare(">", a, b); return a > b; }
		function __ts_ge(a, b) { if (!__ts_comparable(a, b)) __ts_compare(">=", a, b); return a >= b; }
		function __ts_notACondition(x) {
			throw new TypeError("A condition must be a boolean, not " + typeof x + ": compare it, e.g. "
				+ (typeof x === "number" ? "x > 0" : "s != \"\""));
		}
		function __ts_cond(x) {
			if (typeof x === "boolean") {
				return x;
			}
			if (x === null || x === undefined) {
				return false;
			}
			if (typeof x === "object" || typeof x === "function") {
				return true;
			}
			return __ts_notACondition(x);
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
			const number = __ts_JsNumber(result);
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

		// Like the interpreter, the builtins check their arguments: types, integers and the
		// minimum number of arguments (not the maximum: callbacks such as map's get more arguments
		// than they use; too many arguments in a call are for the checks before the run). The
		// prelude itself uses the originals where it needs more (__ts_JsNumber). Replaced on the
		// global object (node runs the file as a module, where a var would be undefined before
		// this point), so the prelude above sees the originals while it is set up.
		const __ts_JsNumber = globalThis.Number;
		function __ts_type(value) {
			return value === null ? "object" : typeof value;
		}
		function __ts_describe(value) {
			return typeof value === "number" ? __ts_JsString(value) : __ts_type(value);
		}
		function __ts_isInteger(value) {
			return typeof value === "number" && Math.trunc(value) === value;
		}
		function __ts_isString(value) {
			return typeof value === "string" || value instanceof __ts_JsString;
		}
		function __ts_atLeast(count, min, name) {
			if (count < min) {
				throw new TypeError("Builtin function " + name + " requires at least " + min + " arguments.");
			}
		}
		function __ts_integer(value, name) {
			if (!__ts_isInteger(value)) {
				throw new TypeError(name + "() needs an integer, not " + __ts_describe(value));
			}
		}
		function __ts_string(value, name) {
			if (!__ts_isString(value)) {
				throw new TypeError(name + "() needs a string, not " + __ts_type(value));
			}
		}
		function __ts_mathNumber(value) {
			if (typeof value !== "number") {
				throw new TypeError("Math functions need numbers, not " + __ts_type(value));
			}
		}
		function __ts_object(value, name) {
			if (value === null || (typeof value !== "object" && typeof value !== "function")) {
				throw new TypeError(name + "() needs an object, not " + __ts_type(value));
			}
		}
		function __ts_thisArray(self, name) {
			if (!Array.isArray(self)) {
				throw new TypeError("Function Array.prototype." + name + " only works for Array objects.");
			}
		}
		// Replaces target[name] by checked (a method, so new fails as in the interpreter)
		function __ts_replace(target, name, checked) {
			Object.defineProperty(target, name, { value: checked, writable: true, configurable: true, enumerable: false });
		}

		(function () {
			const A = Array.prototype;
			const at = A.at, indexOf = A.indexOf, includes = A.includes, lastIndexOf = A.lastIndexOf, push = A.push,
				unshift = A.unshift, join = A.join, sort = A.sort, slice = A.slice, fill = A.fill, splice = A.splice, flat = A.flat;
			__ts_replace(A, "at", { at(index) {
				__ts_atLeast(arguments.length, 1, "at");
				__ts_thisArray(this, "at");
				if (!__ts_isInteger(index)) {
					throw new TypeError("at() needs an integer index, not " + __ts_str(index));
				}
				// Not at.call(this, index): V8 inlines an element access, not a call of the original
				const i = index < 0 ? this.length + index : index;
				return i >= 0 && i < this.length ? this[i] : undefined;
			} }.at);
			// indexOf(x[, start]), includes, lastIndexOf: the index must be an integer. Each wrapper
			// is a function of its own (not made in a loop), so that V8 sees one original per call
			«FOR name : #["indexOf", "includes", "lastIndexOf"]»
				__ts_replace(A, "«name»", { «name»(value, start) {
					__ts_atLeast(arguments.length, 1, "«name»");
					__ts_thisArray(this, "«name»");
					if (arguments.length > 1 && !__ts_isInteger(start)) {
						throw new TypeError(«IF name == "lastIndexOf"»"lastIndexOf() needs an integer index, not " + __ts_describe(start)«ELSE»"indexOf() needs an integer start index, not " + __ts_str(start)«ENDIF»);
					}
					return arguments.length > 1 ? «name».call(this, value, start) : «name».call(this, value);
				} }.«name»);
			«ENDFOR»
			«FOR name : #["push", "unshift"]»
				__ts_replace(A, "«name»", { «name»(item) {
					__ts_atLeast(arguments.length, 1, "«name»");
					__ts_thisArray(this, "«name»");
					return arguments.length === 1 ? «name».call(this, item) : «name».apply(this, arguments);
				} }.«name»);
			«ENDFOR»
			// The result of the callback is a condition
			«FOR name : #["filter", "find", "findIndex", "some", "every", "findLast", "findLastIndex"]»
				const «name» = A.«name»;
				__ts_replace(A, "«name»", { «name»(f) {
					__ts_atLeast(arguments.length, 1, "«name»");
					__ts_thisArray(this, "«name»");
					if (typeof f !== "function") {
						throw new TypeError("«name»() needs a function, not " + __ts_type(f));
					}
					return «name».call(this, (item, index, array) => __ts_cond(f(item, index, array)));
				} }.«name»);
			«ENDFOR»
			__ts_replace(A, "join", { join(separator) {
				__ts_thisArray(this, "join");
				if (separator !== undefined && typeof separator !== "string") {
					throw new TypeError("join() needs a string as separator, not " + __ts_type(separator));
				}
				return join.call(this, separator === undefined ? "," : separator);
			} }.join);
			// Without a comparison function only strings; the comparison function returns numbers
			__ts_replace(A, "sort", { sort(compare) {
				__ts_thisArray(this, "sort");
				if (compare === undefined) {
					for (const item of this) {
						if (typeof item !== "string") {
							throw new TypeError("sort() without a comparison function only sorts strings, not " + __ts_type(item));
						}
					}
					return sort.call(this);
				}
				if (typeof compare !== "function") {
					throw new TypeError("sort() needs a function, not " + __ts_type(compare));
				}
				return sort.call(this, (a, b) => {
					const result = compare(a, b);
					if (typeof result !== "number") {
						throw new TypeError("The comparison function of sort() must return a number");
					}
					return result;
				});
			} }.sort);
			// slice, fill, splice: integer indexes
			const indexes = (args, from, to, name) => {
				for (let i = from; i < Math.min(args.length, to); i++) {
					if (!__ts_isInteger(args[i])) {
						throw new TypeError(name + "() needs integer indexes, not " + __ts_describe(args[i]));
					}
				}
			};
			__ts_replace(A, "slice", { slice(start, end) {
				__ts_thisArray(this, "slice");
				if ((arguments.length > 0 && !__ts_isInteger(start)) || (arguments.length > 1 && !__ts_isInteger(end))) {
					indexes(arguments, 0, 2, "slice");
				}
				return arguments.length === 0 ? slice.call(this) : arguments.length === 1 ? slice.call(this, start) : slice.call(this, start, end);
			} }.slice);
			__ts_replace(A, "fill", { fill(value) {
				__ts_atLeast(arguments.length, 1, "fill");
				__ts_thisArray(this, "fill");
				indexes(arguments, 1, 3, "fill");
				return fill.apply(this, arguments);
			} }.fill);
			__ts_replace(A, "splice", { splice(start, count) {
				__ts_thisArray(this, "splice");
				indexes(arguments, 0, 1, "splice");
				if (arguments.length > 1 && !__ts_isInteger(count)) {
					throw new TypeError("splice() needs an integer count, not " + __ts_describe(count));
				}
				return arguments.length === 0 ? splice.call(this, this.length) : splice.apply(this, arguments);
			} }.splice);
			__ts_replace(A, "flat", { flat(depth) {
				__ts_thisArray(this, "flat");
				if (depth !== undefined && (typeof depth !== "number" || depth < 0 || !(__ts_isInteger(depth) || depth === Infinity))) {
					throw new TypeError("flat() needs a depth of 0 or more, not " + __ts_describe(depth));
				}
				return flat.call(this, depth === undefined ? 1 : depth);
			} }.flat);
		})();

		(function () {
			const S = __ts_JsString.prototype;
			// Methods with an integer and methods with a string as first argument, and an optional
			// integer position
			«FOR name : #["at", "charAt", "charCodeAt", "codePointAt"]»
				const «name» = S.«name»;
				__ts_replace(S, "«name»", { «name»(index) {
					__ts_atLeast(arguments.length, 1, "«name»");
					if (!__ts_isInteger(index)) {
						throw new TypeError(«IF name == "at"»"at() needs an integer index, not " + __ts_str(index)«ELSE»"«name»() needs an integer, not " + __ts_describe(index)«ENDIF»);
					}
					return «name».call(this, index);
				} }.«name»);
			«ENDFOR»
			«FOR name : #["indexOf", "lastIndexOf", "includes", "startsWith", "endsWith"]»
				const «name» = S.«name»;
				__ts_replace(S, "«name»", { «name»(search, position) {
					__ts_atLeast(arguments.length, 1, "«name»");
					__ts_string(search, "«name»");
					if (arguments.length > 1 && !__ts_isInteger(position)) {
						throw new TypeError(«IF name == "indexOf"»"indexOf() needs an integer start index, not " + __ts_str(position)«ELSE»"«name»() needs an integer, not " + __ts_describe(position)«ENDIF»);
					}
					return arguments.length > 1 ? «name».call(this, __ts_JsString(search), position) : «name».call(this, __ts_JsString(search));
				} }.«name»);
			«ENDFOR»
			«FOR name : #["substring", "slice"]»
				const «name» = S.«name»;
				__ts_replace(S, "«name»", { «name»(start, end) {
					«IF name == "substring"»
						__ts_atLeast(arguments.length, 1, "substring");
					«ENDIF»
					if (arguments.length > 0) {
						__ts_integer(start, "«name»");
					}
					if (arguments.length > 1) {
						__ts_integer(end, "«name»");
					}
					return «name».apply(this, arguments);
				} }.«name»);
			«ENDFOR»
			const split = S.split;
			__ts_replace(S, "split", { split(separator, limit) {
				__ts_atLeast(arguments.length, 1, "split");
				__ts_string(separator, "split");
				if (arguments.length > 1) {
					__ts_integer(limit, "split");
					if (limit < 0) {
						throw new TypeError("split() needs a non-negative limit, not " + limit);
					}
				}
				return arguments.length > 1 ? split.call(this, __ts_JsString(separator), limit) : split.call(this, __ts_JsString(separator));
			} }.split);
			«FOR name : #["replace", "replaceAll"]»
				const «name» = S.«name»;
				__ts_replace(S, "«name»", { «name»(search, replacement) {
					__ts_atLeast(arguments.length, 2, "«name»");
					__ts_string(search, "«name»");
					__ts_string(replacement, "«name»");
					return «name».call(this, __ts_JsString(search), __ts_JsString(replacement));
				} }.«name»);
			«ENDFOR»
			const repeat = S.repeat;
			__ts_replace(S, "repeat", { repeat(count) {
				__ts_atLeast(arguments.length, 1, "repeat");
				__ts_integer(count, "repeat");
				if (count < 0) {
					throw new TypeError("repeat() needs a non-negative count, not " + count);
				}
				return repeat.call(this, count);
			} }.repeat);
			«FOR name : #["padStart", "padEnd"]»
				const «name» = S.«name»;
				__ts_replace(S, "«name»", { «name»(length, padding) {
					__ts_atLeast(arguments.length, 1, "«name»");
					__ts_integer(length, "«name»");
					if (arguments.length > 1) {
						__ts_string(padding, "«name»");
						return «name».call(this, length, __ts_JsString(padding));
					}
					return «name».call(this, length);
				} }.«name»);
			«ENDFOR»
			const fromCharCode = __ts_JsString.fromCharCode;
			String.fromCharCode = { fromCharCode(...codes) {
				for (const code of codes) {
					__ts_integer(code, "fromCharCode");
					if (code < 0 || code > 0xFFFF) {
						throw new TypeError("fromCharCode() needs character codes from 0 to 65535, not " + code);
					}
				}
				return fromCharCode(...codes);
			} }.fromCharCode;
		})();

		(function () {
			const N = __ts_JsNumber.prototype;
			const toFixed = N.toFixed, toString = N.toString;
			// An optional integer argument from min to max
			const optional = (value, min, max, message) => {
				if (value !== undefined && (!__ts_isInteger(value) || value < min || value > max)) {
					throw new TypeError(message + ", not " + __ts_describe(value));
				}
			};
			__ts_replace(N, "toFixed", { toFixed(digits) {
				optional(digits, 0, 100, "toFixed() needs a number of digits from 0 to 100");
				return toFixed.call(this, digits === undefined ? 0 : digits);
			} }.toFixed);
			__ts_replace(N, "toString", { toString(radix) {
				optional(radix, 2, 36, "toString() needs a radix from 2 to 36");
				if (radix !== undefined && radix !== 10 && !(Number.isFinite(this.valueOf()) && Math.trunc(this) === this.valueOf())) {
					throw new TypeError("toString(" + radix + ") needs an integer, not " + __ts_JsString(this.valueOf()));
				}
				return radix === undefined ? toString.call(this) : toString.call(this, radix);
			} }.toString);
		})();

		// Number(x) only for numbers, strings and booleans (a function, as in the interpreter not a
		// constructor); parseInt, parseFloat and isNaN without conversions
		globalThis.Number = (function (JsNumber) {
			const N = { Number(value) {
				__ts_atLeast(arguments.length, 1, "Number");
				if (typeof value !== "number" && typeof value !== "string" && typeof value !== "boolean") {
					throw new TypeError("Number() needs a string, a number or a boolean, not " + __ts_type(value));
				}
				return JsNumber(value);
			} }.Number;
			for (const key of Object.getOwnPropertyNames(JsNumber)) {
				if (!(key in N) || key === "prototype") {
					Object.defineProperty(N, key, Object.getOwnPropertyDescriptor(JsNumber, key));
				}
			}
			return N;
		})(__ts_JsNumber);
		globalThis.parseInt = (function (jsParseInt) {
			return { parseInt(text, radix) {
				__ts_atLeast(arguments.length, 1, "parseInt");
				if (typeof text !== "string") {
					throw new TypeError("parseInt() needs a string, not " + __ts_type(text));
				}
				if (radix !== undefined && (!__ts_isInteger(radix) || radix < 2 || radix > 36)) {
					throw new TypeError("parseInt() needs a radix from 2 to 36, not " + __ts_str(radix));
				}
				return radix === undefined ? jsParseInt(text) : jsParseInt(text, radix);
			} }.parseInt;
		})(globalThis.parseInt);
		globalThis.parseFloat = (function (jsParseFloat) {
			return { parseFloat(text) {
				__ts_atLeast(arguments.length, 1, "parseFloat");
				if (typeof text !== "string") {
					throw new TypeError("parseFloat() needs a string, not " + __ts_type(text));
				}
				return jsParseFloat(text);
			} }.parseFloat;
		})(globalThis.parseFloat);
		globalThis.isNaN = { isNaN(value) {
			__ts_atLeast(arguments.length, 1, "isNaN");
			if (typeof value !== "number") {
				throw new TypeError("isNaN() needs a number, not " + __ts_type(value));
			}
			return value !== value;
		} }.isNaN;

		// Math: numbers only
		(function () {
			«FOR name : #["sqrt", "round", "abs", "floor", "ceil", "trunc", "sign", "cbrt", "exp", "log", "log2", "log10", "sin", "cos", "tan", "asin", "acos", "atan"]»
				const «name» = Math.«name»;
				Math.«name» = { «name»(x) {
					__ts_atLeast(arguments.length, 1, "«name»");
					__ts_mathNumber(x);
					return «name»(x);
				} }.«name»;
			«ENDFOR»
			«FOR name : #["pow", "atan2"]»
				const «name» = Math.«name»;
				Math.«name» = { «name»(x, y) {
					__ts_atLeast(arguments.length, 2, "«name»");
					__ts_mathNumber(x);
					__ts_mathNumber(y);
					return «name»(x, y);
				} }.«name»;
			«ENDFOR»
			«FOR name : #["min", "max", "hypot"]»
				const «name» = Math.«name»;
				Math.«name» = { «name»(a, b) {
					for (let i = 0; i < arguments.length; i++) {
						__ts_mathNumber(arguments[i]);
					}
					return arguments.length === 2 ? «name»(a, b) : «name».apply(Math, arguments);
				} }.«name»;
			«ENDFOR»
		})();

		// Object.values / entries / assign need objects, fromEntries [key, value] pairs or a Map
		«FOR name : #["values", "entries"]»
			Object.«name» = (function («name») {
				return { «name»(object) {
					__ts_atLeast(arguments.length, 1, "«name»");
					__ts_object(object, "«name»");
					return «name»(object);
				} }.«name»;
			})(Object.«name»);
		«ENDFOR»
		Object.assign = (function (assign) {
			return { assign(target) {
				__ts_atLeast(arguments.length, 1, "assign");
				__ts_object(target, "assign");
				for (let i = 1; i < arguments.length; i++) {
					if (arguments[i] != null) {
						__ts_object(arguments[i], "assign");
					}
				}
				return assign.apply(Object, arguments);
			} }.assign;
		})(Object.assign);
		Object.fromEntries = (function (fromEntries) {
			return { fromEntries(pairs) {
				__ts_atLeast(arguments.length, 1, "fromEntries");
				if (pairs instanceof globalThis.Map) {
					return fromEntries(pairs);
				}
				if (!Array.isArray(pairs)) {
					throw new TypeError("fromEntries() needs an array of [key, value] pairs or a Map, not " + __ts_type(pairs));
				}
				for (const pair of pairs) {
					if (!Array.isArray(pair) || pair.length < 2) {
						throw new TypeError("fromEntries() needs [key, value] pairs");
					}
					if (typeof pair[0] !== "string" && typeof pair[0] !== "number") {
						throw new TypeError("A property key must be a string or a number, not " + __ts_type(pair[0]));
					}
				}
				return fromEntries(pairs);
			} }.fromEntries;
		})(Object.fromEntries);

		// new Error(message): the message is a string. The prototype stays Error.prototype, so
		// instanceof Error holds for all errors, as before
		globalThis.Error = (function (JsError) {
			const E = function Error(message) {
				if (message !== undefined && typeof message !== "string") {
					throw new TypeError("Error() needs a string as message, not " + __ts_type(message));
				}
				return Reflect.construct(JsError, arguments.length > 0 ? [message] : [], new.target || E);
			};
			E.prototype = JsError.prototype;
			return E;
		})(globalThis.Error);
		
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
		sources = new LinkedHashMap
		modules = new LinkedHashMap
		moduleVariables = new HashMap
		importedModules = new HashMap
		val loading = new ArrayDeque<URI>
		loading.push(script.eResource.URI)
		collectModules(script, loader, loading)
		val code = generateScript(script, includeBuiltins).toString
		if (!sourceMaps)
			return code
		if (!includeBuiltins)
			return SourceMaps.strip(code)
		SourceMaps.attach(code, sources.keySet.map[sourceName].toList, sources.keySet.map[sourceText].toList)
	}

	// Source maps (TinyscriptGenerator.setSourceMaps): a marker with the tinyscript position before
	// each statement, call or property access and checked operator, see SourceMaps
	boolean sourceMaps = false

	/**
	 * With source maps, a program (with the builtins) gets an inline source map, and errors that
	 * end it are printed with the tinyscript line, as by the interpreter; node needs
	 * --enable-source-maps for it.
	 */
	def void setSourceMaps(boolean sourceMaps) {
		this.sourceMaps = sourceMaps
	}

	// The resources of the markers, numbered in the order of their first marker
	LinkedHashMap<Resource, Integer> sources = new LinkedHashMap

	def String mark(EObject element) {
		if (!sourceMaps || element === null)
			return ""
		val node = NodeModelUtils.findActualNodeFor(element)
		val resource = element.eResource
		if (node === null || resource === null)
			return ""
		var index = sources.get(resource)
		if (index === null) {
			index = sources.size
			sources.put(resource, index)
		}
		val text = node.rootNode.text
		val offset = node.offset
		SourceMaps.marker(index, node.startLine - 1, offset - (text.lastIndexOf('\n', offset - 1) + 1))
	}

	// A file as its URI (as node shows it), a module of the standard library as std:name (it may
	// be inside a jar), so that the error handler leaves its frames out
	def private String sourceName(Resource resource) {
		val uri = resource.URI
		if (Modules.isStandardLibrary(uri))
			"std:" + uri.lastSegment.substring(0, uri.lastSegment.length - ".tys".length)
		else
			uri.toString
	}

	def private String sourceText(Resource resource) {
		val root = resource.contents.head
		val node = if (root === null) null else NodeModelUtils.getNode(root)
		if (node === null) null else node.rootNode.text
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
			«IF sourceMaps»
				«uncaughtErrorHandler»
			«ENDIF»
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
							var lines = text.split("\n").map(function (line) { return line.endsWith("\r") ? line.slice(0, -1) : line; });
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
			«mark(stmt)»«stmt.generate»
		«ENDFOR»
	'''

	def dispatch generate(BlockStatement stmt) '''
		{
			«stmt.block.generate»
		}
	'''
	
	def dispatch generate(Block block) '''
		«FOR stmt: block.statements»
			«mark(stmt)»«stmt.generate»
		«ENDFOR»
	'''

	// i++; and i--; check that i is a number (__ts_inc / __ts_dec); a property is evaluated once
	def dispatch generate(ExpressionStatement stmt) {
		val increment = Operators.increment(stmt)
		if (increment === null)
			return '''
				«stmt.expr.generate»;
			'''
		val helper = if (increment == IncrementOperator.INCREMENT) "__ts_inc" else "__ts_dec"
		val target = stmt.expr
		if (target instanceof CallOrPropertyAccess) {
			return '''
				((o, k) => o[k] = «helper»(o[k]))(«target.expr.generate», «propertyKey((target.suffix as CallOrPropertyAccessSuffix).property)»);
			'''
		}
		'''
			«target.generate» = «helper»(«target.generate»);
		'''
	}

	// The key of o.key or o[key] as a Javascript expression
	def String propertyKey(PropertyAccessSuffix property) {
		switch property {
			DotPropertyAccessSuffix case property.key.name !== null: '"' + property.key.name + '"'
			DotPropertyAccessSuffix: property.key.expr.generate.toString
			ComputedPropertyAccessSuffix: property.key.generate.toString
		}
	}
	
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
		while («condition(stmt.cond)») {
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

	// assert(…) also for assert x == y; without parentheses
	def dispatch generate(AssertStatement stmt) '''
		assert(«condition(stmt.cond)»);
	'''
	
	def dispatch generate(IfStatement stmt) '''
		if («condition(stmt.cond)») {
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
	
	// - * / % < <= > >= (and -= *= /= %=) call a checking helper, unless both operands are
	// numbers anyway; && and || check their operands as conditions and give a boolean
	def dispatch generate(BinaryExpression expr) {
		if (!(expr.left instanceof Pattern)) {
			val helper = checkingHelper(expr.op)
			if (helper !== null && !(isNumber(expr.left) && isNumber(expr.right)))
				return '''«mark(expr)»«helper»(«expr.left.generate», «expr.right.generate»)'''
			val base = Operators.baseOf(expr.op)
			if (base !== null && checkingHelper(base) !== null)
				return expr.generateHelperAssignment(checkingHelper(base) + "(")
			if (expr.op == BinaryOperator.AND || expr.op == BinaryOperator.OR)
				return '''«condition(expr.left)» «expr.op.literal» «condition(expr.right)»'''
		}
		generateUnchecked(expr)
	}

	def generateUnchecked(BinaryExpression expr) '''
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
		'''(«condition(expr.cond)» ? «expr.then.generate» : «expr.^else.generate»)'''

	// Nested unary operators get parentheses: "- -x" must not become the decrement "--x"
	// Unary - is checked unless its operand is a number anyway; ! checks its operand as a condition
	def dispatch generate(Unary expr) {
		if (expr.op == UnaryOperator.NEGATE && !isNumber(expr.expr))
			return '''«mark(expr)»__ts_neg(«expr.expr.generate»)'''
		if (expr.op == UnaryOperator.NOT)
			return '''!«IF isBoolean(expr.expr)»«expr.expr.generate»«ELSE»__ts_cond(«expr.expr.generate»)«ENDIF»'''
		generateUnchecked(expr)
	}

	def generateUnchecked(Unary expr)
		'''«IF expr.op == UnaryOperator.DELETE»«expr.generateDelete»«ELSEIF expr.op == UnaryOperator.BIT_NOT»__ts_bitwise("~", «expr.expr.generate»)«ELSEIF expr.op == UnaryOperator.TYPEOF || expr.expr instanceof Unary»«expr.op.literal»(«expr.expr.generate»)«ELSE»«expr.op.literal»«expr.expr.generate»«ENDIF»'''
		
	def dispatch generate(NewExpression expr)
		'''new «expr.expr.generate»'''
		
	def dispatch generate(GroupingExpression expr)
		'''(«expr.expr.generate»)'''

	def dispatch generate(CallOrPropertyAccess expr) 
		'''«mark(expr)»«expr.expr.generate»«IF (expr.suffix !== null)»«expr.suffix.generate»«ENDIF»'''
	
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
