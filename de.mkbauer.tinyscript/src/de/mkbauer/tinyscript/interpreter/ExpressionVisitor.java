package de.mkbauer.tinyscript.interpreter;

import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.ts.BinaryExpression;
import de.mkbauer.tinyscript.ts.DoubleLiteral;
import de.mkbauer.tinyscript.ts.IntegerLiteral;
import de.mkbauer.tinyscript.ts.Primary;
import de.mkbauer.tinyscript.ts.StringLiteral;
import de.mkbauer.tinyscript.ts.Unary;
import de.mkbauer.tinyscript.ts.util.TsSwitch;


/**
 * Evaluates expressions of the Tinyscript language.
 * @author markus.bauer
 *
 */
public class ExpressionVisitor extends TsSwitch<TSValue> {

    public TSValue evaluate(EObject object) {
    	return doSwitch(object);
    }

	@Override
	public TSValue defaultCase(EObject object) {
		throw new RuntimeException("Unsupported expression node: " + object.eClass().getName());
	}
    
    @Override
    public TSValue caseBinaryExpression(BinaryExpression expr) {
    	TSValue left = evaluate(expr.getLeft());
    	String op = expr.getOp();			
    	if (op == null) 
    		return left;
    	TSValue right = evaluate(expr.getRight());
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
    	// Handling of boolean and, or ...
    	// Error handling
		return left;
    }
    
    @Override
    public TSValue caseUnary(Unary expr) {
    	TSValue value = evaluate(expr.getExpr());
    	if (expr.getOp() != null && expr.getOp().equals("-")) 
    		if (value.isNumber()) {
    			value = new TSValue(-value.asDouble());
    		}
    	// TODO: Handle boolean NOT
    	// TODO: Error handling
    	return value;
    }    
    
    @Override
    public TSValue casePrimary(Primary expr) {
    	TSValue value = evaluate(expr.getExpr());
    	// TODO: Handle CallOrMemberSuffix!
    	return value;
    }    
    
    
    @Override
    public TSValue caseIntegerLiteral(IntegerLiteral expr) {
    	return new TSValue(expr.getValue()); 
    }
    
    @Override
    public TSValue caseDoubleLiteral(DoubleLiteral expr) {
    	return new TSValue(expr.getValue()); 
    }
    
    @Override
    public TSValue caseStringLiteral(StringLiteral expr) {
    	return new TSValue(expr.getValue()); 
    }
	
}
