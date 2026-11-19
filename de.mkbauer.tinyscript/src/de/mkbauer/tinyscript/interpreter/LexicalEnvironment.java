package de.mkbauer.tinyscript.interpreter;

import java.util.List;

import de.mkbauer.tinyscript.ts.FunctionDefinition;
import de.mkbauer.tinyscript.ts.Identifier;

/**
 * The declarations of a block, computed once per block and cached.
 *
 * Only the declarations are cached, not the function objects: a function object
 * captures the execution context it is created in, so it must be created anew
 * each time the block is entered (see {@link TinyscriptEngine#hoistFunctionDeclarations}).
 */
public class LexicalEnvironment {

	private List<Identifier> variables;

	private List<FunctionDefinition> functionDeclarations;

	public LexicalEnvironment(List<FunctionDefinition> functionDeclarations,
			List<Identifier> variables) {
		this.functionDeclarations = functionDeclarations;
		this.variables = variables;
	}

	public List<Identifier> getVariables() {
		return variables;
	}

	public List<FunctionDefinition> getFunctionDeclarations() {
		return functionDeclarations;
	}

}
