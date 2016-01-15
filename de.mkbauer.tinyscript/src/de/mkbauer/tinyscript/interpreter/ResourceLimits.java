package de.mkbauer.tinyscript.interpreter;

public class ResourceLimits {
	
	public static final ResourceLimits UNLIMITED = new ResourceLimits();
	
	protected int maxStatements;
	
	protected int maxCallDepth;
	
	protected int maxObjectCreations;
	
	public ResourceLimits() {
		maxStatements = 0;
		maxCallDepth = 0;
		maxObjectCreations = 0;
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

}
