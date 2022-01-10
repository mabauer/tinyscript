package de.mkbauer.tinyscript.interpreter;

import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.FunctionDefinition;
import de.mkbauer.tinyscript.ts.Identifier;

public class InterpretedFunction extends Function {
	
	private FunctionDefinition ast;
	
	private ExecutionContext outerContext;
	
	public InterpretedFunction(TinyscriptEngine engine) {
		super(engine);
		this.ast = null;
		// Each user defined functions gets a new prototype property since it could be used as a constructor
		setPrototypeProperty(new TSObject(engine, engine.getDefaultPrototype()));
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
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
		
		ResourceMonitor monitor = engine.getResourceMonitor();
		if (monitor != null)
			monitor.checkMXCpuTimeAndMemory();

		Block block = getBlock();
		if (block == null) 
			return TSValue.UNDEFINED;
			
		TSValue result = null;
		
		// Create a new execution context
		ExecutionContext currentContext = engine.enterNewExecutionContext(getName(), outerContext);
		
		// Update and check call depth
		engine.callDepth++;
		
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
		
		// Put the arguments into the context
		if (args != null) {
			int argsN = args.size();
			int i = 0;
			for (Identifier param : ast.getParams()) {
				currentContext.create(param.getName());
				if (i < argsN) {
					currentContext.store(param.getName(), args.get(i));
				}
				i++;
			}
		}
		
		// Hoist function declarations
		LexicalEnvironment env = engine.getLexcialEnvironment(block);
		for (TSValue function : env.getFunctions()) {	
			// Create a variable in the current context pointing to each function object
			String functionName = ((Function) function.asObject()).getName();
			if (!currentContext.contains(functionName))
				currentContext.create(functionName);
			currentContext.store(functionName, function);
		}
		
		try {
			result = engine.caseBlock(block);
			if (asConstructor)
				 result = new TSValue(currentContext.getThisRef());
			engine.leaveExecutionContext();
			engine.callDepth--;
			return result;
		}
		catch (TSReturnValue rv) {
			// Restore execution context
			if (asConstructor && rv.equals(TSValue.UNDEFINED))
				result = new TSValue(currentContext.getThisRef());
			else
				result = rv.getReturnValue();
			engine.leaveExecutionContext();
			engine.callDepth--;
			return result;				
		}
		catch (TinyscriptRuntimeException e) {
			engine.attachStackTrace(e);
			engine.leaveExecutionContext();
			engine.callDepth--;
			throw e;
		}
	}
	
	@Override
	public String getName() {
		if (ast != null) {
			if (ast.getId() != null)
				return ast.getId().getName();
		}
		return "";
	}
	
	@Override
	public int getLength() {
		if (ast != null) {
			if (ast.getParams() != null) 
				return ast.getParams().size();
		}
		return 0;
	}
	
	public Block getBlock() {
		if (ast != null) {
			return ast.getBlock();
		}
		return null;
	}

	public String getCodeAsString() {
		String result = "function " + getName() + "(";
		if (ast != null) 
			result = result + ast.getParams().stream()
				.map(id->id.getName())
				.collect(Collectors.joining(", "));
		result = result + ") {...}"; 
		return result;
	}
	

}
