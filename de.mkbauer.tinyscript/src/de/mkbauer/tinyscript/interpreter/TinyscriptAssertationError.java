package de.mkbauer.tinyscript.interpreter;

import org.eclipse.emf.ecore.EObject;

public class TinyscriptAssertationError extends TinyscriptRuntimeException {

		
	public TinyscriptAssertationError() {
		super();
	}
	
	public TinyscriptAssertationError(String message) {
		super(message);
	}
	
	public TinyscriptAssertationError(String message, EObject node) {
		super(message, node);
	}

}
