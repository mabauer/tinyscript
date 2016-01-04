package de.mkbauer.tinyscript.interpreter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.TinyscriptModelUtil;
import de.mkbauer.tinyscript.ts.ArrayInitializer;
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
import de.mkbauer.tinyscript.ts.FunctionDefinition;
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
import de.mkbauer.tinyscript.ts.TsFactory;
import de.mkbauer.tinyscript.ts.TsPackage;
import de.mkbauer.tinyscript.ts.Unary;
import de.mkbauer.tinyscript.ts.VariableStatement;
import de.mkbauer.tinyscript.ts.util.TsSwitch;

import de.mkbauer.tinyscript.runtime.math.Math;
/**
 * Evaluates expressions of the Tinyscript language.
 * @author markus.bauer
 *
 */
public class ExecutionVisitor /* extends TsSwitch<TSValue> */ {
	
	private ExecutionContext currentContext;
	private ExecutionContext globalContext;
	
	private Deque<ExecutionContext> contextStack;
	
	private Map<Block, LexicalEnvironment> lexicalEnvironments;
	
	public ExecutionVisitor() {
		globalContext = new GlobalExecutionContext();
		currentContext = globalContext;
		contextStack = new ArrayDeque<ExecutionContext>();
		lexicalEnvironments = new HashMap<Block, LexicalEnvironment>();
		initializeGlobalContext();
	}
	
	public void initializeGlobalContext() {
		TSObject global = globalContext.getThisRef();
		TSObject.defineDefaultProperty(global, "Math", new Math());
	}
    
    public TSValue execute(EObject object) {
    	if (object == null)
    		throw new NullPointerException();
    	int id = object.eClass().getClassifierID();
    	switch (id) {
		case TsPackage.REFERENCE: 
			return caseReference((Reference) object);
		case TsPackage.IDENTIFIER:
			return caseIdentifier((Identifier) object);	
		case TsPackage.BINARY_EXPRESSION:
    		return caseBinaryExpression((BinaryExpression) object);
		case TsPackage.UNARY:
    		return caseUnary((Unary) object);
		case TsPackage.CALL_OR_PROPERTY_ACCESS:
    		return caseCallOrPropertyAccess((CallOrPropertyAccess) object);
		case TsPackage.PROPERTY_NAME:
    		return casePropertyName((PropertyName) object);
		case TsPackage.BLOCK:
    		return caseBlock((Block) object);
		case TsPackage.VARIABLE_STATEMENT:
    		return caseVariableStatement((VariableStatement) object);
		case TsPackage.IF_STATEMENT:
    		return caseIfStatement((IfStatement) object);
		case TsPackage.ELSE_STATEMENT:
    		return caseElseStatement((ElseStatement) object);
		case TsPackage.NUMERIC_FOR_STATEMENT:
    		return caseNumericForStatement((NumericForStatement) object);
		case TsPackage.FUNCTION_DEFINITION:
    		return caseFunctionDefinition((FunctionDefinition) object);
		case TsPackage.RETURN_STATEMENT:
    		return caseReturnStatement((ReturnStatement) object);
		case TsPackage.NUMBER_LITERAL:
    		return caseNumberLiteral((NumberLiteral) object);
		case TsPackage.STRING_LITERAL:
    		return caseStringLiteral((StringLiteral) object);
		case TsPackage.BOOLEAN_LITERAL:
    		return caseBooleanLiteral((BooleanLiteral) object);
		case TsPackage.OBJECT_INITIALIZER:
    		return caseObjectInitializer((ObjectInitializer) object);
		case TsPackage.ARRAY_INITIALIZER:
    		return caseArrayInitializer((ArrayInitializer) object);
		case TsPackage.BLOCK_STATEMENT:
    		return caseBlockStatement((BlockStatement) object);
		case TsPackage.ASSERT_STATEMENT:
    		return caseAssertStatement((AssertStatement) object);
		case TsPackage.TINYSCRIPT:
			return caseTinyscript((Tinyscript) object);
		default:
    		return defaultCase(object);
    	}
    }

	
	// @Override
	public TSValue defaultCase(EObject object) {
		throw new UnsupportedOperationException("Unsupported script node: " + object.eClass().getName());
	}
  	
    // @Override
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

    // @Override
	public TSValue caseBlock(Block object) {
    	TSValue result = TSValue.UNDEFINED;  	
        for (Statement s : object.getStatements()) {
        	if (!(s instanceof FunctionDeclaration))
        		result = execute(s); 
        }
        return result;
    }
    
    // @Override 
    public TSValue caseBlockStatement(BlockStatement object) {
    	return executeInBlockContext("block", object.getBlock());
    }
    
    // @Override 
    public TSValue caseFunctionDefinition(FunctionDefinition object) {
    	TSFunction function = new TSFunction();
    	function.setOuterContext(currentContext);
    	function.setAst(object);
    	return new TSValue(function);
    }
    
