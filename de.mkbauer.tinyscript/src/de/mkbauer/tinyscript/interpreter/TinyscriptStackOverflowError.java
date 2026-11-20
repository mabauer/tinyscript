package de.mkbauer.tinyscript.interpreter;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;

/**
 * The Java stack is exhausted, usually by an endless or too deep recursion.
 */
public class TinyscriptStackOverflowError extends TinyscriptRuntimeException {

	public TinyscriptStackOverflowError(int callDepth) {
		super("Stack overflow: too many nested calls (call depth " + callDepth + ")");
	}

}
