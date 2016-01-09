package de.mkbauer.tinyscript.interpreter;

import java.util.HashMap;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.ts.Identifier;

public class ExecutionContext {
	
	protected String name;
	
	private HashMap<String, TSValue> values;
	
	private ExecutionContext outer;
	
	protected TSObject thisRef;
	
	// Used to save the last expression before a function call in order to compute stack traces
	protected EObject currentExpression;
	
	private boolean functionContext;

	public ExecutionContext(String name, ExecutionContext outerContext) {
		this.name = name;
		outer = outerContext;
		values = new HashMap<String, TSValue>();
		currentExpression = null;
	}

	public boolean isGlobal() {
		return false;
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
	
	public TSObject getThisRef() {
		return thisRef;
	}

	public void setThisRef(TSObject thisRef) {
		this.thisRef = thisRef;
	}
	
	
	public void setFunctionContext(boolean functionContext) {
		this.functionContext = functionContext;
	}
	
	public boolean isFunctionContext() {
		return this.functionContext;
	}
	
	public String toString() {
		return values.keySet().stream()
				.map(key->key+": "+values.get(key).toString())
				.collect(Collectors.joining(", ", name + "={ ", " }"));
	}

}
