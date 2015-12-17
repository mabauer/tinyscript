package de.mkbauer.tinyscript.interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.TinyscriptModelUtil;
import de.mkbauer.tinyscript.ts.AssertStatement;
import de.mkbauer.tinyscript.ts.BinaryExpression;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.BooleanLiteral;
import de.mkbauer.tinyscript.ts.CallOrPropertyAccess;
import de.mkbauer.tinyscript.ts.CallOrPropertyAccessSuffix;
import de.mkbauer.tinyscript.ts.CallSuffix;
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.Function;
import de.mkbauer.tinyscript.ts.FunctionDeclaration;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.IfStatement;
import de.mkbauer.tinyscript.ts.NumberLiteral;
import de.mkbauer.tinyscript.ts.ObjectInitializer;
import de.mkbauer.tinyscript.ts.PropertyAccessSuffix;
import de.mkbauer.tinyscript.ts.ComputedPropertyAccessSuffix;
import de.mkbauer.tinyscript.ts.DotPropertyAccessSuffix;
import de.mkbauer.tinyscript.ts.PropertyAssignment;
import de.mkbauer.tinyscript.ts.PropertyName;
import de.mkbauer.tinyscript.ts.Reference;
import de.mkbauer.tinyscript.ts.ReturnStatement;
import de.mkbauer.tinyscript.ts.Statement;
import de.mkbauer.tinyscript.ts.StringLiteral;
import de.mkbauer.tinyscript.ts.Tinyscript;
import de.mkbauer.tinyscript.ts.Unary;
import de.mkbauer.tinyscript.ts.VariableStatement;
import de.mkbauer.tinyscript.ts.util.TsSwitch;


/**
 * Evaluates expressions of the Tinyscript language.
 * @author markus.bauer
 *
 */
public class ExecutionVisitor extends TsSwitch<TSValue> {
	
	private ExecutionContext currentContext;
	
	public ExecutionVisitor() {
		currentContext = new ExecutionContext();
	}

    public TSValue execute(EObject object) {
    	return doSwitch(object);
    }
	
	@Override
	public TSValue defaultCase(EObject object) {
		throw new UnsupportedOperationException("Unsupported script node: " + object.eClass().getName());
	}
  	
    @Override
	public TSValue caseTinyscript(Tinyscript object) {
    	return execute(object.getGlobal());
	}

    @Override
	public TSValue caseBlock(Block object) {
    	TSValue result = TSValue.UNDEFINED;
        for (Statement s : object.getStatements()) {
        	result = execute(s); 
        }
        return result;
    }
    
    @Override
    public TSValue caseVariableStatement(VariableStatement object) {
    	for (Expression expr : object.getVardecls()) {
        	execute(expr); 
        }
    	return TSValue.UNDEFINED;
    }
    
    @Override
    public TSValue caseAssertStatement(AssertStatement object) {
    	TSValue cond = execute(object.getCond());
    	if (!cond.asBoolean()) {
    		throw new TinyscriptAssertationError("assert: condition is false", object);
    	}
    	return cond;
    }
  
    @Override
    public TSValue caseIfStatement(IfStatement object) {
    	TSValue cond = execute(object.getCond());
    	if (cond.asBoolean()) {
    		return execute(object.getThen());
    	} else {
    		if (object.getElse() != null) {
    			return execute(object.getElse());
    		}
    	}
    	return TSValue.UNDEFINED;
    }
    
//    
//    @Override
//    public TSValue caseExpression(Expression object) {
//    	throw new UnsupportedOperationException("Unsupported expression node: " + object.eClass().getName());
//    }
    
    @Override
    public TSValue caseBinaryExpression(BinaryExpression expr) {
    	String op = expr.getOp();			

    	TSValue right = execute(expr.getRight());
    	if (op.equals("=")) {
    		return assignValue(expr.getLeft(), right);
    	}
    	
    	TSValue left = execute(expr.getLeft());
    	if (op.equalsIgnoreCase("==")) {
    		return new TSValue(left.equals(right));
    	}
    	if (op.equals("+")) {
    		if (left.isNumber() && right.isNumber()) {
    			return new TSValue(left.asDouble() + right.asDouble());
    		}
    		if (left.isString() || right.isString()) {
    			return new TSValue(left.asString() + right.asString());
    		}
    	}
    	if (op.equals("-")) {
    		if (left.isNumber() && right.isNumber()) {
    			return new TSValue(left.asDouble() - right.asDouble());
    		}
    	}
    	if (op.equals("*")) {
       		if (left.isNumber() && right.isNumber()) {
    			return new TSValue(left.asDouble() * right.asDouble());
    		}
    	}
    	if (op.equals("/")) {
       		if (left.isNumber() && right.isNumber()) {
    			return new TSValue(left.asDouble() / right.asDouble());
    		}
    	}
    	if (op == null) 
    		return left;
    	// Handling of boolean and, or ...
    	// Error handling
		return left;
    }
    
