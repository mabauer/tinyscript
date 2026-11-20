
package de.mkbauer.tinyscript.generator

import de.mkbauer.tinyscript.ts.Tinyscript
import de.mkbauer.tinyscript.ts.VariableStatement
import de.mkbauer.tinyscript.ts.BinaryExpression
import de.mkbauer.tinyscript.ts.Identifier
import de.mkbauer.tinyscript.ts.Reference
import de.mkbauer.tinyscript.ts.AssertStatement
import de.mkbauer.tinyscript.ts.BreakStatement
import de.mkbauer.tinyscript.ts.ContinueStatement
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
import de.mkbauer.tinyscript.ts.NumericForExpression
import de.mkbauer.tinyscript.ts.IterableForExpression
import de.mkbauer.tinyscript.ts.ArrowFunction
import de.mkbauer.tinyscript.ts.ArrowExpressionBody
import de.mkbauer.tinyscript.ts.NullObject
import de.mkbauer.tinyscript.interpreter.ModuleLoader
import de.mkbauer.tinyscript.interpreter.Modules
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

		function assert(condition) {
			if (!condition) {
				message = "Assertion failed";
				if (typeof Error !== "undefined") {
					throw new Error(message);
				}
			throw message; // Fallback
			}
		}
		
		var System = {};
		System.currentTimeMillis = Date.now;
		// Like the interpreter: the arguments of the script
		System.args = typeof process !== "undefined" ? process.argv.slice(2) : [];
		
		if (typeof print !== "function") {
			// Like the interpreter: all arguments, concatenated, on one line
			print = function() {
				var line = "";
				for (var i = 0; i < arguments.length; i++) {
					line = line + String(arguments[i]);
				}
				console.log(line);
			};
		} 
		
		function __ts_add(obj1, obj2) {
			if (typeof obj1 === "string" || typeof obj2 === "string") {
				return obj1 + obj2;
			}
			if (Array.isArray(obj1) || Array.isArray(obj2)) {
				if (!(Array.isArray(obj1) && Array.isArray(obj2))) {
					throw new TypeError("'+' can only add an array to another array or to a string");
				}
				return obj1.concat(obj2);
			}
			return obj1 + obj2;
		}
		
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
					// Native modules (std:fs) have no script; they are emitted as Javascript directly
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
	
	def private generateScript(Tinyscript script, boolean includeBuiltins) '''
		«IF includeBuiltins»
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
		«generateImports(script)»
		«script.generate»
	'''
	
	// std:fs for node, with the same behavior as FileSystemModule (UTF-8, errors end the script)
	def private generateNativeModule(URI module) {
		switch (module.opaquePart) {
			case "fs": '''
				(function () {
					var nodefs = require("fs");
					return Object.freeze({
						exists: function (path) { return nodefs.existsSync(path); },
						readFile: function (path) { return nodefs.readFileSync(path, "utf8"); },
						readLines: function (path) {
							var text = nodefs.readFileSync(path, "utf8");
							if (text === "") { return []; }
							var lines = text.split(/\r?\n/);
							if (lines[lines.length - 1] === "") { lines.pop(); }
							return lines;
						},
						writeFile: function (path, text) { nodefs.writeFileSync(path, text, "utf8"); },
						writeLines: function (path, lines) { nodefs.writeFileSync(path, lines.map(function (line) { return line + "\n"; }).join(""), "utf8"); },
						appendFile: function (path, text) { nodefs.appendFileSync(path, text, "utf8"); },
						listDir: function (path) { return nodefs.readdirSync(path).sort(); }
					});
				})()'''
			default: throw new IllegalArgumentException("Not a native module: " + module)
		}
	}
	
	def private generateImports(Tinyscript script) '''
		«FOR importStatement : script.imports»
			«val module = moduleVariables.get(importedModules.get(importStatement))»
			«IF importStatement.namespace !== null»
				var «importStatement.namespace.generate» = «module»;
			«ENDIF»
			«FOR name : importStatement.names»
				var «name.generate» = «module».«name.name»;
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
		«stmt.expr.generate»«IF stmt.increment !== null»«stmt.increment»«ENDIF»;
	'''
	
	def dispatch generate(VariableStatement stmt) '''
		«FOR vardecl: stmt.vardecls BEFORE 'var ' SEPARATOR ', ' AFTER ';'»
			«vardecl.generate»«ENDFOR»
	'''
	
	def dispatch generate(FunctionDeclaration func) '''
		function «IF (func.id !== null)»«func.id.generate»«ENDIF»(«func.generateParams») {
			«func.block.generate»
		}
		
	''' 
	
	// A Javascript arrow function, so that this is the one of the surrounding function
	def dispatch generate(ArrowFunction func) {
		val body = func.block
		if (body instanceof ArrowExpressionBody)
			'''(«func.generateParams») => («(body.statements.head as ExpressionStatement).expr.generate»)'''
		else '''
			(«func.generateParams») => {
				«body.generate»
			}'''
	}

	def generateParams(FunctionDefinition func)
		'''«FOR param: func.params SEPARATOR ', '»«param.generate»«ENDFOR»«IF func.rest !== null»«IF !func.params.empty», «ENDIF»...«func.rest.generate»«ENDIF»'''
	
	def dispatch generate(FunctionDefinition func) '''
		function «IF (func.id !== null)»«func.id.name»«ENDIF»(«func.generateParams») {
			«func.block.generate»
		}'''	
		
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
		if (stmt.numericForExpr !== null) {
			generateNumericForStatement(stmt);
		}
		else { 
			generateIterableForStatement(stmt);
		}
	}
	
	def generateNumericForStatement(ForStatement stmt) {
		val NumericForExpression expr=stmt.numericForExpr; 
		var String loopVar; 
		var String loopVarDefinition;  
		if (stmt.id !== null) {
			loopVar = stmt.id.generate as String
			loopVarDefinition = "var " + loopVar;
		}
		else {
			loopVar = stmt.ref.generate as String
			loopVarDefinition = loopVar;
		}
		// Like the interpreter, evaluate the bounds once, in the order start, stop, step
		val preamble = new StringBuilder
		val start = evaluateOnce(expr.start, "start", preamble)
		val stop = evaluateOnce(expr.stop, "stop", preamble)
		val step = if (expr.step !== null) evaluateOnce(expr.step, "step", preamble)
		'''
		«preamble»
		«IF (step === null)»
			for («loopVarDefinition» = «start»; «loopVar» <= «stop»; «loopVar» = «loopVar» + 1) {
				«stmt.^do.generate»
			}
		«ELSE»
			for («loopVarDefinition» = «start»; («step» > 0)?(«loopVar» <= «stop»):(«loopVar» >= «stop»); «loopVar» = «loopVar» + «step») {
				«stmt.^do.generate»
			}
		«ENDIF»'''		
	}
	
	/**
	 * Returns the code for an expression that must be evaluated only once: literals
	 * inline, anything else via a temporary variable declared in the preamble.
	 */
	def private String evaluateOnce(Expression expr, String purpose, StringBuilder preamble) {
		if (expr instanceof NumberLiteral || (expr instanceof Unary && (expr as Unary).expr instanceof NumberLiteral))
			return expr.generate.toString
		val name = newTempName(purpose)
		preamble.append("var " + name + " = " + expr.generate + ";\n")
		return name
	}
	
	def private String newTempName(String purpose) {
		tempCounter = tempCounter + 1
		return "__ts_" + purpose + tempCounter
	}
	
	def generateIterableForStatement(ForStatement stmt) {
		val IterableForExpression expr=stmt.iterableForExpr; 
		val iterable = newTempName("iterable")
		val index = newTempName("i")
		val loopVarDefinition = if (stmt.id !== null) "var " + stmt.id.generate else stmt.ref.generate
		'''
		var «iterable» = «expr.iterable.generate»;
		if (typeof «iterable» === "string" || «iterable» instanceof String) {
			«iterable» = String(«iterable»).split("");
		}
		else if («iterable» instanceof globalThis.Map || «iterable» instanceof globalThis.Set) {
			«iterable» = Array.from(«iterable»);
		}
		else if (!Array.isArray(«iterable»)) {
			throw new TypeError("for needs an array, a string, a Map or a Set");
		}
		for (var «index» = 0; «index» < «iterable».length; «index»++) {
			«loopVarDefinition» = «iterable»[«index»];
			«stmt.^do.generate»
		}
		'''
	}
	
	def dispatch generate(BinaryExpression expr) '''
		«IF (expr.op.equals("+="))»«expr.generateAddAssignment»«ELSEIF (expr.op.equals("+"))»__ts_add(«expr.left.generate», «expr.right.generate»)«ELSEIF (expr.op.equals("=="))»__ts_eq(«expr.left.generate», «expr.right.generate»)«ELSEIF (expr.op.equals("!="))»!__ts_eq(«expr.left.generate», «expr.right.generate»)«ELSE»«expr.left.generate» «expr.op» «expr.right.generate»«ENDIF»'''

	// x += y adds like tinyscript's + (e.g. arrays); a property target is evaluated only once
	def generateAddAssignment(BinaryExpression expr) {
		val left = expr.left
		if (left instanceof CallOrPropertyAccess) {
			val suffix = left.suffix as CallOrPropertyAccessSuffix
			val property = suffix.property
			val key = switch property {
				DotPropertyAccessSuffix case property.key.name !== null: '"' + property.key.name + '"'
				DotPropertyAccessSuffix: property.key.expr.generate.toString
				ComputedPropertyAccessSuffix: property.key.generate.toString
			}
			return '''((o, k) => o[k] = __ts_add(o[k], «expr.right.generate»))(«left.expr.generate», «key»)'''
		}
		'''«left.generate» = __ts_add(«left.generate», «expr.right.generate»)'''
	}

	def dispatch generate(ConditionalExpression expr)
		'''(«expr.cond.generate» ? «expr.then.generate» : «expr.^else.generate»)'''

	// Nested unary operators get parentheses: "- -x" must not become the decrement "--x"
	def dispatch generate(Unary expr)
		'''«IF expr.op == "typeof" || expr.expr instanceof Unary»«expr.op»(«expr.expr.generate»)«ELSE»«expr.op»«expr.expr.generate»«ENDIF»'''
		
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
		'''[«suffix.key.generate»]'''
	
	def dispatch generate(DotPropertyAccessSuffix suffix)
		'''.«suffix.key.generate»'''
	
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
	
	def dispatch generate(PropertyAssignment expr) {
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
	
	def dispatch generate(NumberLiteral expr) {
		if (Math.rint(expr.value)==expr.value)
			return String.format("%.0f", expr.value);
		return String.valueOf(expr.value);	
	}
	
	def dispatch generate(BooleanLiteral expr) {
		String.valueOf(expr.value);	
	}

	def dispatch generate(NullObject expr) '''null'''
	
	def String getName(Identifier id) {
		var String name = null;
		if (TinyscriptModelUtil.isShadowing(id))
			name = nameProvider.getFullyQualifiedName(id).toString("_")
		else {
			name = id.name
		}
		return name;
	}
	
}
