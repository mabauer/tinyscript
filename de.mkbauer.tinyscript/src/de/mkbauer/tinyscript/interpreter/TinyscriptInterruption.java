package de.mkbauer.tinyscript.interpreter;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;

/**
 * The script was stopped from outside, e.g. with Ctrl-C in the REPL
 * ({@link TinyscriptEngine#interrupt()}). Like a resource limit, it cannot be caught
 * by the script.
 */
public class TinyscriptInterruption extends TinyscriptRuntimeException {

	public TinyscriptInterruption() {
		super("Interrupted");
	}

}
