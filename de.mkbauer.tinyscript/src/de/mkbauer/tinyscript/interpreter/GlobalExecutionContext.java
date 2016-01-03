package de.mkbauer.tinyscript.interpreter;


public class GlobalExecutionContext extends ExecutionContext {

	public GlobalExecutionContext() {
		super("global", null);
		thisRef = new TSObject();
	}
	
	@Override
	public boolean isGlobal() {
		return true;
	}

	@Override
	public TSValue get(String identifier) {
		return thisRef.get(identifier);
	}

	@Override
	public void create(String identifier) {
		// TODO: Check if it already exists?
		thisRef.put(identifier, TSValue.UNDEFINED);
	}

	@Override
	public TSValue lookup(String identifier) {
		return thisRef.get(identifier);
	}

	@Override
	public void store(String identifier, TSValue value) {
		thisRef.put(identifier, value);
	}

	@Override
	public boolean contains(String identifier) {
		// TODO Auto-generated method stub
		return !thisRef.get(identifier).equals(TSValue.UNDEFINED);
	}

}
