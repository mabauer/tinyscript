package de.mkbauer.tinyscript.interpreter;

import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.Function;

public class TSFunction extends TSAbstractFunction {
	
	private Function ast;
	
	private ExecutionContext outerContext;
	
	public TSFunction() {
		this.ast = null;
	}
	
	public void setAst(Function ast) {
		this.ast = ast;
	}
	
	public Function getAst() {
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
