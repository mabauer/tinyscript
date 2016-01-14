package de.mkbauer.tinyscript.interpreter;

import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;

public class TinyscriptResourceLimitViolation extends
		TinyscriptRuntimeException {

	public TinyscriptResourceLimitViolation() {
		super();
	}
	
	public TinyscriptResourceLimitViolation(String message) {
		super(message);
	}
	
	public TinyscriptResourceLimitViolation(String message, EObject node) {
		super(message, node);
	}
	
}
