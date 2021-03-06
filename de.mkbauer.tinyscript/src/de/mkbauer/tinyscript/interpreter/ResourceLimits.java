package de.mkbauer.tinyscript.interpreter;

public class ResourceLimits {
	
	public static final ResourceLimits UNLIMITED = new ResourceLimits();
	
	protected int maxStatements;
	
	protected int maxCallDepth;
	
	protected int maxObjects;
	
	protected long maxMemory;
	
	protected long maxMXCpuTime;
	
	protected long maxMxMAlloc;
	
	protected int maxObjectCreations;
	
	protected int maxObjectSize;
	
	protected int maxStringLength;
	
	public ResourceLimits() {
		maxStatements = 0;
		maxCallDepth = 0;
		maxObjects = 0;
		maxObjectCreations = 0;
		maxObjectSize = 0;
		maxStringLength = 0;
		maxMXCpuTime = 0;
		maxMxMAlloc = 0;
	}
	
	public int getMaxStatements() {
		return maxStatements;
	}

	public void setMaxStatements(int maxStatements) {
		this.maxStatements = maxStatements;
	}

	public int getMaxRecursionDepth() {
		return maxCallDepth;
	}

	public void setMaxRecursionDepth(int maxCallDepth) {
		this.maxCallDepth = maxCallDepth;
	}

	public int getMaxObjectCreations() {
		return maxObjectCreations;
	}

	public void setMaxObjectCreations(int maxObjectCreations) {
		this.maxObjectCreations = maxObjectCreations;
	}

	public int getMaxObjectSize() {
		return maxObjectSize;
	}

	public void setMaxObjectSize(int maxObjectSize) {
		this.maxObjectSize = maxObjectSize;
	}

	public long getMaxMXCpuTime() {
		return maxMXCpuTime;
	}

	public void setMaxMXCpuTime(long maxMXCpuTime) {
		this.maxMXCpuTime = maxMXCpuTime;
	}

	public long getMaxMxMAlloc() {
		return maxMxMAlloc;
	}

	public void setMaxMxMAlloc(long maxMxMAlloc) {
		this.maxMxMAlloc = maxMxMAlloc;
	}

	public int getMaxStringLength() {
		return maxStringLength;
	}

	public void setMaxStringLength(int maxStringSize) {
		this.maxStringLength = maxStringSize;
	}

	public int getMaxCallDepth() {
		return maxCallDepth;
	}

	public void setMaxCallDepth(int maxCallDepth) {
		this.maxCallDepth = maxCallDepth;
	}

	public int getMaxObjects() {
		return maxObjects;
	}

	public void setMaxObjects(int maxObjects) {
		this.maxObjects = maxObjects;
		
	}

	public long getMaxMemory() {
		return maxMemory;
	}

	public void setMaxMemory(long maxMemory) {
		this.maxMemory = maxMemory;
	}
	

}
