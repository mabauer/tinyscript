package de.mkbauer.tinyscript.interpreter;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.runtime.string.StringConstructor;


public class TSObject {
	
	protected HashMap<String, TSPropertyDescriptor> properties;
	
	private TSObject proto = null;
	protected TinyscriptEngine engine;
	
	protected TSObject(TinyscriptEngine engine) {
		this.engine = engine;
		properties = new HashMap<String, TSPropertyDescriptor>();
		engine.recordObjectCreation(this);
	}
	
	public TSObject(TinyscriptEngine engine, TSObject proto) {
		this.engine = engine;
		properties = new HashMap<String, TSPropertyDescriptor>();
		engine.recordObjectCreation(this);
		if (proto != null)
			setPrototype(proto);
	}
	
	// TODO Write tests and use it instead of TSValue.asString() where appropriate
	public static String toString(TinyscriptEngine engine, TSValue value) {
		if (value == TSValue.UNDEFINED)
			return "[object Undefined]";
		if (value == TSValue.NULL)
			return "[object Null]";
		if (value.isObject()) {
			TSObject object = value.asObject();
			TSValue toStringValue = object.get("toString");
			// if (toStringValue.isString()) 
			// 		return toStringValue.asString();
			if (toStringValue.isObject()) {
				TSObject toString = toStringValue.asObject();
				if (toString instanceof Function) {
					TSValue result = ((Function) toString).call(object, new TSValue[0]);
					if (result.isString())
						return result.asString();
				}
			}
		}
		return value.asString();
	}
	
	public static TSObject toObject(TinyscriptEngine engine, TSValue value) throws TinyscriptTypeError {
		if (value == TSValue.UNDEFINED)  {
			throw new TinyscriptTypeError("Cannot convert 'undefined' to an object");
		}
		if (value == TSValue.NULL) {
			throw new TinyscriptTypeError("Cannot convert 'null' to an object");
		}
		if (value.isPrimitiveString()) {
			StringConstructor ctor = (StringConstructor) engine.getConstructor(StringConstructor.NAME);
			return ctor.createObject(value);
		}
		if (value.isNumber()) {
			// TODO Create a NumberObject object
			TSObject result = new TSObject(engine, engine.getDefaultPrototype()); 
			result.put("value", value);
			return result; 
		}
		if (value.isBoolean()) {
			// TODO Create a BooleanObject object
			TSObject result = new TSObject(engine, engine.getDefaultPrototype()); 
			result.put("value", value);
			return result;
		}
		return value.asObject();
	}
	
	public static TSValue toPrimitive(TinyscriptEngine engine, TSObject object) {
		// TODO Evaluate user defined valueOf property
		if (object instanceof BuiltinType) {
			return ((BuiltinType) object).valueOf();
		}
		return TSValue.UNDEFINED;		
	}
	
	public static double toNumber(TinyscriptEngine engine, TSValue value) {
		// TODO Return NAN if undefined
		if (value.isObject()) {
			value = toPrimitive(engine, value.asObject());
		}
		if (value.isBoolean())
			return (value.asBoolean() ? 1 :0);
		if (value.isNumber())
			return (value.asDouble());
		if (value.isString()) {
			String str = value.asString();
			try {
				double d = Double.parseDouble(str);
				return d;
			}
			catch (NumberFormatException e) {}
			return Double.NaN;
		}
		return Double.NaN;
	}
	
	public static int toInteger(TinyscriptEngine engine, TSValue value) {
		// TODO Return NAN if undefined
		if (value.isObject()) {
			value = toPrimitive(engine, value.asObject());
		}
		if (value.isBoolean())
			return (value.asBoolean() ? 1 :0);
		if (value.isNumber())
			return (value.asInt());
		if (value.isString()) {
			String str = value.asString();
			try {
				double d = Double.parseDouble(str);
				return ((Number)d).intValue();
			}
			catch (NumberFormatException e) {}
			try {
				return Integer.decode(str);
			}
			catch (NumberFormatException e) {}
			// TODO: Should be NAN!
			return 0;
		}
		return 0;
	}
	
	public void defineDefaultProperty(String key, Object value) {
		TSPropertyDescriptor desc = new TSPropertyDescriptor();
		if (!(value instanceof TSValue))
			desc.setValue(new TSValue(value));
		else
			desc.setValue( (TSValue) value);
		// This differs form ECMAScript specs.
		desc.setConfigurable(false);
		desc.setEnumerable(false);
		desc.setWriteable(false);
		setOwnPropertyDescriptor(key, desc);
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
	
	public void defineBuiltinMethod(String name, BuiltinFunctionImplementation implementation, int length) {
		defineDefaultProperty(name, engine.defineBuiltinFunction(name, implementation, length));
	}
	
	
	public void setPrototype(TSObject proto) {
		this.proto = proto;
		defineDefaultProperty("__proto__", new TSValue(this.proto));
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
			update();
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
	
	public List<String> getKeys() {
		return (properties.keySet().stream()
				.filter(key -> properties.get(key).isEnumerable())
				.collect(Collectors.toList()));
	}
	
	public String toString() {
		return properties.keySet().stream()
				.filter(key -> properties.get(key).isEnumerable())
				.map(key -> key + ": " + properties.get(key).getValue().toString())
				.collect(Collectors.joining(", ", "{", "}"));
	}
	
	protected void update() {
		if (engine != null) {
			engine.recordObjectSizeChange(this);
		}
	}
	
	public int getObjectSize() {
		return properties.size();
	}
	
	public int estimateMemory() {
		return getObjectSize()*ResourceMonitor.TSVALUE_SIZE;
	}

}