    @Override
    public TSValue caseUnary(Unary expr) {
    	TSValue value = execute(expr.getExpr());
    	if (expr.getOp() != null && expr.getOp().equals("-")) 
    		if (value.isNumber()) {
    			value = new TSValue(-value.asDouble());
    		}
    	// TODO: Handle boolean NOT
    	// TODO: Error handling
    	return value;
    }    
    

    @Override
    public TSValue caseCallOrPropertyAccess(CallOrPropertyAccess expr) {
    	TSValue value = execute(expr.getExpr()); 
    	CallOrPropertyAccessSuffix suffix = expr.getSuffix();
		if (suffix instanceof PropertyAccessSuffix) {
			String key = evaluatePropertyKey((PropertyAccessSuffix) suffix);
			if (value.isObject()) {
				value = value.asObject().get(key);
			}
			else {
				// TODO: Handle builtin types! E.g.: "xxx".size(),...
				throw new TinyscriptTypeError("Property accessors are only allowed for objects", expr);
			}
		}
    		// TODO: Handle call suffixes	
    	return value;
    }

    @Override
    public TSValue casePropertyName(PropertyName expr) {
    	if (expr.getName() != null)
    		return new TSValue(expr.getName());
    	return execute(expr.getExpr());
    }
    
    @Override
    public TSValue caseIdentifier(Identifier expr) {
   		currentContext.create(expr);
   		return TSValue.UNDEFINED;
    }
    
    @Override
    public TSValue caseReference(Reference expr) {
    	return currentContext.lookup(expr.getId());
    }
    
    @Override
    public TSValue caseBooleanLiteral(BooleanLiteral expr) {
    	return new TSValue(expr.isValue()); 
    }
    
    @Override
    public TSValue caseNumberLiteral(NumberLiteral expr) {
    	return new TSValue(expr.getValue()); 
    }
    
    @Override
    public TSValue caseStringLiteral(StringLiteral expr) {
    	return new TSValue(expr.getValue()); 
    }
    
    @Override
    public TSValue caseObjectInitializer(ObjectInitializer expr) {
    	TSObject obj = new TSObject(); 
    	for (PropertyAssignment assignment : expr.getPropertyassignments()) {
    		String key = null;
    		TSValue keyValue = execute(assignment.getKey());
    		if (keyValue.isString() || keyValue.isNumber()) {
    			key = keyValue.asString();
    			obj.put(key, execute(assignment.getValue()));
    		}
    	}
    	return new TSValue(obj);
    }
    
    public TSValue assignValue(Expression left, TSValue value) {
    	
    	if (left instanceof CallOrPropertyAccess) {
    		CallOrPropertyAccess expr = (CallOrPropertyAccess) left;
        	CallOrPropertyAccessSuffix suffix = expr.getSuffix();
        	TSValue prefix = execute(expr.getExpr());
    		if (suffix instanceof PropertyAccessSuffix) {
    			PropertyAccessSuffix propSuffix = (PropertyAccessSuffix) suffix;
    			String key = evaluatePropertyKey(propSuffix);
    			if (prefix.isObject()) {
    				prefix.asObject().put(key, value);
    				return value;
    			}
    			else {
    				// TODO: Handle builtin types! E.g.: "xxx".size(),...
    				throw new TinyscriptTypeError("Property accessors are only allowed for objects", expr);
    			}
    		}
			
    	}
    	if (left instanceof Identifier) {
    		Identifier identifier = (Identifier) left;
    		currentContext.create(identifier);
    		currentContext.store(identifier, value);
    		return value;
    	}
    	if (left instanceof Reference) {
    		Identifier identifier = ((Reference) left).getId();
    		currentContext.store(identifier, value);
    		return value;
    	}

    	return TSValue.UNDEFINED;
    	// throw new UnsupportedOperationException("Unsupported left-hand expression in assignment: " + left.eClass().getName() );
    }
    
    
	private String evaluatePropertyKey(PropertyAccessSuffix suffix) {
		String key = null;
		TSValue keyExpr = null;
		if (suffix instanceof DotPropertyAccessSuffix) {
			DotPropertyAccessSuffix accessor = (DotPropertyAccessSuffix) suffix;
			keyExpr = execute(accessor.getKey());
		}
		if (suffix instanceof ComputedPropertyAccessSuffix) {
			ComputedPropertyAccessSuffix accessor = (ComputedPropertyAccessSuffix) suffix;
			keyExpr = execute(accessor.getKey());
		}
		if (keyExpr.isString() || keyExpr.isNumber()) {
			key = keyExpr.asString();
		}
		else {
			throw new TinyscriptTypeError("Property accessors should evaluate to String or Number", suffix);
		}

		return key;
	}
	
}
