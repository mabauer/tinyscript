package de.mkbauer.tinyscript.interpreter;

import java.util.HashMap;
import java.util.stream.Collectors;


public class TSObject {
	
	protected HashMap<String, TSPropertyDescriptor> properties;
	
	private TSObject proto = null;
	
	private static TSObject defaultPrototype = null;
	
	public static TSObject getDefaultProtoType() {
		if (defaultPrototype != null)
			return defaultPrototype;
		defaultPrototype = new TSObject(null);
		defineDefaultProperty(defaultPrototype, "toString", new TSValue(new ToString()));
		// defaultPrototype.put("toString", new TSValue(new ToString()));
		return defaultPrototype;
	}
	
	public TSObject() {
		properties = new HashMap<String, TSPropertyDescriptor>();
		initialize();
	}
	
	public TSObject(TSObject proto) {
		properties = new HashMap<String, TSPropertyDescriptor>();
		this.proto = null;
	}
	
	protected void initialize() {
		setPrototype(getDefaultProtoType());
	}
	
	public static void defineDefaultProperty(TSObject object, String key, TSValue value) {
		TSPropertyDescriptor desc = new TSPropertyDescriptor();
		desc.setValue(value);
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
