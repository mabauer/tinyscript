package de.mkbauer.tinyscript.interpreter;


import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.runtime.array.ArrayConstructor;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;
import de.mkbauer.tinyscript.ts.ArrowExpressionBody;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.FunctionDefinition;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.TinyscriptModelUtil;
import de.mkbauer.tinyscript.ts.ClassDeclaration;
import de.mkbauer.tinyscript.ts.ClassMethod;
import de.mkbauer.tinyscript.ts.PropertyAssignment;

public class InterpretedFunction extends Function {
	
	private FunctionDefinition ast;
	
	private ExecutionContext outerContext;
	
	public InterpretedFunction(TinyscriptEngine engine) {
		super(engine);
		this.ast = null;
		// Each user defined functions gets a new prototype property since it could be used as a constructor;
		// like Javascript, its (non-enumerable) constructor property points back to the function
		TSObject prototype = new TSObject(engine, engine.getDefaultPrototype());
		prototype.defineDefaultProperty("constructor", this);
		setPrototypeProperty(prototype);
	}
	
	public void setAst(FunctionDefinition ast) {
		this.ast = ast;
	}
	
	public FunctionDefinition getAst() {
		return ast;
	}
	
	public void setOuterContext(ExecutionContext outer) {
		this.outerContext = outer;
	}
	
	public ExecutionContext getOuterContext() {
		return outerContext;
	}
	
	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		
		ResourceMonitor monitor = engine.getResourceMonitor();
		if (monitor != null)
			monitor.checkMXCpuTimeAndMemory();

		Block block = getBlock();
		if (block == null) 
			return TSValue.UNDEFINED;
			
		// Create a new execution context
		ExecutionContext currentContext = engine.enterNewExecutionContext(getName(), outerContext);
		engine.callDepth++;

		// Leave the context and restore the call depth however the call ends
		try {
			engine.checkInterrupt();
			if (monitor != null)
				monitor.checkCallDepth(engine.callDepth);

			// Set this-Reference
			currentContext.setThisRef(self);

			// With an arrow function, `this` is lexically bound.
			// It means that it uses `this` from the code that contains the arrow function.
			if (isArrowFunction()) {
				currentContext.setThisRef(getOuterContext().getThisRef());
			}
			currentContext.setFunctionContext(true);

			// Put the arguments into the context (a name, or a pattern that takes the argument apart;
			// missing arguments are undefined). A default replaces undefined; it is evaluated in this
			// context, so it sees the parameters before it.
			if (args != null) {
				for (int i = 0; i < ast.getParams().size(); i++) {
					EObject param = ast.getParams().get(i);
					TSValue value = i < args.length ? args[i] : TSValue.UNDEFINED;
					if (value == TSValue.UNDEFINED && param instanceof Identifier name && name.getDefaultValue() != null)
						value = engine.execute(name.getDefaultValue());
					engine.declare(param, value);
				}
			}

			// Special case ...rest parameter
			if (ast.getRest() != null) {
				currentContext.create(ast.getRest().getName());
				ArrayObject restArray = (ArrayObject) engine.getConstructor(ArrayConstructor.NAME).createObject();
				for (int i = ast.getParams().size(); i < args.length; i++) {
					restArray.push(args[i]);
				}
				currentContext.store(ast.getRest().getName(), restArray);
			}

			engine.hoistFunctionDeclarations(engine.getLexicalEnvironment(block));

			TSValue result = engine.caseBlock(block);
			// Only an arrow function with an expression body (x => x * 2) returns its value;
			// otherwise the result is undefined unless there is a return statement
			return block instanceof ArrowExpressionBody ? result : TSValue.UNDEFINED;
		}
		catch (TSReturnValue rv) {
			return rv.getReturnValue();
		}
		catch (TinyscriptRuntimeException e) {
			engine.attachStackTrace(e);
			throw e;
		}
		catch (StackOverflowError e) {
			// Caught near the end of the stack: if creating the error overflows again,
			// a caller with more room left catches that one
			throw new TinyscriptStackOverflowError(engine.callDepth);
		}
		finally {
			engine.leaveExecutionContext();
			engine.callDepth--;
		}
	}
	
	@Override
	public String getName() {
		if (ast != null) {
			if (ast.getId() != null)
				return ast.getId().getName();
			// Like Javascript: { greet() { ... } } and { greet: function() { ... } } are named greet
			if (ast.eContainer() instanceof PropertyAssignment property) {
				String key = TinyscriptModelUtil.propertyKey(property.getKey());
				if (key != null)
					return key;
			}
			// A method of a class: Num.evaluate (the constructor is the class, see ClassObject)
			if (ast.eContainer() instanceof ClassMethod method && method.eContainer() instanceof ClassDeclaration declaration)
				return declaration.getId().getName() + "." + TinyscriptModelUtil.propertyKey(method.getName());
		}
		return "";
	}
	
	@Override
	public boolean isConstructor() {
		return !isArrowFunction() && !isMethod();
	}

	// A method of a class or the shorthand of an object literal ({ area() { … } })
	private boolean isMethod() {
		return ast != null && (ast.eContainer() instanceof ClassMethod && !(this instanceof ClassObject)
				|| TinyscriptModelUtil.isMethodShorthand(ast));
	}

	@Override
	public String kind() {
		return isArrowFunction() ? "an arrow function" : isMethod() ? "a method" : "a function";
	}

	@Override
	public int getLength() {
		if (ast != null) {
			// As in Javascript: the parameters before the first one with a default
			int length = 0;
			for (EObject param : ast.getParams()) {
				if (param instanceof Identifier name && name.getDefaultValue() != null)
					break;
				length++;
			}
			return length;
		}
		return 0;
	}
	
	public Block getBlock() {
		if (ast != null) {
			return ast.getBlock();
		}
		return null;
	}

	

}
