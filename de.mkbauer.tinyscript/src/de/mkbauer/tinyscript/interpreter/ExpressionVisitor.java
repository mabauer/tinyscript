package de.mkbauer.tinyscript.interpreter;

import java.util.Iterator;

import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.ts.Addition;
import de.mkbauer.tinyscript.ts.AssignmentExpression;
import de.mkbauer.tinyscript.ts.Atomic;
import de.mkbauer.tinyscript.ts.DoubleLiteral;
import de.mkbauer.tinyscript.ts.IntegerLiteral;
import de.mkbauer.tinyscript.ts.Multiplication;
import de.mkbauer.tinyscript.ts.StringLiteral;
import de.mkbauer.tinyscript.ts.Unary;
import de.mkbauer.tinyscript.ts.UnaryOrPrimary;
import de.mkbauer.tinyscript.ts.VariableOrMember;
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
    public TSValue caseAssignmentExpression(AssignmentExpression expr) {
    	TSValue value = evaluate(expr.getLhs());
    	return value;
    }
    
    @Override
    public TSValue caseAddition(Addition expr) {
		TSValue value = evaluate(expr.getExpr1());
		Iterator<String> op = expr.getOps().iterator();
    	for (EObject operand : expr.getExprs()) {
    		TSValue other = evaluate(operand);
    		String symbol = op.next();
    		if (value.isNumber() && other.isNumber()) {
    			// TODO: Operator handling
    			if (symbol.equals("+"))
    				value = new TSValue(value.asDouble() + other.asDouble());
    			else if (symbol.equals("-"))
    				value = new TSValue(value.asDouble() - other.asDouble());
    		}
    		if (value.isString() || other.isString()) {
    			if (symbol.equals("+"))
    				value = new TSValue(value.asString() + other.asString());
    		}
    		// TODO: Error handling
    	}
    	return value;
    }
    
    @Override
    public TSValue caseMultiplication(Multiplication expr) {
    	TSValue value = evaluate(expr.getExpr1());
    	Iterator<String> op = expr.getOps().iterator();			
    	for (EObject operand : expr.getExprs()) {
			TSValue other = evaluate(operand);
			String symbol = op.next();
			if (value.isNumber() && other.isNumber()) {
				// TODO: Operator handling
				if (symbol.equals("*"))
    				value = new TSValue(value.asDouble() * other.asDouble());
    			else if (symbol.equals("/"))
    				value = new TSValue(value.asDouble() / other.asDouble());
			}
			// TODO: Error handling
		}
		return value;
    }
    
    @Override
    public TSValue caseUnaryOrPrimary(UnaryOrPrimary expr) {
    	TSValue value = evaluate(expr.getExpr());
    	return value;
    }
    
    @Override
    public TSValue caseUnary(Unary expr) {
    	TSValue value = evaluate(expr.getExpr());
    	if (value.isNumber()) {
    		value = new TSValue(-value.asDouble());
    	}
    	// TODO: Error handling
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
