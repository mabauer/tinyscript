package de.mkbauer.tinyscript.interpreter;



public class TSReturnValue extends RuntimeException {
	
	private TSValue returnValue = null;
	
	TSReturnValue(TSValue value) {
		super();
		this.returnValue = value;
	}
	
	@Override
	public synchronized Throwable fillInStackTrace() {
		return null;
	}
	
	public TSValue getReturnValue() {
		return returnValue;
	}

}

