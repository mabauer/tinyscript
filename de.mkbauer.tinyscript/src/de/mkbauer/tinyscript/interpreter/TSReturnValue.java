package de.mkbauer.tinyscript.interpreter;



public class TSReturnValue extends RuntimeException {
	
	private TSValue returnValue = null;
	
	TSReturnValue(TSValue value) {
		super();
		this.returnValue = value;
	}
	
	public TSValue getReturnValue() {
		return returnValue;
	}

}

