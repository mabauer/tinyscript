package de.mkbauer.tinyscript.interpreter;

import org.eclipse.emf.ecore.EObject;

public class TinyscriptRuntimeException extends RuntimeException {
	
	private static final long serialVersionUID = 2517723085390256381L;

	private EObject node;
	
	TinyscriptRuntimeException() {
		super();
		this.node = null;
	}
	
	TinyscriptRuntimeException(String message) {
		super(message);
		this.node = null;
	}
	
	TinyscriptRuntimeException(String message, EObject node) {
		super(message);
		this.node = node;
	}

}
