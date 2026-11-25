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

public class InterpretedFunction extends Function {
	
	private FunctionDefinition ast;
	
	private ExecutionContext outerContext;
	
	// The name, worked out once from the AST
	private String name;

	public InterpretedFunction(TinyscriptEngine engine) {
		super(engine);
		this.ast = null;
	}

	/**
	 * Gives a function that can be a constructor (a class, a function written with function) its
	 * prototype property; like Javascript, its (non-enumerable) constructor property points back
	 * to the function. Arrow functions and methods have none.
	 */
	public void createPrototypeProperty() {
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
		ExecutionContext currentContext = engine.enterNewExecutionContext(getStackName(), outerContext);
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
	
	/**
	 * The name, as Javascript gives it (f.name, print): the name of the function, or else where
	 * it is written: const f = () => …, f = function () { … }, { greet() { … } },
	 * { greet: x => x }, a default parameter step = () => 1 and the method area of a class.
	 */
	@Override
	public String getName() {
		if (name == null)
			name = ast != null ? TinyscriptModelUtil.functionName(ast) : "";
		return name;
	}

	/** The name in a stack trace: a method of a class with the class, as node: Num.evaluate. */
	public String getStackName() {
		if (ast != null && ast.getId() == null && ast.eContainer() instanceof ClassMethod method
				&& method.eContainer() instanceof ClassDeclaration declaration && !(this instanceof ClassObject))
			return declaration.getId().getName() + "." + getName();
		return getName();
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
