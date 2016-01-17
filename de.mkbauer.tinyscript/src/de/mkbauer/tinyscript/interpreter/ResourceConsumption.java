package de.mkbauer.tinyscript.interpreter;

public class ResourceConsumption {
	
	protected long executionTime;
	
	protected int statements;
	
	protected int callDepth;
	
	protected long memory;
	
	protected int objects;
	
	protected int objectCreations;
	
	public ResourceConsumption() {
		executionTime = 0;
		statements = 0;
		callDepth = 0;
		memory = 0;
		objects = 0;
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

	public long getMemory() {
		return memory;
	}

	public void setMemory(long memory) {
		this.memory = memory;
	}

	public int getObjects() {
		return objects;
	}

	public void setObjects(int objects) {
		this.objects = objects;
	}
	
	public void add(ResourceConsumption other) {
		executionTime += other.executionTime;
		statements += other.statements;
		callDepth = Math.max(callDepth, other.callDepth);
		memory = Math.max(memory, other.memory);
		objects = Math.max(objects, other.objects);
		objectCreations += other.objectCreations;
	}
	
}
