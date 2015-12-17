package de.mkbauer.tinyscript.interpreter;

import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.Function;

public class TSFunction extends TSObject {
	
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
	
	public String getName() {
		if (ast != null) {
			if (ast.getId() != null)
				return ast.getId().getName();
		}
		return null;
	}
	
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
	
	public String toString() {
		String result = "{ [Function";
		if (getName() != null)
			result = result + ": " + getName(); 
		result = result + values.keySet().stream()
				.map(key->key+": "+values.get(key).toString())
				.collect(Collectors.joining(", ")) + "] }";
		return result;
	}
}
