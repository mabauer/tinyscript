package de.mkbauer.tinyscript.interpreter;

public class ResourceConsumption {
	
	protected long executionTime;
	
	protected int statements;
	
	protected int callDepth;
	
	protected long memory;
	
	protected long memoryMax;
	
	protected long mxCpuTime;
	
	protected long mxMAlloc;
	
	protected int objects;
	
	protected int objectsMax;
	
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
	
	public long getMemoryMax() {
		return memoryMax;
	}

	public void setMemoryMax(long memoryMax) {
		this.memoryMax = memoryMax;
	}

	public int getObjectsMax() {
		return objectsMax;
	}

	public void setObjectsMax(int objectsMax) {
		this.objectsMax = objectsMax;
	}

	public long getMxCpuTime() {
		return mxCpuTime;
	}

	public void setMxCpuTime(long mxCpuTime) {
		this.mxCpuTime = mxCpuTime;
	}

	public long getMxMAlloc() {
		return mxMAlloc;
	}

	public void setMxMAlloc(long mxMAlloc) {
		this.mxMAlloc = mxMAlloc;
	}

	public void add(ResourceConsumption other) {
		executionTime += other.executionTime;
		statements += other.statements;
		callDepth = Math.max(callDepth, other.callDepth);
		memory = other.memory;
		memoryMax = Math.max(memoryMax, other.memoryMax);
		objects = other.objects;
		objectsMax = Math.max(objectsMax, other.objectsMax);
		objectCreations += other.objectCreations;
		mxCpuTime += other.mxCpuTime;
		mxMAlloc += other.mxMAlloc;
	}
	
	public String toString() {
		String result = "time=" + executionTime + "ms"
				+ ((mxCpuTime > 0) ? ", cpu=" + mxCpuTime / 1000000 + "ms" : "")
				+ ", stmts=" + statements 
				+ ", calldepth=" + callDepth
				+ ((objectsMax > 0) ? ", objs=" + objectsMax : "")
				+ ((memoryMax > 0) ? ", umem=" + memoryMax / 1024 + "K": "")
				+ ", creates=" + objectCreations
				+ ((mxMAlloc > 0) ? ", malloc=" + mxMAlloc / 1024 + "K" : "");
		return result;
	}
	
}
