package de.mkbauer.tinyscript.interpreter;

import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.FunctionDefinition;

public class InterpretedFunction extends Function {
	
	private FunctionDefinition ast;
	
	private ExecutionContext outerContext;
	
	public InterpretedFunction(GlobalExecutionContext globalContext) {
		super(globalContext);
		this.ast = null;
		// Each user defined functions gets a new prototype property since it could be used as a constructor
		setPrototypeProperty(new TSObject(globalContext.getDefaultPrototype()));
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
	public String getName() {
		if (ast != null) {
			if (ast.getId() != null)
				return ast.getId().getName();
		}
		return null;
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
	

}
