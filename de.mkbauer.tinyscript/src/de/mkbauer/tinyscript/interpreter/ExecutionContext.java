package de.mkbauer.tinyscript.interpreter;

import java.util.HashMap;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.ts.Identifier;

public class ExecutionContext {
	
	private String name;
	
	private HashMap<String, TSValue> values;
	
	private ExecutionContext outer;
	
	public ExecutionContext(String name) {
		this.name = name;
		outer = null;
		values = new HashMap<String, TSValue>();
	}
	
	public ExecutionContext(String name, ExecutionContext outerContext) {
		this.name = name;
		outer = outerContext;
		values = new HashMap<String, TSValue>();
	}

	public boolean isGlobal() {
		return (outer == null);
	}
	
	public TSValue get(String identifier) {
		return values.get(identifier);
	}
	
	public void create(String identifier) {
		if (values.containsKey(identifier)) {
			throw new TinyscriptReferenceError("Duplicate Identifier: " + identifier);
		}
		else {
			values.put(identifier, TSValue.UNDEFINED);
		}
	}
	
	public TSValue lookup(String identifier) {
		TSValue result = values.get(identifier);
		if (result != null)
			return result;
		if (isGlobal()) {
			throw new TinyscriptReferenceError("Unknown Identifier: " + identifier);
		}
		else { 
			return outer.lookup(identifier);
		}
	}
	
	public void store(String identifier, TSValue value) {
		if (values.containsKey(identifier)) {
			values.put(identifier, value);
		}
		else {
			if (isGlobal()) {
				throw new TinyscriptReferenceError("Unknown Identifier:" + identifier);
			}
			else {
				outer.store(identifier, value);
			}
		}
	}
	
	public boolean contains(String identifier) {
		return values.containsKey(identifier); 
	}
	
	public String toString() {
		return values.keySet().stream()
				.map(key->key+": "+values.get(key).toString())
				.collect(Collectors.joining(", ", name + "={ ", " }"));
	}

}
