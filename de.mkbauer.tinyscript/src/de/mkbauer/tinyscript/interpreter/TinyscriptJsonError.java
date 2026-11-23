package de.mkbauer.tinyscript.interpreter;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;

/**
 * JSON.parse got a text that is no JSON. Like failed input / output (TinyscriptIOError), catch (e)
 * gets it, as an Error named SyntaxError (as in Javascript): data from outside may be broken.
 */
public class TinyscriptJsonError extends TinyscriptRuntimeException {

	private static final long serialVersionUID = 1L;

	public TinyscriptJsonError(String message) {
		super(message);
	}
}