    // @Override
    public TSValue caseVariableStatement(VariableStatement object) {
    	for (Expression expr : object.getVardecls()) {
        	execute(expr); 
        }
    	return TSValue.UNDEFINED;
    }
    
    // @Override
    public TSValue caseReturnStatement(ReturnStatement object) {
    	TSValue returnValue = execute(object.getExpr());
    	throw new TSReturnValue(returnValue);
    }
    
    // @Override
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
    		if (left.isArray()) {
    			if (right.isArray()) {
    				return new TSValue(TSArray.concat(left.asArray(), right.asArray()));
    			}
    			else {
    				TSArray result = left.asArray().clone();
    				result.add(right);
    				return new TSValue(result);
    			}	
    		}
    		if (right.isArray()) {
    			if (left.isArray()) {
    				return new TSValue(TSArray.concat(left.asArray(), right.asArray()));
    			}
    			else {
    				TSArray result = new TSArray();
    				result.add(left);
    				result = TSArray.concat(result, right.asArray());
    				return new TSValue(result);
    			}
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
    	if (op.equals("%")) {
       		if (left.isNumber() && right.isNumber()) {
    			return new TSValue(left.asDouble() % right.asDouble());
    		}
    	}
    	if (op == null) 
    		return left;
    	// Handling of boolean and, or ...
    	// Error handling
		throw new UnsupportedOperationException("Unsupported binary expression: " + op);
    }
    
    // @Override
    public TSValue caseUnary(Unary expr) {
    	TSValue value = execute(expr.getExpr());
    	if (expr.getOp().equals("-")) {
    		if (value.isNumber()) {
    			value = new TSValue(-value.asDouble());
    			return value;
    		}
    		else {
    			throw new TinyscriptTypeError("Unsupported unary expression operand: " + expr.getExpr());
    		}
    	}
    	if (expr.getOp().equals("!")) {
    		return new TSValue(!value.asBoolean());
    	}
    	// TODO: Error handling
    	return value;
    }    
    

    // @Override
    // TODO: Merge with assignemtValue
    public TSValue caseCallOrPropertyAccess(CallOrPropertyAccess expr) {
    	TSValue base = execute(expr.getExpr()); 
    	TSValue result = TSValue.UNDEFINED;
    	EObject suffix = expr.getSuffix();
		if (suffix instanceof CallOrPropertyAccessSuffix) {
			CallOrPropertyAccessSuffix callOrProp = (CallOrPropertyAccessSuffix) suffix;
			TSValue keyValue = evaluatePropertyKey(callOrProp.getProperty());
			if (base.isObject()) {
				result = base.asObject().get(keyValue.asString());
			}
			else {
				// TODO: Handle builtin types! E.g.: "xxx".size(),...
				// TODO: Check, if value is undefined
				throw new TinyscriptTypeError("Property accessors are only allowed for objects", expr);
			}
			CallSuffix callSuffix = callOrProp.getCall();
			if (callSuffix != null) {
				if (result.isObject() && (result.asObject() instanceof TSAbstractFunction)) {
					result = processFunctionCall((TSAbstractFunction) result.asObject(), base.asObject(), callSuffix.getArguments());
				}
				else {
					throw new TinyscriptTypeError("Calls are only allowed for function objects", expr);
				}	
			}		
		}
    	if 	(suffix instanceof CallSuffix) {
    		CallSuffix callSuffix = (CallSuffix) suffix;
        	if (base.isObject() && (base.asObject() instanceof TSAbstractFunction)) {
        		result = processFunctionCall((TSAbstractFunction) base.asObject(), null, callSuffix.getArguments());
    		}
        	else {
    			throw new TinyscriptTypeError("Calls are only allowed for function objects", expr);
    		}	
    		
    	}
    	return result;
    }

    // @Override
    public TSValue casePropertyName(PropertyName expr) {
    	if (expr.getName() != null)
    		return new TSValue(expr.getName());
    	return execute(expr.getExpr());
    }
    
    // @Override
    public TSValue caseIdentifier(Identifier expr) {
   		currentContext.create(expr.getName());
   		return TSValue.UNDEFINED;
    }
    
    // @Override
    public TSValue caseReference(Reference expr) {
    	try {
    		if (expr.isThis())
    			return new TSValue(currentContext.getThisRef());
    		return currentContext.lookup(expr.getId().getName());
    	}
    	catch (IllegalArgumentException e) {
    		throw new TinyscriptReferenceError("Unknown Identifier", expr);
    	}
    }
    
    // @Override
    public TSValue caseBooleanLiteral(BooleanLiteral expr) {
    	return new TSValue(expr.isValue()); 
    }
    
    // @Override
    public TSValue caseNumberLiteral(NumberLiteral expr) {
    	return new TSValue(expr.getValue()); 
    }
    
