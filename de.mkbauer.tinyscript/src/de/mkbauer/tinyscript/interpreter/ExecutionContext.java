package de.mkbauer.tinyscript.interpreter;

import java.util.HashMap;

import de.mkbauer.tinyscript.ts.Identifier;

public class ExecutionContext {
	
	private String name;
	
	private HashMap<Identifier, TSValue> values;
	
	private ExecutionContext outer;
	
	public ExecutionContext(String name) {
		this.name = name;
		outer = null;
		values = new HashMap<Identifier, TSValue>();
	}
	
	public ExecutionContext(String name, ExecutionContext outerContext) {
		this.name = name;
		outer = outerContext;
		values = new HashMap<Identifier, TSValue>();
	}

	public boolean isGlobal() {
		return (outer == null);
	}
	
	public TSValue get(Identifier identifier) {
		return values.get(identifier);
	}
	
	public void create(Identifier identifier) {
		if (values.containsKey(identifier)) {
			throw new IllegalArgumentException("Duplicate Identifier: "+ identifier.getName());
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
				throw new IllegalArgumentException("Unknown Identifier: " + identifier.getName());
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
				throw new IllegalArgumentException("Unknown Identifier: "+ identifier.getName());
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
