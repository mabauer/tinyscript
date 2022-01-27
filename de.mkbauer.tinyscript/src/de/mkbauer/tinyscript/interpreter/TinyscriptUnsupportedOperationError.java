package de.mkbauer.tinyscript.interpreter;


import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;


public class TinyscriptUnsupportedOperationError extends TinyscriptRuntimeException {

		
	public TinyscriptUnsupportedOperationError() {
		super();
	}
	
	public TinyscriptUnsupportedOperationError(String message) {
		super(message);
	}
	
	public TinyscriptUnsupportedOperationError(String message, EObject node) {
		super(message, node);
	}

}


