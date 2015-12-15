package de.mkbauer.tinyscript.interpreter;

import java.util.HashMap;

import de.mkbauer.tinyscript.ts.Identifier;

public class ExecutionContext {
	
	private HashMap<Identifier, TSValue> values;
	
	private ExecutionContext outer;
	
	private ExecutionContext caller;
	
	public ExecutionContext() {
		caller = null;
		outer = null;
		values = new HashMap<Identifier, TSValue>();
	}
	
	public boolean isGlobal() {
		return (caller == null);
	}
	
	public TSValue get(Identifier identifier) {
		return values.get(identifier);
	}
	
	public void create(Identifier identifier) {
		if (values.containsKey(identifier)) {
			throw new IllegalArgumentException("Duplicate Identifier");
		}
		else {
			values.put(identifier, TSValue.UNDEFINED);
		}
	}
	
	public TSValue lookup(Identifier identifier) {
		if (values.containsKey(identifier)) {
			return values.get(identifier);
		}
		else {
			if (isGlobal()) {
				return null;
			}
			else { 
				return outer.lookup(identifier);
			}
		}
	}
	
	public void store(Identifier identifier, TSValue value) {
		if (values.containsKey(identifier)) {
			values.put(identifier, value);
		}
		else {
			if (isGlobal()) {
				throw new IllegalArgumentException("Unknown Identifier");
			}
			else {
				outer.store(identifier, value);
			}
		}
	}
	
	public boolean contains(Identifier identifier) {
		return values.containsKey(identifier); 
	}

}
