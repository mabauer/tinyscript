package de.mkbauer.tinyscript.interpreter;

/**
 * Thrown by a <code>break</code> statement and caught by the innermost enclosing loop.
 * The validator ensures that <code>break</code> only occurs inside a loop of the same function.
 */
public class TSBreak extends RuntimeException {

	static final TSBreak INSTANCE = new TSBreak();

	private TSBreak() {
		super(null, null, false, false);
	}

}
