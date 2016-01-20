
package de.mkbauer.tinyscript.generator

import de.mkbauer.tinyscript.ts.Tinyscript
import de.mkbauer.tinyscript.ts.VariableStatement
import de.mkbauer.tinyscript.ts.BinaryExpression
import de.mkbauer.tinyscript.ts.Identifier
import de.mkbauer.tinyscript.ts.Reference
import de.mkbauer.tinyscript.ts.AssertStatement
import de.mkbauer.tinyscript.ts.StringLiteral
import de.mkbauer.tinyscript.ts.BlockStatement
import de.mkbauer.tinyscript.ts.Block
import de.mkbauer.tinyscript.TinyscriptModelUtil
import org.eclipse.emf.ecore.EObject
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

class TinyscriptGenerator  {
	
	@Inject
	TinyscriptQualifiedNameProvider nameProvider;
	
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
			var result;
			if (Array.isArray(obj1)) {
				if (Array.isArray(obj2)) {
					return obj1.concat(obj2);
				} 
				else {
					result = obj1.slice(0);
					result.push(obj2);
					return result;
				}
			}
			if (Array.isArray(obj2)) {
				result = obj2.slice();
				result.unshift(obj1);
				return result;
			}
			return obj1 + obj2;
		}
		
	'''
	
	def generate(Tinyscript script, boolean includeBuiltins) '''
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
		function «IF (func.id != null)»«func.id.generate»«ENDIF»(«FOR param: func.params SEPARATOR ', '»«param.generate»«ENDFOR») {
			«func.block.generate»
		}
		
	''' 
	
	def dispatch generate(ArrowFunction func) 
		'''function («FOR param: func.params SEPARATOR ', '»«param.generate»«ENDFOR») { return «func.block.generate» ;}'''

	def dispatch generate(FunctionDefinition func) '''
		function «IF (func.id != null)»«func.id.name»«ENDIF»(«FOR param: func.params SEPARATOR ', '»«param.generate»«ENDFOR») {
			«func.block.generate»
		}'''	
		
	def dispatch generate(ReturnStatement stmt) '''
		return«IF stmt.expr != null» «stmt.expr.generate»«ENDIF»;
	'''
	
	def dispatch generate(AssertStatement stmt) '''
		assert «stmt.cond.generate»;
	'''
	
	def dispatch generate(IfStatement stmt) '''
		if («stmt.cond.generate») {
			«stmt.then.generate»
		}
		«IF stmt.^else!=null»
		else {
			«stmt.^else.^else.generate»
		}
		«ENDIF»
	'''
	
	def dispatch generate(ForStatement stmt) {
		if (stmt.numericForExpr != null) {
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
		if (stmt.id != null) {
			loopVar = stmt.id.generate as String
			loopVarDefinition = "var " + loopVar;
		}
		else {
			loopVar = stmt.ref.generate as String
			loopVarDefinition = loopVar;
		}
		'''
		«IF (expr.step == null)»
			for («loopVarDefinition» = «expr.start.generate»; «loopVar» <= «expr.stop.generate»; «loopVar» = «loopVar» + 1) {
				«stmt.^do.generate»
			}
		«ELSE»
			for («loopVarDefinition» = «expr.start.generate»; («expr.step.generate» > 0)?(«loopVar» <= «expr.stop.generate»):(«loopVar» >= «expr.stop.generate»); «loopVar» = «loopVar» + «expr.step.generate») {
				«stmt.^do.generate»
			}
		«ENDIF»'''		
	}
	
	def generateIterableForStatement(ForStatement stmt) {
		val IterableForExpression expr=stmt.iterableForExpr; 
		'''
		if (Array.isArray(«expr.iterable.generate»)) {
			for (var __ts_i = 0; __ts_i < («expr.iterable.generate»).length; __ts_i++) {
				var «stmt.id.generate» = «expr.iterable.generate»[__ts_i];
				«stmt.^do.generate»
			}	
		}
		'''
	}
	
	def dispatch generate(BinaryExpression expr) '''
		«IF (expr.op.equals("+"))»__ts_add(«expr.left.generate», «expr.right.generate»)«ELSE»«expr.left.generate» «expr.op» «expr.right.generate»«ENDIF»'''

	def dispatch generate(Unary expr)
		'''«expr.op»«expr.expr.generate»'''
		
	def dispatch generate(NewExpression expr)
		'''new «expr.expr.generate»'''
		
	def dispatch generate(GroupingExpression expr)
		'''(«expr.expr.generate»)'''

	def dispatch generate(CallOrPropertyAccess expr) 
		'''«expr.expr.generate»«IF (expr.suffix != null)»«expr.suffix.generate»«ENDIF»'''
	
	def dispatch generate(CallOrPropertyAccessSuffix suffix) 
		'''«suffix.property.generate»«IF (suffix.call != null)»«suffix.call.generate»«ENDIF»'''

	def dispatch generate(CallSuffix suffix)
		'''(«FOR arg: suffix.arguments SEPARATOR ', '»«arg.generate»«ENDFOR»)'''
	
	def dispatch generate(ComputedPropertyAccessSuffix suffix)
		'''[«suffix.key.generate»]'''
	
	def dispatch generate(DotPropertyAccessSuffix suffix)
		'''.«suffix.key.generate»'''
	
	def dispatch generate(PropertyName expr) 
		'''«IF expr.name != null»«expr.name»«ELSE»expr.expr.generate«ENDIF»'''
	
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
