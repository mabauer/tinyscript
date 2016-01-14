package de.mkbauer.tinyscript.interpreter;

public class ResourceConsumption {
	
	protected long executionTime;
	
	protected int statements;
	
	protected int callDepth;
	
	protected int objectCreations;
	
	public ResourceConsumption() {
		statements = 0;
		callDepth = 0;
		objectCreations = 0;
	}
	
	public long getExecutionTime() {
		return executionTime;
	}

	public int getStatements() {
		return statements;
	}

	public int getCallDepth() {
		return callDepth;
	}

	public int getObjectCreations() {
		return objectCreations;
	}

}
