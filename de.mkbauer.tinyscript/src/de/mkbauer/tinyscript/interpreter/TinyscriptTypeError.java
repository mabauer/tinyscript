package de.mkbauer.tinyscript.interpreter;


import org.eclipse.emf.ecore.EObject;

public class TinyscriptTypeError extends TinyscriptRuntimeException {

		
	public TinyscriptTypeError() {
		super();
	}
	
	public TinyscriptTypeError(String message) {
		super(message);
	}
	
	public TinyscriptTypeError(String message, EObject node) {
		super(message, node);
	}

}
