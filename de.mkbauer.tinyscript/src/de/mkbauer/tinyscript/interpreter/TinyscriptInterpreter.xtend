package de.mkbauer.tinyscript.interpreter

import de.mkbauer.tinyscript.TinyscriptStandaloneSetup
import de.mkbauer.tinyscript.ts.Addition
import de.mkbauer.tinyscript.ts.AssignmentExpression
import de.mkbauer.tinyscript.ts.DoubleLiteral
import de.mkbauer.tinyscript.ts.IntegerLiteral
import de.mkbauer.tinyscript.ts.Multiplication
import de.mkbauer.tinyscript.ts.SourceElements
import de.mkbauer.tinyscript.ts.Statement
import de.mkbauer.tinyscript.ts.StringLiteral
import de.mkbauer.tinyscript.ts.Tinyscript
import de.mkbauer.tinyscript.ts.Unary
import de.mkbauer.tinyscript.ts.UnaryOrPrimary
import java.util.HashMap
import org.eclipse.emf.common.util.URI
import org.eclipse.xtext.resource.XtextResourceSet

class TinyscriptInterpreter {
	
	var Tinyscript program;
	
	def static void main(String[] args) {
 		val fileName = args.get(0)
 		val uri = URI.createURI(fileName)
  
 		val injector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration
 		val resourceSet = injector.getInstance(XtextResourceSet)
 		val resource = resourceSet.createResource(uri)
 		resource.load(new HashMap())
 		val script = resource.contents.get(0) as Tinyscript

 		val interpreter = new TinyscriptInterpreter(script)
 		interpreter.interpret() 
    }  
    
    new(Tinyscript p) {
    	program = p
    }
    
    def void interpret() {
    	interpret(program)
    }
  
  	def dispatch void interpret(Tinyscript p) {
  		interpret(p.elements)
  	}
  	
    def dispatch void interpret(SourceElements b) {
        b.statements.forEach[s | interpret(s) ]
    }
    
    def dispatch void interpret(Statement s) {
    	throw new UnsupportedOperationException("Unsupported statement" + s.toString)
    }
    
    def dispatch TSValue evaluate(AssignmentExpression expr) {
    	val value = expr.lhs.evaluate()
    	return value
    }
    
    def dispatch TSValue evaluate(Addition expr) {
		var TSValue value = expr.expr1.evaluate()
		val op = expr.ops.iterator()
    	for (operand : expr.exprs) {
    		val TSValue other = operand.evaluate()
    		val symbol = op.next()
    		if (value.isNumber() && other.isNumber()) {
    			// TODO: Operator handling
    			if (symbol == '+')
    				value = new TSValue(value.asDouble() + other.asDouble())
    			else if (symbol == '-')
    				value = new TSValue(value.asDouble() - other.asDouble())
    		}
    		if (value.isString() || other.isString()) {
    			if (symbol == '+')
    				value = new TSValue(value.asString() + other.asString())
    		}
    		// TODO: Error handling
    	}
    	return value
    }
    
    def dispatch TSValue evaluate(Multiplication expr) {
    	var TSValue value = expr.expr1.evaluate()
    	val op = expr.ops.iterator()			
    	for (operand : expr.exprs) {
			val TSValue other = operand.evaluate()
			val symbol = op.next()
			if (value.isNumber() && other.isNumber()) {
				// TODO: Operator handling
    			if (symbol == '*')
    				value = new TSValue(value.asDouble() * other.asDouble())
    			else if (symbol == '/')
    				value = new TSValue(value.asDouble() / other.asDouble())
			}
			// TODO: Error handling
		}
		return value
    }
    
    def dispatch TSValue evaluate(UnaryOrPrimary expr) {
    	val value = expr.expr.evaluate()
    	return value
    }
    
    def dispatch TSValue evaluate(Unary expr) {
    	var TSValue value = expr.expr.evaluate()
    	if (value.isNumber()) {
    		value = new TSValue(-value.asDouble())
    	}
    	// TODO: Error handling
    	return value
    }    
    
    def dispatch TSValue evaluate(IntegerLiteral expr) {
    	return new TSValue(expr.value); 
    }
    
    def dispatch TSValue evaluate(DoubleLiteral expr) {
    	return new TSValue(expr.value); 
    }
    
    def dispatch TSValue evaluate(StringLiteral expr) {
    	return new TSValue(expr.value); 
    }
    
    
    
}