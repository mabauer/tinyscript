package de.mkbauer.tinyscript.interpreter;


import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;


public class TinyscriptArgumentError extends TinyscriptRuntimeException {

		
	public TinyscriptArgumentError() {
		super();
	}
	
	public TinyscriptArgumentError(String message) {
		super(message);
	}
	
	public TinyscriptArgumentError(String message, EObject node) {
		super(message, node);
	}

}


