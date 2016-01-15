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
	
	public InterpretedFunction(ExecutionVisitor ev) {
		super(ev);
		this.ast = null;
		// Each user defined functions gets a new prototype property since it could be used as a constructor
		setPrototypeProperty(new TSObject(ev.getDefaultPrototype()));
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
		
		if (asConstructor) {
			ev.checkAndIncreaseObjectCreations();
		}

		Block block = getBlock();
		TSValue result = null;
		
		// Create a new execution context
		ExecutionContext currentContext = ev.enterNewExecutionContext(getName(), outerContext);
		
		// Check and update resource consumption
		ev.checkAndIncrementCallDepth();
		
		// Set this-Reference
		currentContext.setThisRef(self);
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
		try {
			result = ev.caseBlock(block);
			if (asConstructor)
				 result = new TSValue(currentContext.getThisRef());
			ev.leaveExecutionContext();
			ev.DecrementCallDepth();
			return result;
		}
		catch (TSReturnValue rv) {
			// Restore execution context
			if (asConstructor && rv.equals(TSValue.UNDEFINED))
				result = new TSValue(currentContext.getThisRef());
			else
				result = rv.getReturnValue();
			ev.leaveExecutionContext();
			ev.DecrementCallDepth();
			return result;				
		}
		catch (TinyscriptRuntimeException e) {
			ev.attachStackTrace(e);
			ev.leaveExecutionContext();
			ev.DecrementCallDepth();
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
