package de.mkbauer.tinyscript.interpreter;

import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResourceSet;

import de.mkbauer.tinyscript.TSStacktraceElement;
import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.TinyscriptModelUtil;
import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.ts.ArrayInitializer;
import de.mkbauer.tinyscript.ts.AssertStatement;
import de.mkbauer.tinyscript.ts.BinaryExpression;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.BlockStatement;
import de.mkbauer.tinyscript.ts.BooleanLiteral;
import de.mkbauer.tinyscript.ts.BreakStatement;
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
import de.mkbauer.tinyscript.ts.TsFactory;
import de.mkbauer.tinyscript.ts.TsPackage;
import de.mkbauer.tinyscript.ts.Unary;
import de.mkbauer.tinyscript.ts.VariableStatement;
import de.mkbauer.tinyscript.ts.WhileStatement;
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
public class TinyscriptEngine {
	
	private static final String BUILTINSFILE = "__builtins.tys";

	private XtextResourceSet resourceSet;
	
	private ExecutionContext currentContext;
	private GlobalExecutionContext globalContext;
	
	private Deque<ExecutionContext> contextStack;
	
	private Map<Block, LexicalEnvironment> lexicalEnvironments;
	
	private TSObject objectPrototype;
	
	private OutputStream stdOut; 
	
	private boolean sandboxed = true; 
	
	protected int callDepth;
	private ResourceMonitor resourceMonitor;
	
	public TinyscriptEngine(XtextResourceSet resourceSet) {
		this.resourceSet = resourceSet;
		initialize();
	}
	
	public TinyscriptEngine(XtextResourceSet resourceSet, ResourceMonitor resourceMonitor) {
		this.resourceSet = resourceSet;
		this.resourceMonitor = resourceMonitor;
		initialize();
	}
	
	public ResourceMonitor getResourceMonitor() {
		return resourceMonitor;
	}
		
	private void initialize() {	
		
		if (resourceMonitor != null)
			resourceMonitor.start();
		
		objectPrototype = new TSObject(this);
		
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
		addIdentifierToBuiltins("Object");

		addToGlobalObject("Function", new FunctionConstructor(this));
		addToGlobalObject("String", new StringConstructor(this));
		addToGlobalObject("Array", new ArrayConstructor(this));

		addToGlobalObject("Math", new MathObject(this));
		addToGlobalObject("fs", new FSObject(this));		
		addToGlobalObject("System", new SystemObject(this));
		addToGlobalObject("print", Print.create(this));
		
		if (resourceMonitor != null)
			resourceMonitor.stop();
	}
	
	private void addIdentifierToBuiltins(String name) {
		TsFactory factory = TsFactory.eINSTANCE;
		Identifier id = factory.createIdentifier();
		id.setName(name);
		URI builtinsUri = URI.createURI(BUILTINSFILE);
		Resource builtinsResource = resourceSet.getResource(builtinsUri, false);
		if (builtinsResource == null) {
			builtinsResource = resourceSet.createResource(builtinsUri);
		}
		builtinsResource.getContents().add(id);
	}
	