    // @Override
    public TSValue caseStringLiteral(StringLiteral expr) {
    	return new TSValue(expr.getValue()); 
    }
    
    // @Override
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
    
    // @Override
    public TSValue caseArrayInitializer(ArrayInitializer expr) {
    	TSArray arr = new TSArray(); 
    	int i = 0;
    	for (Expression itemExpr : expr.getValues()) {
    		arr.put(String.valueOf(i), execute(itemExpr));
    		i++;
    	}
    	return new TSValue(arr);
    }
    
    public TSValue assignValue(Expression left, TSValue value) {
    	
    	if (left instanceof Reference) {
    	    Reference reference = (Reference) left;
    	    if (reference.isThis())
    	    	throw new TinyscriptReferenceError("Cannot assign avalue to 'this'", left);
    		Identifier identifier = reference.getId();
    		currentContext.store(identifier.getName(), value);
    		return value;
    	}
    	if (left instanceof Identifier) {
    		Identifier identifier = (Identifier) left;
    		currentContext.create(identifier.getName());
    		currentContext.store(identifier.getName(), value);
    		return value;
    	}
    	if (left instanceof CallOrPropertyAccess) {
    		CallOrPropertyAccess expr = (CallOrPropertyAccess) left;
        	EObject suffix = expr.getSuffix();
        	TSValue prefix = execute(expr.getExpr());
    		if (suffix instanceof CallOrPropertyAccessSuffix) {
    			PropertyAccessSuffix propSuffix = ((CallOrPropertyAccessSuffix) suffix).getProperty();
    			TSValue keyValue = evaluatePropertyKey(propSuffix);
    			if (prefix.isObject()) {
    				prefix.asObject().put(keyValue.asString(), value);
    				return value;
    			}
    			else {
    				// TODO: Handle builtin types! E.g.: "xxx".size(),...
    				// TODO: Check if value is undefined!
    				throw new TinyscriptTypeError("Property accessors are only allowed for objects", expr);
    			}
    		}
    		if (suffix instanceof CallSuffix) {
    			throw new TinyscriptTypeError("Invalid left-hand side expression", expr);
    		}
    	}
    	return TSValue.UNDEFINED;
    	// throw new UnsupportedOperationException("Unsupported left-hand expression in assignment: " + left.eClass().getName() );
    }
    
	private TSValue evaluatePropertyKey(PropertyAccessSuffix suffix) {
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
			return keyExpr;
		}
		else {
			throw new TinyscriptTypeError("Property accessors should evaluate to String or Number", suffix);
		}
	}
	
    public TSValue processFunctionCall(TSAbstractFunction functionObject, TSObject self, List<Expression> argExprs) {
			List<TSValue> args = null;  			
			if (argExprs.size() > 0) {
				args = new ArrayList<TSValue>(argExprs.size());
				for (EObject argExpr : argExprs) {
					args.add(execute(argExpr));
				}
			}
			return applyFunction(functionObject, self, args);
    }
    
    private TSValue applyFunction(TSAbstractFunction function, TSObject self, List<TSValue> args) {
    	if (function instanceof TSFunction) {
    		return applyInterpretedFunction((TSFunction) function, self, args);
    	}
    	return ((TSBuiltinFunction) function).apply(self, args);
    }
    
	private TSValue applyInterpretedFunction(TSFunction function, TSObject self, List<TSValue> args) {
		Block block = function.getBlock();
		try {
			// Create a new execution context
			enterNewExecutionContext(function.getName(), function.getOuterContext());
			// Set this-Reference
			currentContext.setThisRef(self);
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
			TSValue result = caseBlock(block);
			leaveExecutionContext();
			return result;
		}
		catch (TSReturnValue rv) {
			// Restore execution context
			leaveExecutionContext();
			return rv.getReturnValue();				
		}

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
		TSObject thisRef = currentContext.getThisRef();
		if (outer == null) {
			currentContext = new ExecutionContext(name, currentContext);
		}
		else {
			currentContext = new ExecutionContext(name, outer);
		}
		currentContext.setThisRef(thisRef);
	}
	
	private void leaveExecutionContext() {
		currentContext = contextStack.pop();
	}
	
	private LexicalEnvironment getLexcialEnvironment(Block block) {
		LexicalEnvironment result = lexicalEnvironments.get(block);
		if (result != null)
			return result;
		List <TSValue> functions = new ArrayList<TSValue>();
		for (FunctionDefinition funcdecl : TinyscriptModelUtil.functionDeclarationsInBlock(block)) {
			TSValue functionObject = caseFunctionDefinition(funcdecl);
			functions.add(functionObject);
		}
		List <Identifier> variables = TinyscriptModelUtil.declaredVariablesInBlock(block);
		result =  new LexicalEnvironment(functions, variables);
		lexicalEnvironments.put(block, result);
		return result;
	}


}
