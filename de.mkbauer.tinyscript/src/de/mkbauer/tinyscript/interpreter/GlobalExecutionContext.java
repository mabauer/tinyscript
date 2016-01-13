package de.mkbauer.tinyscript.interpreter;


public class GlobalExecutionContext extends ExecutionContext {

	private TSObject globalObject;
	
	public GlobalExecutionContext() {
		super("global", null);
		
		globalObject = new TSObject();
		
		// TODO: In strict mode, this should be TSValue.undefined
		thisRef = globalObject;

	}
	
	public TSObject getGlobalObject() {
		return globalObject;
	}
	
	@Override
	public boolean isGlobal() {
		return true;
	}

	@Override
	public TSValue get(String identifier) {
		return globalObject.get(identifier);
	}

	@Override
	public void create(String identifier) {
		// TODO: Check if it already exists?
		globalObject.put(identifier, TSValue.UNDEFINED);
	}

	@Override
	public TSValue lookup(String identifier) {
		return globalObject.get(identifier);
	}

	@Override
	public void store(String identifier, TSValue value) {
		globalObject.put(identifier, value);
	}

	@Override
	public boolean contains(String identifier) {
		return !globalObject.get(identifier).equals(TSValue.UNDEFINED);
	}
	
	@Override
	public boolean isFunctionContext() {
		return true;
	}

}