	private void addToGlobalObject(String name, TSObject value) {
		TSObject globalObject = globalContext.getGlobalObject();
		globalObject.defineDefaultProperty(name, value);
		addIdentifierToBuiltins(name);	
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

	/**
	 * Used by builtins that must not be available in sandboxed mode (e.g. file access).
	 */
	public void enforceSandboxing(String builtinName) {
		if (sandboxed) {
			throw new TinyscriptUnsupportedOperationError("Builtin function " + builtinName + " is not available when running sandboxed.",
					currentContext.currentExpression);
		}
	}

	public void setSandboxed(boolean sandboxed) {
		this.sandboxed = sandboxed;
	}

	public TSObject getDefaultPrototype() {
		return objectPrototype;
	}
	
	public BuiltinConstructor getConstructor(String name) {
		if (globalContext.get(name) instanceof BuiltinConstructor ctor)
			return ctor;
		throw new TinyscriptRuntimeException("Could not find constructor " + name);
	}
	
	public BuiltinFunction defineBuiltinFunction(String name, BuiltinFunctionImplementation implementation, int length) {
		BuiltinFunction function = new BuiltinFunction(this);
		function.setName(name);
		function.setLength(length);
		function.defineImplementation(implementation);
		return function;
	}
	
	public GlobalExecutionContext getGlobalContext() {
		return globalContext;
	}

	protected ExecutionContext getCurrentContext() {
		return currentContext;
	}
    
    public TSValue execute(EObject object) {
    	try {
    		return dispatch(object);
    	}
    	catch (TinyscriptRuntimeException e) {
    		// Errors raised without a node (e.g. resource limit violations) get the innermost node being executed
    		e.attachNodeIfAbsent(object);
    		throw e;
    	}
    }

    private TSValue dispatch(EObject object) {
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
		case TsPackage.WHILE_STATEMENT:
    		return caseWhileStatement((WhileStatement) object);
		case TsPackage.BREAK_STATEMENT:
    		return caseBreakStatement((BreakStatement) object);
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
		case TsPackage.TINYSCRIPT:
			return caseTinyscript((Tinyscript) object);
		default:
    		return defaultCase(object);
    	}
    }

	
	public TSValue defaultCase(EObject object) {
		throw new TinyscriptUnsupportedOperationError("Unsupported script node: " + object.eClass().getName(), object);
	}
  	
