
package de.mkbauer.tinyscript.generator

import de.mkbauer.tinyscript.ts.Tinyscript
import de.mkbauer.tinyscript.ts.VariableStatement
import de.mkbauer.tinyscript.ts.BinaryExpression
import de.mkbauer.tinyscript.ts.Identifier
import de.mkbauer.tinyscript.ts.Reference
import de.mkbauer.tinyscript.ts.AssertStatement
import de.mkbauer.tinyscript.ts.BreakStatement
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
import de.mkbauer.tinyscript.ts.NullObject
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
		
		if (typeof print !== "function") {
			print = function(s) {
				console.log(s);
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
		tempCounter = 0
		generateScript(script, includeBuiltins)
	}
	
	def private generateScript(Tinyscript script, boolean includeBuiltins) '''
		«IF includeBuiltins»
			«generateBuiltins»
		«ENDIF»
		«script.generate»
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
		«stmt.expr.generate»;
	'''
	
	def dispatch generate(VariableStatement stmt) '''
		«FOR vardecl: stmt.vardecls BEFORE 'var ' SEPARATOR ', ' AFTER ';'»
			«vardecl.generate»«ENDFOR»
	'''
	
	def dispatch generate(FunctionDeclaration func) '''
		function «IF (func.id !== null)»«func.id.generate»«ENDIF»(«FOR param: func.params SEPARATOR ', '»«param.generate»«ENDFOR») {
			«func.block.generate»
		}
		
	''' 
	
	def dispatch generate(ArrowFunction func) 
		'''function («FOR param: func.params SEPARATOR ', '»«param.generate»«ENDFOR») { return «func.block.generate» ;}'''
	
	def dispatch generate(FunctionDefinition func) '''
		function «IF (func.id !== null)»«func.id.name»«ENDIF»(«FOR param: func.params SEPARATOR ', '»«param.generate»«ENDFOR») {
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
		if (Array.isArray(«iterable»)) {
			for (var «index» = 0; «index» < «iterable».length; «index»++) {
				«loopVarDefinition» = «iterable»[«index»];
				«stmt.^do.generate»
			}	
		}
		'''
	}
	
	def dispatch generate(BinaryExpression expr) '''
		«IF (expr.op.equals("+"))»__ts_add(«expr.left.generate», «expr.right.generate»)«ELSEIF (expr.op.equals("=="))»__ts_eq(«expr.left.generate», «expr.right.generate»)«ELSEIF (expr.op.equals("!="))»!__ts_eq(«expr.left.generate», «expr.right.generate»)«ELSE»«expr.left.generate» «expr.op» «expr.right.generate»«ENDIF»'''

	def dispatch generate(Unary expr)
		'''«IF expr.op == "typeof"»typeof («expr.expr.generate»)«ELSE»«expr.op»«expr.expr.generate»«ENDIF»'''
		
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
		"\"" + expr.value + "\"";	
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
