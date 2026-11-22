package de.mkbauer.tinyscript.interpreter;

import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;

/**
 * An error thrown by a script with throw: its message is the message of the thrown Error
 * object. try / catch catches it (and failed input / output, TinyscriptIOError), nothing else.
 */
public class TinyscriptError extends TinyscriptRuntimeException {

	private final TSValue thrownValue;

	public TinyscriptError(String message, TSValue thrownValue, EObject node) {
		super(message, node);
		this.thrownValue = thrownValue;
	}

	public TSValue getThrownValue() {
		return thrownValue;
	}

}
