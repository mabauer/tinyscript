package de.mkbauer.tinyscript.interpreter;

import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.TSStacktraceElement;
import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.TinyscriptModelUtil;
import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.TinyscriptSyntaxError;
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
import de.mkbauer.tinyscript.ts.ExpressionStatement;
import de.mkbauer.tinyscript.ts.ForStatement;
import de.mkbauer.tinyscript.ts.FunctionDefinition;
import de.mkbauer.tinyscript.ts.FunctionDeclaration;
import de.mkbauer.tinyscript.ts.GroupingExpression;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.IfStatement;
import de.mkbauer.tinyscript.ts.IterableForExpression;
import de.mkbauer.tinyscript.ts.NewExpression;
import de.mkbauer.tinyscript.ts.NullObject;
import de.mkbauer.tinyscript.ts.NumberLiteral;
import de.mkbauer.tinyscript.ts.NumericForExpression;
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
import de.mkbauer.tinyscript.ts.TsPackage;
import de.mkbauer.tinyscript.ts.TypeOfExpression;
import de.mkbauer.tinyscript.ts.Unary;
import de.mkbauer.tinyscript.ts.VariableStatement;
import de.mkbauer.tinyscript.runtime.Print;
import de.mkbauer.tinyscript.runtime.array.ArrayConstructor;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;
import de.mkbauer.tinyscript.runtime.fs.FSObject;
import de.mkbauer.tinyscript.runtime.function.FunctionConstructor;
import de.mkbauer.tinyscript.runtime.math.MathObject;
import de.mkbauer.tinyscript.runtime.object.ObjectConstructor;
import de.mkbauer.tinyscript.runtime.string.StringConstructor;
import de.mkbauer.tinyscript.runtime.system.SystemObject;


/**
 * Evaluates expressions of the Tinyscript language.
 * @author markus.bauer
 *
 */
public class ExecutionVisitor /* extends TsSwitch<TSValue> */ {
	
	private ExecutionContext currentContext;
	private GlobalExecutionContext globalContext;
	
	private Deque<ExecutionContext> contextStack;
	
	private Map<Block, LexicalEnvironment> lexicalEnvironments;
	
	private TSObject objectPrototype;
	
	private OutputStream stdOut; 
	
	private boolean sandboxed = true; 
	
	protected int callDepth;
	private ResourceMonitor resourceMonitor;
	
	public ExecutionVisitor() {
		initialize();
	}
	
	public ExecutionVisitor(ResourceMonitor resourceMonitor) {
		this.resourceMonitor = resourceMonitor;
		initialize();
	}
	
	public ResourceMonitor getResourceMonitor() {
		return resourceMonitor;
	}
		
	private void initialize() {	
		
		if (resourceMonitor != null)
			resourceMonitor.start();
		
		objectPrototype = new TSObject();
		recordObjectCreation(objectPrototype);
		
		globalContext = new GlobalExecutionContext(this);
		recordObjectCreation(globalContext.getGlobalObject());
		
		currentContext = globalContext;
		contextStack = new ArrayDeque<ExecutionContext>();
		lexicalEnvironments = new HashMap<Block, LexicalEnvironment>();
				 
		TSObject globalObject = globalContext.getGlobalObject();
		
		globalObject.setPrototype(objectPrototype);

		// Caveat: Object needs to be initialized first!
		// TSObject.defineDefaultProperty(globalObject, "Object", new ObjectObject(this));
		ObjectConstructor objectconstructor = new ObjectConstructor(this);
		objectconstructor.initialize();

		TSObject.defineDefaultProperty(globalObject, "Function", new FunctionConstructor(this));
		TSObject.defineDefaultProperty(globalObject, "String", new StringConstructor(this));
		TSObject.defineDefaultProperty(globalObject, "Array", new ArrayConstructor(this));

		TSObject.defineDefaultProperty(globalObject, "Math", new MathObject(this));
		TSObject.defineDefaultProperty(globalObject, "fs", new FSObject(this));		
		TSObject.defineDefaultProperty(globalObject, "System", new SystemObject(this));
		TSObject.defineDefaultProperty(globalObject, "print", new Print(this));
		
		if (resourceMonitor != null)
			resourceMonitor.stop();
	}
	
