package de.mkbauer.tinyscript.interpreter;


public class GlobalExecutionContext extends ExecutionContext {

	private TSObject globalObject;
	
	public GlobalExecutionContext(TinyscriptEngine engine) {
		super("global", null);
		
		globalObject = new TSObject(engine, engine.getDefaultPrototype());
		
		// Like a Javascript module (strict mode), `this` is undefined outside a function (null
		// here); so the global object with the builtins is not reachable from a script
		thisRef = null;

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
		// Overwriting an existing global is intended: within a script the validator rejects
		// duplicate declarations, and in the REPL a later "let x" declares a fresh x
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
