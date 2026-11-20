package de.mkbauer.tinyscript.interpreter;

import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

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
import de.mkbauer.tinyscript.ts.ConditionalExpression;
import de.mkbauer.tinyscript.ts.ContinueStatement;
import de.mkbauer.tinyscript.ts.ThrowStatement;
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
import de.mkbauer.tinyscript.ts.ImportStatement;
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
import de.mkbauer.tinyscript.scoping.SharedGlobals;
import de.mkbauer.tinyscript.runtime.array.ArrayConstructor;
import de.mkbauer.tinyscript.runtime.number.NumberFunctions;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;
import de.mkbauer.tinyscript.runtime.collection.MapConstructor;
import de.mkbauer.tinyscript.runtime.error.ErrorConstructor;
import de.mkbauer.tinyscript.runtime.error.ErrorObject;
import de.mkbauer.tinyscript.runtime.collection.MapObject;
import de.mkbauer.tinyscript.runtime.collection.SetConstructor;
import de.mkbauer.tinyscript.runtime.collection.SetObject;
import de.mkbauer.tinyscript.runtime.string.StringObject;
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

	// Modules: how imports are resolved (null: no imports), and the modules loaded so far
	private ModuleLoader moduleLoader;
	private final Map<URI, ModuleExports> loadedModules = new HashMap<>();
	private final Deque<URI> modulesBeingLoaded = new ArrayDeque<>();
	
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
		addToGlobalObject("Map", new MapConstructor(this));
		addToGlobalObject("Set", new SetConstructor(this));
		addToGlobalObject("Error", new ErrorConstructor(this));

		addToGlobalObject("Number", NumberFunctions.createNumber(this));
		addToGlobalObject("parseInt", NumberFunctions.createParseInt(this));
		addToGlobalObject("isNaN", NumberFunctions.createIsNaN(this));
		addToGlobalObject("Math", new MathObject(this));
		addToGlobalObject("System", new SystemObject(this));
		addToGlobalObject("print", Print.create(this));
		
		if (resourceMonitor != null)
			resourceMonitor.stop();
	}
	
	/**
	 * Sets System.args, the arguments of the script (e.g. from the command line).
	 */
	public void setScriptArguments(List<String> arguments) {
		((SystemObject) globalContext.getGlobalObject().get("System")).setArguments(arguments);
	}

	private void addIdentifierToBuiltins(String name) {
		TsFactory factory = TsFactory.eINSTANCE;
		Identifier id = factory.createIdentifier();
		id.setName(name);
		URI builtinsUri = URI.createURI(BUILTINSFILE);
		Resource builtinsResource = resourceSet.getResource(builtinsUri, false);
		if (builtinsResource == null) {
			builtinsResource = resourceSet.createResource(builtinsUri);
			SharedGlobals.share(builtinsResource);
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

	public void setSandboxed(boolean sandboxed) {
		this.sandboxed = sandboxed;
	}

	/**
	 * Enables imports: {@code loader} resolves the paths of imports to modules.
	 */
	public void setModuleLoader(ModuleLoader loader) {
		this.moduleLoader = loader;
	}

	public TSObject getDefaultPrototype() {
		return objectPrototype;
	}
	
	public BuiltinConstructor getConstructor(String name) {
		if (globalContext.get(name) instanceof BuiltinConstructor ctor)
			return ctor;
		throw new TinyscriptRuntimeException("Could not find constructor " + name);
	}
	
	/**
	 * A builtin function that takes exactly {@code length} arguments.
	 */
	public BuiltinFunction defineBuiltinFunction(String name, BuiltinFunctionImplementation implementation, int length) {
		return defineBuiltinFunction(name, implementation, length, length);
	}

	/**
	 * A builtin function that takes {@code minArgs} to {@code maxArgs} arguments
	 * ({@link BuiltinFunction#VARIADIC}: any number).
	 */
	public BuiltinFunction defineBuiltinFunction(String name, BuiltinFunctionImplementation implementation, int minArgs, int maxArgs) {
		BuiltinFunction function = new BuiltinFunction(this);
		function.setName(name);
		function.setLength(minArgs);
		function.setMaxArgs(maxArgs);
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
		case TsPackage.BLOCK, TsPackage.ARROW_EXPRESSION_BODY:
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
		case TsPackage.CONTINUE_STATEMENT:
    		return caseContinueStatement((ContinueStatement) object);
		case TsPackage.THROW_STATEMENT:
    		return caseThrowStatement((ThrowStatement) object);
		case TsPackage.CONDITIONAL_EXPRESSION:
    		return caseConditionalExpression((ConditionalExpression) object);
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
			
		// The main script is the root of the import chain (to detect cycles back to it)
		modulesBeingLoaded.clear();
		modulesBeingLoaded.push(object.eResource().getURI());
		try {
			bindImports(object);
			hoistFunctionDeclarations(getLexicalEnvironment(object.getGlobal()));
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

	// =========================================================================
	// Modules
	// =========================================================================

	/**
	 * Binds the names imported by {@code script} in the current context: the named
	 * exports, or the whole exports object for {@code import * as m}.
	 */
	private void bindImports(Tinyscript script) {
		for (ImportStatement importStatement : script.getImports()) {
			try {
				ModuleExports exports = importModule(importStatement);
				if (importStatement.getNamespace() != null) {
					bindImport(importStatement.getNamespace().getName(), exports);
				}
				for (Identifier name : importStatement.getNames()) {
					if (!exports.hasOwnProperty(name.getName()))
						throw new TinyscriptReferenceError(exports.getModuleName() + " does not export \"" + name.getName() + "\"");
					bindImport(name.getName(), exports.get(name.getName()));
				}
			}
			catch (TinyscriptRuntimeException e) {
				e.attachNodeIfAbsent(importStatement);
				throw e;
			}
		}
	}

	private void bindImport(String name, TSValue value) {
		if (!currentContext.contains(name))
			currentContext.create(name);
		currentContext.store(name, value);
	}

	private ModuleExports importModule(ImportStatement importStatement) {
		URI module = Modules.resolve(moduleLoader, importStatement);
		ModuleExports exports = loadedModules.get(module);
		if (exports != null)
			return exports;
		if (modulesBeingLoaded.contains(module))
			throw Modules.cyclicImport(modulesBeingLoaded, module);
		exports = Modules.isNative(module) ? Modules.createNative(module, this, sandboxed) : executeModule(module);
		loadedModules.put(module, exports);
		return exports;
	}

	/**
	 * Loads, validates and runs a module in its own context (which only sees the builtins),
	 * and returns its exports.
	 */
	private ModuleExports executeModule(URI module) {
		Tinyscript script = Modules.load(resourceSet, module);
		modulesBeingLoaded.push(module);
		enterNewExecutionContext("module " + module.lastSegment(), globalContext);
		try {
			bindImports(script);
			hoistFunctionDeclarations(getLexicalEnvironment(script.getGlobal()));
			execute(script.getGlobal());
			Map<String, TSValue> exports = new LinkedHashMap<>();
			for (Identifier name : TinyscriptModelUtil.exportedIdentifiers(script))
				exports.put(name.getName(), currentContext.lookup(name.getName()));
			return new ModuleExports(this, Modules.displayName(module), exports);
		}
		finally {
			leaveExecutionContext();
			modulesBeingLoaded.pop();
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
    	if (object.getIncrement() != null)
    		return increment(object);
    	return execute(object.getExpr());
    }

    // i++; and i--; (only statements)
    private TSValue increment(ExpressionStatement object) {
    	String op = object.getIncrement();
    	return updateValue(object.getExpr(), current -> {
    		if (!(current instanceof TSNumber number))
    			throw new TinyscriptTypeError("'" + op + "' needs a number, not " + current.typeOf(), object);
    		return TSValue.of(op.equals("++") ? number.value() + 1 : number.value() - 1);
    	});
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
    	if (!cond.asCondition()) {
    		throw new TinyscriptAssertationError("assert: condition is false", object);
    	}
    	return cond;
    }
  
    public TSValue caseIfStatement(IfStatement object) {
    	recordStatement();
    	if (execute(object.getCond()).asCondition())
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
		try {
			if (iterable instanceof ArrayObject arr) {
				// Like Javascript: sees the items that the loop body adds
				for (int i = 0; i < arr.getLength(); i++) {
					result = executeLoopIteration(forstmt, arr.item(i));
				}
				return result;
			}
			List<TSValue> items = iterableItems(iterable);
			if (items == null)
				throw new TinyscriptTypeError("for needs an array, a string, a Map or a Set, not " + iterable.typeOf(), expr.getIterable());
			for (TSValue item : items) {
				result = executeLoopIteration(forstmt, item);
			}
		}
		catch (TSBreak b) {
			// leave the loop
		}
		return result;
    }

    /**
     * The items of an iterable: those of an array, the characters of a string, the values
     * of a Set or the entries [key, value] of a Map (a copy, so that a loop may change the
     * Set or Map); null if {@code value} is not iterable.
     */
    public List<TSValue> iterableItems(TSValue value) {
    	return switch (value) {
    		case ArrayObject arr -> new ArrayList<>(arr.getItems());
    		case TSString s -> characters(s.value());
    		case StringObject s -> characters(s.getValue());
    		case SetObject set -> new ArrayList<>(set.getValues());
    		case MapObject map -> map.getEntries().entrySet().stream()
    				.map(entry -> {
    					ArrayObject pair = (ArrayObject) getConstructor(ArrayConstructor.NAME).createObject();
    					pair.push(entry.getKey());
    					pair.push(entry.getValue());
    					return (TSValue) pair;
    				})
    				.toList();
    		default -> null;
    	};
    }

    private static List<TSValue> characters(String str) {
    	List<TSValue> result = new ArrayList<>(str.length());
    	for (int i = 0; i < str.length(); i++)
    		result.add(TSValue.of(str.substring(i, i + 1)));
    	return result;
    }

    public TSValue caseWhileStatement(WhileStatement whilestmt) {
    	TSValue result = TSValue.UNDEFINED;
    	try {
    		while (true) {
    			countLoopIteration();
    			if (!execute(whilestmt.getCond()).asCondition())
    				break;
    			try {
    				result = executeInBlockContext("while", whilestmt.getDo());
    			}
    			catch (TSContinue c) {
    				// go on with the next iteration
    			}
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
    	try {
			if (forstmt.getId() != null) {
				return executeInBlockContext("for", forstmt.getDo(), forstmt.getId(), loopValue);
			}
			currentContext.store(forstmt.getRef().getId().getName(), loopValue);
			return executeInBlockContext("for", forstmt.getDo());
    	}
    	catch (TSContinue c) {
    		return TSValue.UNDEFINED;
    	}
    }

    public TSValue caseBreakStatement(BreakStatement object) {
    	recordStatement();
    	throw TSBreak.INSTANCE;
    }

    public TSValue caseContinueStatement(ContinueStatement object) {
    	recordStatement();
    	throw TSContinue.INSTANCE;
    }

    // throw value; ends the script: the message is that of an Error object, or the value as a string
    public TSValue caseThrowStatement(ThrowStatement object) {
    	recordStatement();
    	TSValue value = execute(object.getExpr());
    	String message = value instanceof ErrorObject error ? error.getMessage() : TSObject.stringify(value);
    	throw new TinyscriptError(message, value, object);
    }

    // cond ? a : b evaluates only one of a and b
    public TSValue caseConditionalExpression(ConditionalExpression expr) {
    	return execute(expr.getCond()).asCondition() ? execute(expr.getThen()) : execute(expr.getElse());
    }
    
    public TSValue caseBinaryExpression(BinaryExpression expr) {
    	String op = expr.getOp();
    	// Operators that evaluate their right operand only when needed
    	switch (op) {
    		case "=":
    			return assignValue(expr.getLeft(), execute(expr.getRight()));
    		case "+=", "-=", "*=", "/=", "%=":
    			// x op= y is x = x op y, but x (e.g. a[f()]) is evaluated only once
    			return updateValue(expr.getLeft(), current -> applyOperator(op.substring(0, 1), current, execute(expr.getRight()), expr));
    		case "&&":
    			return TSValue.of(execute(expr.getLeft()).asCondition() && execute(expr.getRight()).asCondition());
    		case "||":
    			return TSValue.of(execute(expr.getLeft()).asCondition() || execute(expr.getRight()).asCondition());
    		case "??": {
    			TSValue left = execute(expr.getLeft());
    			return left == TSValue.UNDEFINED || left == TSValue.NULL ? execute(expr.getRight()) : left;
    		}
    	}
    	return applyOperator(op, execute(expr.getLeft()), execute(expr.getRight()), expr);
    }

    private TSValue applyOperator(String op, TSValue left, TSValue right, BinaryExpression expr) {
    	return switch (op) {
    		case "==" -> TSValue.of(left.tinyscriptEquals(right));
    		case "!=" -> TSValue.of(!left.tinyscriptEquals(right));
    		case "<", "<=", ">", ">=" -> compare(op, left, right, expr);
    		case "+" -> add(left, right, expr);
    		case "-", "*", "/", "%" -> arithmetic(op, left, right, expr);
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
    		String result = TSObject.stringify(left) + TSObject.stringify(right);
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
    	throw new TinyscriptTypeError("'+' adds two numbers, two arrays, or a string and a value, not "
    			+ left.typeOf() + " and " + right.typeOf(), expr);
    }

    private TSValue arithmetic(String op, TSValue left, TSValue right, BinaryExpression expr) {
    	if (!(left instanceof TSNumber leftNumber && right instanceof TSNumber rightNumber))
    		throw new TinyscriptTypeError("'" + op + "' needs two numbers, not " + left.typeOf() + " and " + right.typeOf(), expr);
    	double l = leftNumber.value();
    	double r = rightNumber.value();
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
    			if (!(value instanceof TSNumber number))
    				throw new TinyscriptTypeError("'-' needs a number, not " + value.typeOf(), expr);
    			yield TSValue.of(-number.value());
    		}
    		case "!" -> TSValue.of(!value.asCondition());
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
    	try {
    		return evaluateCallOrPropertyAccess(expr);
    	}
    	catch (TSOptionalChainEnd end) {
    		// a?.b.c: an optional access on null or undefined skips the rest of the chain
    		if (expr.eContainer() instanceof CallOrPropertyAccess outer && outer.getExpr() == expr)
    			throw end;
    		return TSValue.UNDEFINED;
    	}
    }

    private TSValue evaluateCallOrPropertyAccess(CallOrPropertyAccess expr) {
    	TSValue base = execute(expr.getExpr());
    	EObject suffix = expr.getSuffix();
		if (suffix instanceof CallOrPropertyAccessSuffix callOrProp) {
			// object.property or object.method(...)
			if (isOptional(callOrProp.getProperty()) && (base == TSValue.UNDEFINED || base == TSValue.NULL))
				throw TSOptionalChainEnd.INSTANCE;
			TSObject object = toObjectForPropertyAccess(base, expr);
			TSValue key = evaluatePropertyKey(callOrProp.getProperty());
			TSValue property = getProperty(object, key);
			if (callOrProp.getCall() == null)
				return property;
			if (!(property instanceof Function method)) {
				String type = typeName(object);
				throw new TinyscriptTypeError(property == TSValue.UNDEFINED
						? type + " has no method '" + key.asString() + "'"
						: "'" + key.asString() + "' of " + type + " is " + article(property) + ", not a function", expr);
			}
			return processFunctionCall(expr, method, object, callOrProp.getCall().getArguments());
		}
    	if (suffix instanceof CallSuffix call) {
    		// function(...)
    		if (!(base instanceof Function function)) {
    			throw new TinyscriptTypeError(expr.getExpr() instanceof Reference reference && !reference.isThis()
    					? "'" + reference.getId().getName() + "' is " + article(base) + ", not a function"
    					: "Only functions can be called, not " + base.typeOf(), expr);
    		}
    		return processFunctionCall(expr, function, null, call.getArguments());
    	}
    	return TSValue.UNDEFINED;
    }

    // a?.b or a?.[b]
    private static boolean isOptional(PropertyAccessSuffix suffix) {
    	return switch (suffix) {
    		case DotPropertyAccessSuffix dot -> dot.isOptional();
    		case ComputedPropertyAccessSuffix computed -> computed.isOptional();
    		default -> false;
    	};
    }

    /**
     * The name of an object's type for error messages: the name of its constructor,
     * e.g. String, Array, Map or a constructor function written in tinyscript.
     */
    private static String typeName(TSObject object) {
    	if (object.get("constructor") instanceof Function constructor && constructor.getName() != null && !constructor.getName().isEmpty())
    		return constructor.getName();
    	return "Object";
    }

    // "a number", "an object", "undefined"
    private static String article(TSValue value) {
    	String type = value.typeOf();
    	if (value == TSValue.UNDEFINED || value == TSValue.NULL)
    		return value.asString();
    	return (type.startsWith("o") ? "an " : "a ") + type;
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
    	// Like Javascript, "abc"[1] is "b"
    	if (object instanceof StringObject string && isArrayIndex(key)) {
    		int index = key.asInt();
    		return index < string.getLength() ? TSValue.of(string.getValue().substring(index, index + 1)) : TSValue.UNDEFINED;
    	}
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

    /**
     * Replaces the value of a variable or property with update(old value) and returns
     * the new value; the variable or property (e.g. a[f()]) is evaluated only once.
     */
    private TSValue updateValue(Expression target, UnaryOperator<TSValue> update) {
    	if (target instanceof Reference reference && !reference.isThis()) {
    		TSValue value = update.apply(caseReference(reference));
    		currentContext.store(reference.getId().getName(), value);
    		return value;
    	}
    	if (target instanceof CallOrPropertyAccess expr && expr.getSuffix() instanceof CallOrPropertyAccessSuffix callOrProp
    			&& callOrProp.getCall() == null) {
    		TSObject object = toObjectForPropertyAccess(execute(expr.getExpr()), expr);
    		TSValue key = evaluatePropertyKey(callOrProp.getProperty());
    		TSValue value = update.apply(getProperty(object, key));
    		putProperty(object, key, value);
    		return value;
    	}
    	throw new TinyscriptTypeError("Invalid left-hand side expression", target);
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
		if (functionObject instanceof BuiltinFunction builtin)
			builtin.checkMaxArgs(args.length, expr);
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
	
	/**
	 * Attaches the tinyscript call stack to an error, in the innermost function call
	 * (the callers that the error passes on its way up keep it). Of a deep stack (a deep
	 * recursion), only the innermost and outermost calls are kept.
	 */
	protected void attachStackTrace(TinyscriptRuntimeException e) {
		if (e.getTinyscriptStacktrace() != null)
			return;
		List<ExecutionContext> calls = contextStack.stream()
				.filter(ctx -> ctx.isFunctionContext() && ctx.currentExpression != null)
				.toList();
		int kept = TinyscriptRuntimeException.SHORTENED_STACKTRACE_CALLS;
		int omitted = calls.size() > 2 * kept ? calls.size() - 2 * kept : 0;
		List<TSStacktraceElement> stacktrace = new ArrayList<TSStacktraceElement>();
		if (e.getNode() != null)
			stacktrace.add(new TSStacktraceElement(TinyscriptModelUtil.getFilenameOfASTNode(e.getNode()), 
					currentContext.name, TinyscriptModelUtil.getLineOfASTNode(e.getNode())));
		else 
			stacktrace.add(new TSStacktraceElement(currentContext.name, 0));
		for (int i = 0; i < calls.size(); i++) {
			if (omitted > 0 && i == kept)
				i += omitted;
			ExecutionContext ctx = calls.get(i);
			stacktrace.add(new TSStacktraceElement(TinyscriptModelUtil.getFilenameOfASTNode(ctx.currentExpression), 
					ctx.name, TinyscriptModelUtil.getLineOfASTNode(ctx.currentExpression)));
		}
		e.setTinyscriptStacktrace(stacktrace.toArray(new TSStacktraceElement[0]), omitted, kept + 1);
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

	/**
	 * Checks the string length limit before a string of {@code length} characters is built.
	 */
	public void checkStringLength(long length) {
		if (resourceMonitor != null)
			resourceMonitor.checkStringLength(length);
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