	public void defineStdOut(OutputStream os) {
		stdOut = os;	
	}
	
	public OutputStream getStdOut() {
		return stdOut;
	}

	public boolean isSandboxed() {
		return sandboxed;
	}

	public void setSandboxed(boolean sandboxed) {
		this.sandboxed = sandboxed;
	}

	public TSObject getDefaultPrototype() {
		return objectPrototype;
	}
	
	public TSValue getObjectPrototypeFor(String objectName) {
		TSValue value = globalContext.get(objectName);
		Function object = null;
		if (value != TSValue.UNDEFINED) {
			object = (Function) value.asObject();
			TSValue prototype = object.getPrototypeProperty();
			return prototype;
		}
		return TSValue.UNDEFINED;
	}
	
	public GlobalExecutionContext getGlobalContext() {
		return globalContext;
	}

	protected ExecutionContext getCurrentContext() {
		return currentContext;
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
		case TsPackage.EXPRESSION_STATEMENT:
			return caseExpressionStatement((ExpressionStatement)object);
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
		case TsPackage.FOR_STATEMENT:
    		return caseForStatement((ForStatement) object);
		case TsPackage.FUNCTION_DEFINITION:
			return caseFunctionDefinition((FunctionDefinition) object);
		case TsPackage.ARROW_FUNCTION:
			return caseArrowFunction((FunctionDefinition) object);
		case TsPackage.RETURN_STATEMENT:
    		return caseReturnStatement((ReturnStatement) object);
		case TsPackage.GROUPING_EXPRESSION:
			return caseGroupingExpression((GroupingExpression) object);
		case TsPackage.NUMBER_LITERAL:
    		return caseNumberLiteral((NumberLiteral) object);
		case TsPackage.STRING_LITERAL:
    		return caseStringLiteral((StringLiteral) object);
		case TsPackage.BOOLEAN_LITERAL:
    		return caseBooleanLiteral((BooleanLiteral) object);
		case TsPackage.NEW_EXPRESSION:
    		return caseNewExpression((NewExpression) object);
		case TsPackage.NULL_OBJECT:
    		return caseNullObject((NullObject) object); 		
		case TsPackage.OBJECT_INITIALIZER:
    		return caseObjectInitializer((ObjectInitializer) object);
		case TsPackage.ARRAY_INITIALIZER:
    		return caseArrayInitializer((ArrayInitializer) object);
		case TsPackage.BLOCK_STATEMENT:
    		return caseBlockStatement((BlockStatement) object);
		case TsPackage.ASSERT_STATEMENT:
    		return caseAssertStatement((AssertStatement) object);
		case TsPackage.TYPE_OF_EXPRESSION:
			return caseTypeOfExpression((TypeOfExpression) object);
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
		// Start from scratch!
		currentContext = globalContext;
		contextStack.clear();
		callDepth = 0;
		if (resourceMonitor != null)
			resourceMonitor.start();
		checkMXCpuTimeAndMemory();
			
		// Hoist function declarations:
    	// Create function objects (or get them form a cache)
    	LexicalEnvironment env = getLexcialEnvironment(object.getGlobal());
		for (TSValue function : env.getFunctions()) {	
			// Create a variable in the current context pointing to each function object
			String functionName = ((Function) function.asObject()).getName();
			if (!currentContext.contains(functionName))
				currentContext.create(functionName);
			currentContext.store(functionName, function);
		}
		try {
			TSValue result = execute(object.getGlobal());

			if (resourceMonitor != null)
				resourceMonitor.stop();
			return result;
		}
		catch (TinyscriptRuntimeException e) {
			if (resourceMonitor != null)
				resourceMonitor.stop();
			if (e.getTinyscriptStacktrace() == null || (e.getTinyscriptStacktrace().length == 0))
				attachStackTrace(e);
			throw e;
		}
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
    
    public TSValue caseExpressionStatement(ExpressionStatement object) {
    	recordStatement();
    	return execute(object.getExpr());
    }    
    
    // @Override 
    public TSValue caseFunctionDefinition(FunctionDefinition object) {
    	InterpretedFunction function = new InterpretedFunction(this);
    	function.setOuterContext(currentContext);
    	function.setAst(object);
    	return new TSValue(function);
    }
    
    // @Override 
    public TSValue caseArrowFunction(FunctionDefinition object) {
    	InterpretedFunction function = new InterpretedFunction(this);
    	function.setArrowFunction(true);
    	function.setOuterContext(currentContext);
    	function.setAst(object);
    	return new TSValue(function);
    }
    
    
    // @Override
    public TSValue caseVariableStatement(VariableStatement object) {
    	recordStatement();
    	for (Expression expr : object.getVardecls()) {
        	execute(expr); 
        }
    	return TSValue.UNDEFINED;
    }
    
    // @Override
    public TSValue caseReturnStatement(ReturnStatement object) {
    	recordStatement();
    	if (callDepth == 0) {
    		throw new TinyscriptSyntaxError("Return statement is only valid inside functions", 
    				TinyscriptModelUtil.getFilenameOfASTNode(object), TinyscriptModelUtil.getLineOfASTNode(object));
    	}
    	TSValue returnValue = TSValue.UNDEFINED;
    	if (object.getExpr() != null)
    		returnValue =execute(object.getExpr());
    	throw new TSReturnValue(returnValue);
    }
    
    // @Override
    public TSValue caseAssertStatement(AssertStatement object) {
    	recordStatement();
    	TSValue cond = execute(object.getCond());
    	if (!cond.asBoolean()) {
    		throw new TinyscriptAssertationError("assert: condition is false", object);
    	}
    	return cond;
    }
  
    // @Override
    public TSValue caseIfStatement(IfStatement object) {
    	recordStatement();
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
    }
    
    // @Override
    public TSValue caseForStatement(ForStatement forstmt) {
    	recordStatement();
    	if (forstmt.getNumericForExpr() != null) {
    		return executeNumericForStatement(forstmt);
    	}
    	else {
    		return executeIterableForStatement(forstmt);
    	}
    }
    
    public TSValue executeNumericForStatement(ForStatement forstmt) {
    	// TODO: Use TSObject.toNumber/toInteger for bounds
    	NumericForExpression expr = forstmt.getNumericForExpr();
    	TSValue result = TSValue.UNDEFINED;
		TSValue startValue = execute(expr.getStart());
		TSValue stopValue = execute(expr.getStop());    		
		if (!startValue.isMathematicalInteger())
			throw new TinyscriptTypeError("for needs an integer value as first bound", expr.getStart());
		if (!stopValue.isNumber())
			throw new TinyscriptTypeError("for needs a number value as second bound", expr.getStart());
		double start = startValue.asDouble();
		double stop = stopValue.asDouble();
		double step = 1;
		if (expr.getStep() != null) {
			TSValue stepValue =  execute(expr.getStep());
			if (!stopValue.isMathematicalInteger()) 
				throw new TinyscriptTypeError("for needs an integer value as step expression", expr.getStep());
			step = stepValue.asDouble();
		}
		// TODO: If bounds are NaNs, don't loop!
		if (Double.isInfinite(start) || Double.isInfinite(stop))
			return result;
		for (double loopValue = start ; (step > 0)?(loopValue <= stop):(loopValue >= stop); loopValue = loopValue + step ) {
			checkMXCpuTimeAndMemory();
			if (forstmt.getId() != null) {
				result = executeInBlockContext("for", forstmt.getDo(), forstmt.getId(), new TSValue(loopValue));
			}
			else {
				currentContext.store(forstmt.getRef().getId().getName(), new TSValue(loopValue));
				result = executeInBlockContext("for", forstmt.getDo());
			}
		}
    	return result;
    }
    
    public TSValue executeIterableForStatement(ForStatement forstmt) {
    	IterableForExpression expr = forstmt.getIterableForExpr();
    	TSValue result = TSValue.UNDEFINED;
		TSValue iterable = execute(expr.getIterable());
		if (iterable.isArray()) {
			ArrayObject arr = (ArrayObject) iterable.asObject();
			for (int i = 0; i < arr.getLength(); i++) {
				checkMXCpuTimeAndMemory();
				TSValue loopValue = arr.item(i);
				if (forstmt.getId() != null) {
					result = executeInBlockContext("for", forstmt.getDo(), forstmt.getId(), loopValue);
				}
				else {
					currentContext.store(forstmt.getRef().getId().getName(), loopValue);
					result = executeInBlockContext("for", forstmt.getDo());
				} 
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
    	if (op.equalsIgnoreCase("!=")) {
    		return new TSValue(!left.equals(right));
    	}
    	// TODO: Make comparisons work for other types. Strategy: Implement TSValue as Comparable
    	// TODO: Respect the evaluation order. In ECMAScript: left to right
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
    	if (op.equalsIgnoreCase(">=")) {
    		if (left.isNumber() && right.isNumber())
    			return new TSValue(left.asDouble() >= right.asDouble());
    		else
    			throw new UnsupportedOperationException("Unsupported binary expression: " + op);
    	}
    	if (op.equalsIgnoreCase("<=")) {
    		if (left.isNumber() && right.isNumber())
    			return new TSValue(left.asDouble() <= right.asDouble());
    		else
    			throw new UnsupportedOperationException("Unsupported binary expression: " + op);
    	}
    	if (op.equals("+")) {
    		if (left.isNumber() && right.isNumber()) {
    			return new TSValue(left.asDouble() + right.asDouble());
    		}
    		if (left.isArray()) {
    			if (right.isArray()) {
    				return new TSValue(ArrayObject.concat(left.asArray(), right.asArray()));
    			}
    			else {
    				ArrayObject result = left.asArray().clone();
    				result.push(right);
    				return new TSValue(result);
    			}	
    		}
    		if (right.isArray()) {
    			if (left.isArray()) {
    				return new TSValue(ArrayObject.concat(left.asArray(), right.asArray()));
    			}
    			else {
    				ArrayObject result = new ArrayObject(this);
    				result.push(left);
    				result = ArrayObject.concat(result, right.asArray());
    				return new TSValue(result);
    			}
    		}
    		if (left.isString() || right.isString()) {
    			String result = left.asString() + right.asString();
    			recordStringCreation(result);
    			return new TSValue(result);
    		}
    		return TSValue.NAN;
    	}
    	if (op.equals("-")) {
    		if (left.isNumber() && right.isNumber()) {
    			return new TSValue(left.asDouble() - right.asDouble());
    		}
    		return TSValue.NAN;
    	}
    	if (op.equals("*")) {
       		if (left.isNumber() && right.isNumber()) {
    			return new TSValue(left.asDouble() * right.asDouble());
    		}
       		return TSValue.NAN;
    	}
    	if (op.equals("/")) {
       		if (left.isNumber() && right.isNumber()) {
       			// TODO: Catch division by 0
    			return new TSValue(left.asDouble() / right.asDouble());
    		}
       		return TSValue.NAN;
    	}
    	if (op.equals("%")) {
       		if (left.isNumber() && right.isNumber()) {
    			return new TSValue(left.asDouble() % right.asDouble());
    		}
       		return TSValue.NAN;
    	}
    	if (op.equals("instanceof")) {
    		if (!(right.isObject() && right.asObject() instanceof Function))
    			throw new TinyscriptTypeError("'instanceof' requires a function object as right-hand operand");
    		else {
    			Function constructor = (Function) right.asObject();
    			return new TSValue(constructor.hasInstance(left));
    		}
    				
    	}
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
    
    public TSValue caseGroupingExpression(GroupingExpression expr) {
    	TSValue result = execute(expr.getExpr());
    	return result;
    }
    
    public TSValue caseNewExpression(NewExpression expr) {
    	TSValue result = execute(expr.getExpr());
    	return result;
    }
    

    // @Override
    // TODO: Merge with assignValue
    public TSValue caseCallOrPropertyAccess(CallOrPropertyAccess expr) {
    	TSValue base = execute(expr.getExpr()); 
    	TSValue result = TSValue.UNDEFINED;
    	EObject suffix = expr.getSuffix();
		if (suffix instanceof CallOrPropertyAccessSuffix) {
			CallOrPropertyAccessSuffix callOrProp = (CallOrPropertyAccessSuffix) suffix;
			TSValue keyValue = evaluatePropertyKey(callOrProp.getProperty());
			TSObject baseasObject;
			if (!base.isObject()) {
				if (base == TSValue.NULL || base == TSValue.UNDEFINED) {
					throw new TinyscriptTypeError("Cannot access property of undefined or null", expr);
				}	
				baseasObject = TSObject.toObject(this, base);
			}
			else {
				baseasObject = base.asObject();
			}
			if (baseasObject instanceof ArrayObject && keyValue.isNumber())
				result = ((ArrayObject) baseasObject).item(keyValue.asInt());
			else
				result = baseasObject.get(keyValue.asString());
			CallSuffix callSuffix = callOrProp.getCall();
			if (callSuffix != null) {
				if (result.isObject() && (result.asObject() instanceof Function)) {
					TSObject thisRef = null;
					boolean asConstructor = (expr.getExpr() instanceof NewExpression);
					if (asConstructor) {
						thisRef = new TSObject(this, result.asObject().get("prototype").asObject());
					}
					else
						thisRef = baseasObject;
					result = processFunctionCall(expr, (Function) result.asObject(), asConstructor, thisRef, callSuffix.getArguments());
				}
				else {
					// TODO: Check for Null or Undefined!
					throw new TinyscriptTypeError("Calls are only allowed for function objects", expr);
				}	
			}		
		}
    	if 	(suffix instanceof CallSuffix) {
    		CallSuffix callSuffix = (CallSuffix) suffix;
        	if (base.isObject() && (base.asObject() instanceof Function)) {
        		TSObject thisRef = null;
        		boolean asConstructor = (expr.getExpr() instanceof NewExpression);
				if (asConstructor) {
					thisRef = new TSObject(this, base.asObject().get("prototype").asObject());
				}
        		result = processFunctionCall(expr, (Function) base.asObject(), asConstructor, thisRef, callSuffix.getArguments());
    		}
        	else {
        		// TODO: Check for Null or Undefined!
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
    		if (expr.isThis()) {
    			TSObject thisRef = currentContext.getThisRef();
    			// 'this" behaves as specified in Javascript strict mode.
    			if (thisRef != null)
    				return new TSValue(thisRef);
    			else
    				return TSValue.UNDEFINED;
    		}
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
    	String result = expr.getValue();
    	recordStringCreation(result);
    	return new TSValue(result); 
    }
    
    public TSValue caseNullObject(NullObject expr) {
    	return TSValue.NULL;
    }
    
    // @Override
    public TSValue caseObjectInitializer(ObjectInitializer expr) {
    	TSObject obj = new TSObject(this, getDefaultPrototype()); 
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
    	ArrayObject arr = new ArrayObject(this); 
    	int i = 0;
    	for (Expression itemExpr : expr.getValues()) {
    		arr.put(String.valueOf(i), execute(itemExpr));
    		i++;
    	}
    	return new TSValue(arr);
    }
    
    public TSValue caseTypeOfExpression(TypeOfExpression expr) {
    	TSValue value = currentContext.lookup(expr.getName());
    	if (expr.getSuffixes() != null) {
    		for (DotPropertyAccessSuffix suffix : expr.getSuffixes()) {
    			if (!value.isObject()) {
    				value = TSValue.UNDEFINED; 
    				break;
    			}
    			else {
    				value = value.asObject().get(suffix.getKey().getName());
    			}
    		}
    		
    	}
    	return new TSValue(value.typeOf());
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
        	TSValue base = execute(expr.getExpr());
    		if (suffix instanceof CallOrPropertyAccessSuffix) {
    			PropertyAccessSuffix propSuffix = ((CallOrPropertyAccessSuffix) suffix).getProperty();
    			TSValue keyValue = evaluatePropertyKey(propSuffix);
    			TSObject baseasObject;
    			if (!base.isObject()) {
    				if (base == TSValue.NULL || base == TSValue.UNDEFINED) {
    					throw new TinyscriptTypeError("Cannot access property of undefined or null", expr);
    				}	
    				baseasObject = TSObject.toObject(this, base);
    			}
    			else {
    				baseasObject = base.asObject();
    			}
    			if (baseasObject instanceof ArrayObject && keyValue.isNumber())
    				((ArrayObject) baseasObject).setItem(keyValue.asInt(), value);
    			else
    				baseasObject.put(keyValue.asString(), value);
    			if ( ((CallOrPropertyAccessSuffix) suffix).getCall() != null)
    				throw new TinyscriptTypeError("Invalid left-hand side expression", expr);
    			return value;
    		}
    		if (suffix instanceof CallSuffix) {
    			throw new TinyscriptTypeError("Invalid left-hand side expression", expr);
    		}
    	}
    	return TSValue.UNDEFINED;
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
		return keyExpr;
	}
	
    public TSValue processFunctionCall(EObject expr, Function functionObject, boolean asConstructor, TSObject self, List<Expression> argExprs) {
    	currentContext.currentExpression = expr;
    	
			TSValue[] args = new TSValue[argExprs.size()];
			for (int i = 0; i < argExprs.size(); i++) {
				args[i] = execute(argExprs.get(i));
			}		
		return functionObject.call(asConstructor, self, args);
    }
	
	private TSValue executeInBlockContext(String name, Block block) {
		return executeInBlockContext(name, block, null, null);
	}
	
	private TSValue executeInBlockContext(String name, Block block, Identifier variable, TSValue value) {
		// checkMXCpuTimeAndMemory();
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
				String functionName = ((Function) function.asObject()).getName();
				if (!currentContext.contains(functionName))
					currentContext.create(functionName);
				currentContext.store(functionName, function);
			}
		}
		try {
			result = caseBlock(block);
		}
		catch (TinyscriptRuntimeException e) {
			if (needsNewContext)
				leaveExecutionContext();
			throw e;
		}
		if (needsNewContext)
			leaveExecutionContext();
		return result;
	}
	
	protected ExecutionContext enterNewExecutionContext(String name) {
		return enterNewExecutionContext(name, null);
	}
	
	protected ExecutionContext enterNewExecutionContext(String name, ExecutionContext outer) {
		contextStack.push(currentContext);
		TSObject thisRef = currentContext.getThisRef();
		if (outer == null) {
			currentContext = new ExecutionContext(name, currentContext);
		}
		else {
			currentContext = new ExecutionContext(name, outer);
		}
		currentContext.setThisRef(thisRef);
		return currentContext;
	}
	
	protected void leaveExecutionContext() {
		currentContext = contextStack.pop();
	}
	
	protected LexicalEnvironment getLexcialEnvironment(Block block) {
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
	
	protected void attachStackTrace(TinyscriptRuntimeException e) {
		List<TSStacktraceElement> stacktrace = new ArrayList<TSStacktraceElement>();
		if (e.getNode() != null)
			stacktrace.add(new TSStacktraceElement(TinyscriptModelUtil.getFilenameOfASTNode(e.getNode()), 
					currentContext.name, TinyscriptModelUtil.getLineOfASTNode(e.getNode())));
		else 
			stacktrace.add(new TSStacktraceElement(currentContext.name, 0));
		for (ExecutionContext ctx: contextStack) {
			if (ctx.isFunctionContext() && ctx.currentExpression != null) {
				stacktrace.add(new TSStacktraceElement(TinyscriptModelUtil.getFilenameOfASTNode(ctx.currentExpression), 
						ctx.name, TinyscriptModelUtil.getLineOfASTNode(ctx.currentExpression)));
			}
		}
			
		TSStacktraceElement[] result = new TSStacktraceElement[0];
		result = stacktrace.toArray(result);
		e.setTinyscriptStacktrace(result);
	}

	private void recordStatement() {
		if (resourceMonitor != null) 
			resourceMonitor.recordStatement();
	}
	
	protected void recordObjectCreation(TSObject object) {
		if (resourceMonitor != null)
			resourceMonitor.recordObjectCreation(object);
	}
	
	protected void recordObjectSizeChange(TSObject object) {
		if (resourceMonitor != null)
			resourceMonitor.recordObjectSizeChange(object);
	}
	
	public void recordStringCreation(String str) {
		if (resourceMonitor != null)
			resourceMonitor.recordStringCreation(str);
	}
	
	protected void checkMXCpuTimeAndMemory() {
		if (resourceMonitor != null)
			resourceMonitor.checkMXCpuTimeAndMemory();
	}
}
