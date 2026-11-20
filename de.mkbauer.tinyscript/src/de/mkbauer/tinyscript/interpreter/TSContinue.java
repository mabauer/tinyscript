package de.mkbauer.tinyscript.interpreter;

/**
 * Thrown by a <code>continue</code> statement and caught by the innermost enclosing loop,
 * which goes on with its next iteration. The validator ensures that <code>continue</code>
 * only occurs inside a loop of the same function.
 */
public class TSContinue extends RuntimeException {

	static final TSContinue INSTANCE = new TSContinue();

	private TSContinue() {
		super(null, null, false, false);
	}

}