	public TSValue caseTinyscript(Tinyscript object) {
		// Start from scratch!
		currentContext = globalContext;
		contextStack.clear();
		callDepth = 0;
		if (resourceMonitor != null)
			resourceMonitor.start();
		checkMXCpuTimeAndMemory();
			
		hoistFunctionDeclarations(getLexicalEnvironment(object.getGlobal()));
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

	public TSValue caseBlock(Block object) {
    	TSValue result = TSValue.UNDEFINED;  	
        for (Statement s : object.getStatements()) {
        	if (!(s instanceof FunctionDeclaration))
        		result = execute(s); 
        }
        return result;
    }
    
    public TSValue caseBlockStatement(BlockStatement object) {
    	return executeInBlockContext("block", object.getBlock());
    }
    
    public TSValue caseExpressionStatement(ExpressionStatement object) {
    	recordStatement();
    	return execute(object.getExpr());
    }    
    
    public TSValue caseFunctionDefinition(FunctionDefinition object) {
    	return createFunction(object, false);
    }

    public TSValue caseArrowFunction(FunctionDefinition object) {
    	return createFunction(object, true);
    }

    /**
     * Creates a function object that closes over the current execution context.
     */
    private TSValue createFunction(FunctionDefinition definition, boolean isArrowFunction) {
    	InterpretedFunction function = new InterpretedFunction(this);
    	function.setArrowFunction(isArrowFunction);
    	function.setOuterContext(currentContext);
    	function.setAst(definition);
    	return function;
    }

    
    public TSValue caseVariableStatement(VariableStatement object) {
    	recordStatement();
    	for (Expression expr : object.getVardecls()) {
        	execute(expr); 
        }
    	return TSValue.UNDEFINED;
    }
    
    // The validator ensures that return only occurs inside functions
    public TSValue caseReturnStatement(ReturnStatement object) {
    	recordStatement();
    	TSValue returnValue = (object.getExpr() != null) ? execute(object.getExpr()) : TSValue.UNDEFINED;
    	throw new TSReturnValue(returnValue);
    }
    
    public TSValue caseAssertStatement(AssertStatement object) {
    	recordStatement();
    	TSValue cond = execute(object.getCond());
    	if (!cond.asBoolean()) {
    		throw new TinyscriptAssertationError("assert: condition is false", object);
    	}
    	return cond;
    }
  
    public TSValue caseIfStatement(IfStatement object) {
    	recordStatement();
    	if (execute(object.getCond()).asBoolean())
    		return executeInBlockContext("if", object.getThen());
    	if (object.getElse() != null)
    		return caseElseStatement(object.getElse());
    	return TSValue.UNDEFINED;
    }
    
    public TSValue caseElseStatement(ElseStatement object) {
    	if (object.getElseIf() != null)
    		return caseIfStatement(object.getElseIf());
    	return executeInBlockContext("else", object.getElse());
    }
    
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
			if (!stepValue.isMathematicalInteger())
				throw new TinyscriptTypeError("for needs an integer value as step expression", expr.getStep());
			step = stepValue.asDouble();
			if (step == 0)
				throw new TinyscriptTypeError("for needs a step other than 0", expr.getStep());
		}
		if (Double.isInfinite(start) || Double.isInfinite(stop))
			return result;
		try {
			for (double loopValue = start ; (step > 0)?(loopValue <= stop):(loopValue >= stop); loopValue = loopValue + step ) {
				result = executeLoopIteration(forstmt, TSValue.of(loopValue));
			}
		}
		catch (TSBreak b) {
			// leave the loop
		}
    	return result;
    }

    public TSValue executeIterableForStatement(ForStatement forstmt) {
    	IterableForExpression expr = forstmt.getIterableForExpr();
    	TSValue result = TSValue.UNDEFINED;
		TSValue iterable = execute(expr.getIterable());
		if (iterable instanceof ArrayObject arr) {
			try {
				for (int i = 0; i < arr.getLength(); i++) {
					result = executeLoopIteration(forstmt, arr.item(i));
				}
			}
			catch (TSBreak b) {
				// leave the loop
			}
		}
		return result;
    }

    public TSValue caseWhileStatement(WhileStatement whilestmt) {
    	TSValue result = TSValue.UNDEFINED;
    	try {
    		while (true) {
    			countLoopIteration();
    			if (!execute(whilestmt.getCond()).asBoolean())
    				break;
    			result = executeInBlockContext("while", whilestmt.getDo());
    		}
    	}
    	catch (TSBreak b) {
    		// leave the loop
    	}
    	return result;
    }

    /**
     * Each loop iteration counts as a statement, so that even a loop with an empty
     * body (or an endless one) is bound by the resource limits.
     */
    private void countLoopIteration() {
    	recordStatement();
		checkMXCpuTimeAndMemory();
    }

    /**
     * Executes the body of a for loop once with the given loop value.
     */
    private TSValue executeLoopIteration(ForStatement forstmt, TSValue loopValue) {
    	countLoopIteration();
		if (forstmt.getId() != null) {
			return executeInBlockContext("for", forstmt.getDo(), forstmt.getId(), loopValue);
		}
		currentContext.store(forstmt.getRef().getId().getName(), loopValue);
		return executeInBlockContext("for", forstmt.getDo());
    }

    public TSValue caseBreakStatement(BreakStatement object) {
    	recordStatement();
    	throw TSBreak.INSTANCE;
    }
    
    public TSValue caseBinaryExpression(BinaryExpression expr) {
    	String op = expr.getOp();
    	// Operators that evaluate their right operand only when needed
    	switch (op) {
    		case "=":
    			return assignValue(expr.getLeft(), execute(expr.getRight()));
    		case "&&":
    			return TSValue.of(execute(expr.getLeft()).asBoolean() && execute(expr.getRight()).asBoolean());
    		case "||":
    			return TSValue.of(execute(expr.getLeft()).asBoolean() || execute(expr.getRight()).asBoolean());
    	}
    	TSValue left = execute(expr.getLeft());
    	TSValue right = execute(expr.getRight());
    	return switch (op) {
    		case "==" -> TSValue.of(left.tinyscriptEquals(right));
    		case "!=" -> TSValue.of(!left.tinyscriptEquals(right));
    		case "<", "<=", ">", ">=" -> compare(op, left, right, expr);
    		case "+" -> add(left, right, expr);
    		case "-", "*", "/", "%" -> arithmetic(op, left, right);
    		case "instanceof" -> instanceOf(left, right);
    		default -> throw new TinyscriptUnsupportedOperationError("Unsupported binary expression: " + op, expr);
    	};
    }

    /**
     * Compares two numbers or two strings (lexicographically). Comparing values
     * of other or mixed types is a type error: there is no implicit conversion.
     */
    private TSValue compare(String op, TSValue left, TSValue right, BinaryExpression expr) {
    	int comparison;
    	if (left.isNumber() && right.isNumber())
    		comparison = Double.compare(left.asDouble(), right.asDouble());
    	else if (left.isString() && right.isString())
    		comparison = left.asString().compareTo(right.asString());
    	else
    		throw new TinyscriptTypeError("'" + op + "' compares two numbers or two strings, not "
    				+ left.typeOf() + " and " + right.typeOf(), expr);
    	return TSValue.of(switch (op) {
    		case "<" -> comparison < 0;
    		case "<=" -> comparison <= 0;
    		case ">" -> comparison > 0;
    		default -> comparison >= 0;
    	});
    }

    /**
     * + adds numbers, concatenates strings (if either operand is a string) and arrays.
     */
    private TSValue add(TSValue left, TSValue right, BinaryExpression expr) {
    	if (left.isString() || right.isString()) {
    		String result = left.asString() + right.asString();
    		recordStringCreation(result);
    		return TSValue.of(result);
    	}
    	if (left instanceof ArrayObject leftArray && right instanceof ArrayObject rightArray) {
    		ArrayObject result = (ArrayObject) getConstructor(ArrayConstructor.NAME).createObject();
    		leftArray.getItems().forEach(result::push);
    		rightArray.getItems().forEach(result::push);
    		return result;
    	}
    	if (left instanceof ArrayObject || right instanceof ArrayObject)
    		throw new TinyscriptTypeError("'+' can only add an array to another array or to a string", expr);
    	if (left instanceof TSNumber l && right instanceof TSNumber r)
    		return TSValue.of(l.value() + r.value());
    	return TSValue.NAN;
    }

    private TSValue arithmetic(String op, TSValue left, TSValue right) {
    	if (!(left.isNumber() && right.isNumber()))
    		return TSValue.NAN;
    	double l = left.asDouble();
    	double r = right.asDouble();
    	return TSValue.of(switch (op) {
    		case "-" -> l - r;
    		case "*" -> l * r;
    		case "/" -> l / r;
    		default -> l % r;
    	});
    }

    private TSValue instanceOf(TSValue left, TSValue right) {
    	if (!(right instanceof Function constructor))
    		throw new TinyscriptTypeError("'instanceof' requires a function object as right-hand operand");
    	return TSValue.of(constructor.hasInstance(left));
    }

    public TSValue caseUnary(Unary expr) {
    	TSValue value = execute(expr.getExpr());
    	return switch (expr.getOp()) {
    		case "-" -> {
    			if (!value.isNumber())
    				throw new TinyscriptTypeError("Unsupported unary expression operand: " + expr.getExpr());
    			yield TSValue.of(-value.asDouble());
    		}
    		case "!" -> TSValue.of(!value.asBoolean());
    		case "typeof" -> TSValue.of(value.typeOf());
    		default -> throw new TinyscriptUnsupportedOperationError("Unsupported unary expression: " + expr.getOp(), expr);
    	};
    }

    public TSValue caseGroupingExpression(GroupingExpression expr) {
    	return execute(expr.getExpr());
    }

    public TSValue caseNewExpression(NewExpression expr) {
    	TSValue constructor = execute(expr.getExpr());
    	// new Foo(...): the call that follows constructs the object (see processFunctionCall)
    	if (expr.eContainer() instanceof CallOrPropertyAccess call && isConstructorCall(call))
    		return constructor;
    	// new Foo: like Javascript, construct without arguments
    	if (!(constructor instanceof Function function))
    		throw new TinyscriptTypeError("'new' requires a function", expr);
    	return construct(function, new TSValue[0]);
    }

    /**
     * new Foo(...) or new ns.Foo(...): a call right after "new"; like Javascript,
     * the called function (Foo or ns.Foo) is the constructor.
     */
    private static boolean isConstructorCall(CallOrPropertyAccess expr) {
    	if (!(expr.getExpr() instanceof NewExpression))
    		return false;
    	return expr.getSuffix() instanceof CallSuffix
    			|| (expr.getSuffix() instanceof CallOrPropertyAccessSuffix suffix && suffix.getCall() != null);
    }
    

    public TSValue caseCallOrPropertyAccess(CallOrPropertyAccess expr) {
    	TSValue base = execute(expr.getExpr());
    	EObject suffix = expr.getSuffix();
		if (suffix instanceof CallOrPropertyAccessSuffix callOrProp) {
			// object.property or object.method(...)
			TSObject object = toObjectForPropertyAccess(base, expr);
			TSValue property = getProperty(object, evaluatePropertyKey(callOrProp.getProperty()));
			if (callOrProp.getCall() == null)
				return property;
			return callFunction(expr, property, object, callOrProp.getCall());
		}
    	if (suffix instanceof CallSuffix call) {
    		// function(...)
    		return callFunction(expr, base, null, call);
    	}
    	return TSValue.UNDEFINED;
    }

    private TSValue callFunction(CallOrPropertyAccess expr, TSValue function, TSObject thisRef, CallSuffix call) {
    	if (!(function instanceof Function functionObject))
    		throw new TinyscriptTypeError("Calls are only allowed for function objects", expr);
    	return processFunctionCall(expr, functionObject, thisRef, call.getArguments());
    }

    /**
     * Property access on a primitive value (e.g. a string) uses its wrapper object.
     */
    private TSObject toObjectForPropertyAccess(TSValue base, CallOrPropertyAccess expr) {
    	if (base instanceof TSObject object)
    		return object;
    	if (base == TSValue.NULL || base == TSValue.UNDEFINED)
    		throw new TinyscriptTypeError("Cannot access property of undefined or null", expr);
    	return TSObject.toObject(this, base);
    }

    // Arrays store numeric keys as items
    private TSValue getProperty(TSObject object, TSValue key) {
    	if (object instanceof ArrayObject arr && isArrayIndex(key))
    		return arr.item(key.asInt());
    	return object.get(key.asString());
    }

    private void putProperty(TSObject object, TSValue key, TSValue value) {
    	if (object instanceof ArrayObject arr && isArrayIndex(key))
    		arr.setItem(key.asInt(), value);
    	else
    		object.put(key.asString(), value);
    }

    // Like Javascript, only non-negative integers are array indexes; a[-1] or a[1.5] are properties
    private static boolean isArrayIndex(TSValue key) {
    	return key.isMathematicalInteger() && key.asDouble() >= 0 && key.asDouble() <= Integer.MAX_VALUE;
    }

    public TSValue casePropertyName(PropertyName expr) {
    	if (expr.getName() != null)
    		return TSValue.of(expr.getName());
    	return execute(expr.getExpr());
    }
    
    public TSValue caseIdentifier(Identifier expr) {
   		currentContext.create(expr.getName());
   		return TSValue.UNDEFINED;
    }
    
    public TSValue caseReference(Reference expr) {
		if (expr.isThis()) {
			// 'this' behaves as specified in Javascript strict mode.
			TSObject thisRef = currentContext.getThisRef();
			return (thisRef != null) ? thisRef : TSValue.UNDEFINED;
		}
		return currentContext.lookup(expr.getId().getName());
    }
    
    public TSValue caseBooleanLiteral(BooleanLiteral expr) {
    	return TSValue.of(expr.isValue()); 
    }
    
    public TSValue caseNumberLiteral(NumberLiteral expr) {
    	return TSValue.of(expr.getValue()); 
    }
    
    public TSValue caseStringLiteral(StringLiteral expr) {
    	String result = expr.getValue();
    	recordStringCreation(result);
    	return TSValue.of(result); 
    }
    
    public TSValue caseNullObject(NullObject expr) {
    	return TSValue.NULL;
    }
    
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
    	return obj;
    }
    
    public TSValue caseArrayInitializer(ArrayInitializer expr) {
    	ArrayObject arr = (ArrayObject) getConstructor(ArrayConstructor.NAME).createObject(); 
    	int i = 0;
    	for (Expression itemExpr : expr.getValues()) {
    		arr.put(String.valueOf(i), execute(itemExpr));
    		i++;
    	}
    	return arr;
    }
    
    
    public TSValue assignValue(Expression left, TSValue value) {
    	
    	if (left instanceof Reference reference) {
    	    if (reference.isThis())
    	    	throw new TinyscriptReferenceError("Cannot assign a value to 'this'", left);
    		currentContext.store(reference.getId().getName(), value);
    		return value;
    	}
    	if (left instanceof Identifier identifier) {
    		// var x = value
    		currentContext.create(identifier.getName());
    		currentContext.store(identifier.getName(), value);
    		return value;
    	}
    	if (left instanceof CallOrPropertyAccess expr) {
    		// Only object.property = value is a valid assignment target, not a call
    		if (!(expr.getSuffix() instanceof CallOrPropertyAccessSuffix callOrProp) || callOrProp.getCall() != null)
    			throw new TinyscriptTypeError("Invalid left-hand side expression", expr);
        	TSValue base = execute(expr.getExpr());
			TSValue key = evaluatePropertyKey(callOrProp.getProperty());
			putProperty(toObjectForPropertyAccess(base, expr), key, value);
			return value;
    	}
    	return TSValue.UNDEFINED;
    }

	private TSValue evaluatePropertyKey(PropertyAccessSuffix suffix) {
		return switch (suffix) {
			case DotPropertyAccessSuffix dot -> execute(dot.getKey());
			case ComputedPropertyAccessSuffix computed -> execute(computed.getKey());
			default -> throw new TinyscriptUnsupportedOperationError("Unsupported property access", suffix);
		};
	}
	
    public TSValue processFunctionCall(CallOrPropertyAccess expr, Function functionObject, TSObject self, List<Expression> argExprs) {
    	currentContext.currentExpression = expr;
		TSValue[] args = new TSValue[argExprs.size()];
		for (int i = 0; i < argExprs.size(); i++) {
			args[i] = execute(argExprs.get(i));
		}
		if (isConstructorCall(expr))
			return construct(functionObject, args);
		return functionObject.apply(self, args);
    }

	/**
	 * Calls a function as a constructor: its this-reference points to a newly created
	 * object derived from the constructor's prototype property. Built-in constructors
	 * create their objects with their own createObject() instead.
	 */
	private TSObject construct(Function constructor, TSValue[] args) {
		TSObject self;
		if (constructor instanceof BuiltinConstructor builtin) {
			self = builtin.createObject();
		}
		else {
			// Like Javascript: if the prototype property is not an object, use Object.prototype
			TSObject proto = constructor.get("prototype") instanceof TSObject p ? p : getDefaultPrototype();
			self = new TSObject(this, proto);
		}
		constructor.apply(self, args);
		return self;
	}
	
	private TSValue executeInBlockContext(String name, Block block) {
		return executeInBlockContext(name, block, null, null);
	}
	
	private TSValue executeInBlockContext(String name, Block block, Identifier variable, TSValue value) {
    	LexicalEnvironment env = getLexicalEnvironment(block);
    	if ((variable == null) && env.getFunctionDeclarations().isEmpty() && env.getVariables().isEmpty())
    		return caseBlock(block);

		enterNewExecutionContext(name);
		// Leave the context however the block ends: normally, with an error,
		// or with a return/break that unwinds through it
		try {
			if (variable != null) {
				currentContext.create(variable.getName());
				currentContext.store(variable.getName(), value);
			}
			hoistFunctionDeclarations(env);
			return caseBlock(block);
		}
		finally {
			leaveExecutionContext();
		}
	}

	/**
	 * Creates a variable for each function declared in the block, pointing to a new
	 * function object that closes over the current execution context.
	 */
	protected void hoistFunctionDeclarations(LexicalEnvironment env) {
		for (FunctionDefinition declaration : env.getFunctionDeclarations()) {
			String functionName = declaration.getId().getName();
			if (!currentContext.contains(functionName))
				currentContext.create(functionName);
			currentContext.store(functionName, caseFunctionDefinition(declaration));
		}
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
	
	protected LexicalEnvironment getLexicalEnvironment(Block block) {
		LexicalEnvironment result = lexicalEnvironments.get(block);
		if (result != null)
			return result;
		result = new LexicalEnvironment(TinyscriptModelUtil.functionDeclarationsInBlock(block),
				TinyscriptModelUtil.declaredVariablesInBlock(block));
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

	/**
	 * Checks the object size limit before an object grows to {@code size} entries.
	 */
	public void checkObjectSize(int size) {
		if (resourceMonitor != null)
			resourceMonitor.checkObjectSize(size);
	}

	public void recordOutput(int length) {
		if (resourceMonitor != null)
			resourceMonitor.recordOutput(length);
	}
	
	protected void checkMXCpuTimeAndMemory() {
		if (resourceMonitor != null)
			resourceMonitor.checkMXCpuTimeAndMemory();
	}
}
