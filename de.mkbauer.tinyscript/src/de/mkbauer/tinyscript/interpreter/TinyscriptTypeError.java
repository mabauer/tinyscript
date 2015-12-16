package de.mkbauer.tinyscript.interpreter;


import org.eclipse.emf.ecore.EObject;

public class TinyscriptTypeError extends TinyscriptRuntimeException {

		
	TinyscriptTypeError() {
		super();
	}
	
	TinyscriptTypeError(String message) {
		super(message);
	}
	
	TinyscriptTypeError(String message, EObject node) {
		super(message, node);
	}

}
