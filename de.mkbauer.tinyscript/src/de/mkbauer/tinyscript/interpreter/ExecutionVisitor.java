package de.mkbauer.tinyscript.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.TinyscriptModelUtil;
import de.mkbauer.tinyscript.ts.AssertStatement;
import de.mkbauer.tinyscript.ts.BinaryExpression;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.BlockStatement;
import de.mkbauer.tinyscript.ts.BooleanLiteral;
import de.mkbauer.tinyscript.ts.CallOrPropertyAccess;
import de.mkbauer.tinyscript.ts.CallOrPropertyAccessSuffix;
import de.mkbauer.tinyscript.ts.CallSuffix;
import de.mkbauer.tinyscript.ts.ElseStatement;
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.ForEachStatement;
import de.mkbauer.tinyscript.ts.Function;
import de.mkbauer.tinyscript.ts.FunctionDeclaration;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.IfStatement;
import de.mkbauer.tinyscript.ts.NumberLiteral;
import de.mkbauer.tinyscript.ts.NumericForStatement;
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
	
	private Stack<ExecutionContext> contextStack;
	
	private Map<Block, LexicalEnvironment> lexicalEnvironments;
	
	public ExecutionVisitor() {
		currentContext = new ExecutionContext("global");
		contextStack = new Stack<ExecutionContext>();
		lexicalEnvironments = new HashMap<Block, LexicalEnvironment>();
	}
	
    public TSValue execute(EObject object) {
    	if (object == null)
    		throw new NullPointerException();
    	return doSwitch(object);
    }
	
	@Override
	public TSValue defaultCase(EObject object) {
		throw new UnsupportedOperationException("Unsupported script node: " + object.eClass().getName());
	}
  	
    @Override
	public TSValue caseTinyscript(Tinyscript object) {
		// Hoist function declarations:
    	// Create function objects (or get them form a cache)
    	LexicalEnvironment env = getLexcialEnvironment(object.getGlobal());
		for (TSValue function : env.getFunctions()) {	
			// Create a variable in the current context pointing to each function object
			String functionName = ((TSFunction) function.asObject()).getName();
			if (!currentContext.contains(functionName))
				currentContext.create(functionName);
			currentContext.store(functionName, function);
		}
    	return execute(object.getGlobal());
	}

    @Override
	public TSValue caseBlock(Block object) {
    	TSValue result = TSValue.UNDEFINED;  	
        for (Statement s : object.getStatements()) {
        	if (!(s instanceof FunctionDeclaration))
        		result = execute(s); 
        }
        return result;
    }
    
    @Override 
    public TSValue caseBlockStatement(BlockStatement object) {
    	return executeInBlockContext("block", object.getBlock());
    }
    
    @Override 
    public TSValue caseFunction(Function object) {
    	TSFunction function = new TSFunction();
    	function.setOuterContext(currentContext);
    	function.setAst(object);
    	return new TSValue(function);
    }
    
    @Override
    public TSValue caseVariableStatement(VariableStatement object) {
    	for (Expression expr : object.getVardecls()) {
        	execute(expr); 
        }
    	return TSValue.UNDEFINED;
    }
    
    @Override
    public TSValue caseReturnStatement(ReturnStatement object) {
    	TSValue returnValue = execute(object.getExpr());
    	throw new TSReturnValue(returnValue);
    }
    
    @Override
    public TSValue caseAssertStatement(AssertStatement object) {
    	TSValue cond = execute(object.getCond());
    	if (!cond.asBoolean()) {
    		throw new TinyscriptAssertationError("assert: condition is false", object);
    	}
    	return cond;
    }
  
    // @Override
    public TSValue caseIfStatement(IfStatement object) {
    	TSValue cond = execute(object.getCond());
    	TSValue result = TSValue.UNDEFINED;
    	if (cond.asBoolean()) {
    		result = executeInBlockContext("if", object.getThen());
    		// result = caseBlock(object.getThen());
    	} else {
    		if (object.getElse() != null) {
    			result= caseElseStatement(object.getElse());
    		}
    	}
    	return result;
    }
    
    // @Override
    public TSValue caseElseStatement(ElseStatement object) {
    	return executeInBlockContext("else", object.getElse());
    	// return caseBlock(object.getElse());
    }
    
    // @Override
    public TSValue caseNumericForStatement(NumericForStatement foreach) {
    	TSValue result = TSValue.UNDEFINED;
		TSValue startValue = execute(foreach.getStart());
		TSValue stopValue = execute(foreach.getStop());    		
		if (!startValue.isMathematicalInteger())
			throw new TinyscriptTypeError("for needs an integer value as first bound", foreach.getStart());
		if (!stopValue.isNumber())
			throw new TinyscriptTypeError("for needs a number value as second bound", foreach.getStart());
		double start = startValue.asDouble();
		double stop = stopValue.asDouble();
		double step = 1;
		if (foreach.getStep() != null) {
			TSValue stepValue =  execute(foreach.getStep());
			if (!stopValue.isMathematicalInteger()) 
				throw new TinyscriptTypeError("for needs an integer value as step expression", foreach.getStep());
			step = stepValue.asDouble();
		}
		for (double loopValue = start ; (step > 0)?(loopValue <= stop):(loopValue >= stop); loopValue = loopValue + step ) {
			if (foreach.getId() != null) {
				result = executeInBlockContext("for", foreach.getDo(), foreach.getId(), new TSValue(loopValue));
			}
			else {
				currentContext.store(foreach.getRef().getId().getName(), new TSValue(loopValue));
				result = executeInBlockContext("for", foreach.getDo());
			}
		}
    	return result;
    }
    
   
    // @Override
    public TSValue caseBinaryExpression(BinaryExpression expr) {
    	String op = expr.getOp();			
    	if (op.equals("=")) {
    	   	TSValue right = execute(expr.getRight());
    		return assignValue(expr.getLeft(), right);
    	}   	
    	TSValue left = execute(expr.getLeft());
       	if (op.equalsIgnoreCase("&&")) {
       		if (left.asBoolean())
       			return new TSValue(execute(expr.getRight()).asBoolean());
       		return new TSValue(false);
    	}
    	if (op.equalsIgnoreCase("||")) {
    		if (left.asBoolean())
    			return new TSValue(true);
    		return new TSValue(execute(expr.getRight()).asBoolean());
    	}
    	TSValue right = execute(expr.getRight());
    	if (op.equalsIgnoreCase("==")) {
    		return new TSValue(left.equals(right));
    	}
    	if (op.equalsIgnoreCase(">")) {
    		if (left.isNumber() && right.isNumber())
    			return new TSValue(left.asDouble() > right.asDouble());
    		else
    			throw new UnsupportedOperationException("Unsupported binary expression: " + op);
    	}
    	if (op.equalsIgnoreCase("<")) {
    		if (left.isNumber() && right.isNumber())
    			return new TSValue(left.asDouble() < right.asDouble());
    		else
    			throw new UnsupportedOperationException("Unsupported binary expression: " + op);
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
		throw new UnsupportedOperationException("Unsupported binary expression: " + op);
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
    	if 	(suffix instanceof CallSuffix) {
    		if (value.isObject() && (value.asObject() instanceof TSFunction)) {
    			CallSuffix callSuffix = (CallSuffix) suffix;
    			List<TSValue> args = null;  			
    			if (callSuffix.getArguments() != null && callSuffix.getArguments().size() > 0) {
    				args = new ArrayList<TSValue>();
    				for (EObject argExpr : callSuffix.getArguments()) {
    					args.add(execute(argExpr));
    				}
    			}
    			value = applyFunction((TSFunction) value.asObject(), args);
    		}
    		else {
    			throw new TinyscriptTypeError("Calls are only allowed for function objects", expr);
    		}
    	}
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
   		currentContext.create(expr.getName());
   		return TSValue.UNDEFINED;
    }
    
    @Override
    public TSValue caseReference(Reference expr) {
    	try {
    		return currentContext.lookup(expr.getId().getName());
    	}
    	catch (IllegalArgumentException e) {
    		throw new TinyscriptReferenceError("Unknown Identifier", expr);
    	}
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
    		if (suffix instanceof CallSuffix) {
    			throw new TinyscriptTypeError("Invalid left-hand side expression", expr);
    		}
    	}
    	if (left instanceof Identifier) {
    		Identifier identifier = (Identifier) left;
    		currentContext.create(identifier.getName());
    		currentContext.store(identifier.getName(), value);
    		return value;
    	}
    	if (left instanceof Reference) {
    		Identifier identifier = ((Reference) left).getId();
    		currentContext.store(identifier.getName(), value);
    		return value;
    	}

    	return TSValue.UNDEFINED;
    	// throw new UnsupportedOperationException("Unsupported left-hand expression in assignment: " + left.eClass().getName() );
    }
    
	public TSValue applyFunction(TSFunction function, List<TSValue> args) {
		Block block = function.getBlock();
		try {
			// Create a new execution context
			enterNewExecutionContext(function.getName(), function.getOuterContext());
			// Put the arguments into the context
			if (args != null) {
				int argsN = args.size();
				int i = 0;
				for (Identifier param : function.getAst().getParams()) {
					currentContext.create(param.getName());
					if (i < argsN) {
						currentContext.store(param.getName(), args.get(i));
					}
					i++;
				}
			}
			TSValue result = execute(block);
			leaveExecutionContext();
			return result;
		}
		catch (TSReturnValue rv) {
			// Restore execution context
			leaveExecutionContext();
			return rv.getReturnValue();				
		}

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
	
	private TSValue executeInBlockContext(String name, Block block) {
		return executeInBlockContext(name, block, null, null);
	}
	
	private TSValue executeInBlockContext(String name, Block block, Identifier variable, TSValue value) {
		TSValue result = null;
    	boolean needsNewContext = true;
    	LexicalEnvironment env = getLexcialEnvironment(block);
    	if ((variable==null) && (env.getFunctions().isEmpty()) && (env.getVariables().isEmpty()))
    		needsNewContext = false;
    	if (needsNewContext) {
			enterNewExecutionContext(name);
			// Insert variable into context
			if (variable != null) {
				currentContext.create(variable.getName());
				currentContext.store(variable.getName(), value);
			}
			// Hoist function declarations
			for (TSValue function : env.getFunctions()) {	
				// Create a variable in the current context pointing to each function object
				String functionName = ((TSFunction) function.asObject()).getName();
				if (!currentContext.contains(functionName))
					currentContext.create(functionName);
				currentContext.store(functionName, function);
			}
		}
		try {
			result = caseBlock(block);
		}
		catch (TSReturnValue e) {
			if (needsNewContext)
				leaveExecutionContext();
			throw e;
		}
		if (needsNewContext)
			leaveExecutionContext();
		return result;
	}
	
	private void enterNewExecutionContext(String name) {
		enterNewExecutionContext(name, null);
	}
	
	private void enterNewExecutionContext(String name, ExecutionContext outer) {
		contextStack.push(currentContext);
		if (outer == null) {
			currentContext = new ExecutionContext(name, currentContext);
		}
		else {
			currentContext = new ExecutionContext(name, outer);
		}
	}
	
	private void leaveExecutionContext() {
		currentContext = contextStack.pop();
	}
	
	private LexicalEnvironment getLexcialEnvironment(Block block) {
		LexicalEnvironment result = lexicalEnvironments.get(block);
		if (result != null)
			return result;
		List <TSValue> functions = new ArrayList<TSValue>();
		for (Function funcdecl : TinyscriptModelUtil.functionDeclarationsInBlock(block)) {
			TSValue functionObject = caseFunction(funcdecl);
			functions.add(functionObject);
		}
		List <Identifier> variables = TinyscriptModelUtil.declaredVariablesInBlock(block);
		result =  new LexicalEnvironment(functions, variables);
		lexicalEnvironments.put(block, result);
		return result;
	}
}
