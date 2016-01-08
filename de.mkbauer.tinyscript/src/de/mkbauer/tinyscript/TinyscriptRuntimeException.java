package de.mkbauer.tinyscript;

import org.eclipse.emf.ecore.EObject;

public class TinyscriptRuntimeException extends RuntimeException {
	
	private static final long serialVersionUID = 2517723085390256381L;

	private EObject node;
	
	public TinyscriptRuntimeException() {
		super();
		this.node = null;
	}
	
	public TinyscriptRuntimeException(String message) {
		super(message);
		this.node = null;
	}
	
	public TinyscriptRuntimeException(String message, EObject node) {
		super(message);
		this.node = node;
	}
	
	public TinyscriptRuntimeException(Exception e) {
		super(e);
		this.node = null;
	}

}
