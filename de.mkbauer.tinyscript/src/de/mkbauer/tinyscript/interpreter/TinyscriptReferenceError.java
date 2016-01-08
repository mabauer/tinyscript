package de.mkbauer.tinyscript.interpreter;


import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;

public class TinyscriptReferenceError extends TinyscriptRuntimeException {

		
	public TinyscriptReferenceError() {
		super();
	}
	
	public TinyscriptReferenceError(String message) {
		super(message);
	}
	
	public TinyscriptReferenceError(String message, EObject node) {
		super(message, node);
	}

}
