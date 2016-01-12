package de.mkbauer.tinyscript.interpreter;

import java.util.HashMap;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.runtime.string.StringObject;


public class TSObject {
	
	protected HashMap<String, TSPropertyDescriptor> properties;
	
	private TSObject proto = null;
	
	public TSObject() {
		properties = new HashMap<String, TSPropertyDescriptor>();
	}
	
	public TSObject(TSObject proto) {
		properties = new HashMap<String, TSPropertyDescriptor>();
		if (proto != null)
			setPrototype(proto); 
	}
	
	public static TSObject toObject(ExecutionVisitor ev, TSValue value) throws TinyscriptTypeError {
		if (value == TSValue.UNDEFINED)  {
			throw new TinyscriptTypeError("Cannot convert 'undefined' to an object");
		}
		if (value == TSValue.NULL) {
			throw new TinyscriptTypeError("Cannot convert 'null' to an object");
		}
		if (value.isPrimitiveString()) {
			return new StringObject(ev, value);
		}
		if (value.isNumber()) {
			// TODO Create a NumberObject object
			TSObject result = new TSObject(ev.getDefaultPrototype()); 
			result.put("value", value);
			return result; 
		}
		if (value.isBoolean()) {
			// TODO Create a BooleanObject object
			TSObject result = new TSObject(ev.getDefaultPrototype()); 
			result.put("value", value);
			return result;
		}
		return value.asObject();
	}
	
	public static void defineDefaultProperty(TSObject object, String key, Object value) {
		TSPropertyDescriptor desc = new TSPropertyDescriptor();
		if (!(value instanceof TSValue))
			desc.setValue(new TSValue(value));
		else
			desc.setValue( (TSValue) value);
		// This differs form ECMAScript specs.
		desc.setConfigurable(false);
		desc.setEnumerable(false);
		desc.setWriteable(false);
		object.setOwnPropertyDescriptor(key, desc);
	}
	
	public TSPropertyDescriptor getOwnPropertyDescriptor(String key) {
		return properties.get(key);
	}
	
	public void setOwnPropertyDescriptor(String key, TSPropertyDescriptor desc) {
		properties.put(key,  desc);
	}
	
	public boolean hasOwnProperty(String key) {
		return getOwnPropertyDescriptor(key) != null;
	}
	
	public void setPrototype(TSObject proto) {
		this.proto = proto;
		defineDefaultProperty(this, "__proto__", new TSValue(this.proto));
	}
	
	public TSObject getPrototype() {
		return proto;
	}
		
	public TSValue get(String key) {
		TSValue result = null;
		TSPropertyDescriptor desc = properties.get(key);
		if (desc != null)
			return desc.getValue();
		if (proto != null) {
			result = proto.get(key);
			if (result != null)
				return result;
		}
		return TSValue.UNDEFINED;
	}
	
	
	public void put(String key, TSValue value) {
		TSPropertyDescriptor desc = properties.get(key);
		if (desc != null)
			desc.setValue(value);
		else {
			desc = new TSPropertyDescriptor(value);
			properties.put(key, desc);
		}
	}
		
	public boolean contains(String key) {
		return properties.containsKey(key); 
	}
	
	public boolean hasEnumerableProperties() {
		return (properties.keySet().stream()
				.filter(key -> properties.get(key).isEnumerable())
				.count() > 0);
	}
	
	public String toString() {
		return properties.keySet().stream()
				.filter(key -> properties.get(key).isEnumerable())
				.map(key -> key + ": " + properties.get(key).getValue().toString())
				.collect(Collectors.joining(", ", "{", "}"));
	}

}
