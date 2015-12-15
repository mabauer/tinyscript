package de.mkbauer.tinyscript.interpreter;

import org.eclipse.emf.ecore.EObject;

public class TinyscriptAssertationError extends TinyscriptRuntimeException {

		
	TinyscriptAssertationError() {
		super();
	}
	
	TinyscriptAssertationError(String message) {
		super(message);
	}
	
	TinyscriptAssertationError(String message, EObject node) {
		super(message, node);
	}

}
