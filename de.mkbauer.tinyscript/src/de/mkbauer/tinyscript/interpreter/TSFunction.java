package de.mkbauer.tinyscript.interpreter;

import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.FunctionDefinition;

public class TSFunction extends TSAbstractFunction {
	
	private FunctionDefinition ast;
	
	private ExecutionContext outerContext;
	
	public TSFunction() {
		this.ast = null;
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
