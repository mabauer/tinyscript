package de.mkbauer.tinyscript.interpreter;

import java.util.List;

import de.mkbauer.tinyscript.ts.Identifier;

public class LexicalEnvironment {
	
	private List<Identifier> variables;
	
	private List<TSValue> functions;
	
	public LexicalEnvironment(List<TSValue> functions,
			List<Identifier> variables) {
		this.functions = functions;
		this.variables = variables;
	}

	public List<Identifier> getVariables() {
		return variables;
	}
	
	public List<TSValue> getFunctions() {
		return functions;
	}

}
