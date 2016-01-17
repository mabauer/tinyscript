package de.mkbauer.tinyscript.interpreter;

import java.util.HashMap;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.runtime.string.StringObject;


public class TSObject {
	
	protected HashMap<String, TSPropertyDescriptor> properties;
	
	private TSObject proto = null;
	protected ExecutionVisitor ev;
	
	protected TSObject() {
		properties = new HashMap<String, TSPropertyDescriptor>();
	}
	
	protected TSObject(ExecutionVisitor ev) {
		this.ev = ev;
		properties = new HashMap<String, TSPropertyDescriptor>();
		ev.monitorObjectCreation(this);
	}
	
	public TSObject(ExecutionVisitor ev, TSObject proto) {
		this.ev = ev;
		properties = new HashMap<String, TSPropertyDescriptor>();
		ev.monitorObjectCreation(this);
		if (proto != null)
			setPrototype(proto);
	}
	
	// TODO Write tests and use it instead of TSValue.asString() where appropriate
	public static String toString(ExecutionVisitor ev, TSValue value) {
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
					TSValue result = ((Function) toString).call(false, object, new TSValue[0]);
					if (result.isString())
						return result.asString();
				}
			}
		}
		return value.asString();
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
			TSObject result = new TSObject(ev, ev.getDefaultPrototype()); 
			result.put("value", value);
			return result; 
		}
		if (value.isBoolean()) {
			// TODO Create a BooleanObject object
			TSObject result = new TSObject(ev, ev.getDefaultPrototype()); 
			result.put("value", value);
			return result;
		}
		return value.asObject();
	}
	
	public static TSValue toPrimitive(ExecutionVisitor ev, TSObject object) {
		// TODO Evaluate user defined valueOf property
		if (object instanceof BuiltinType) {
			return ((BuiltinType) object).valueOf();
		}
		return TSValue.UNDEFINED;		
	}
	
	public static int toInteger(ExecutionVisitor ev, TSValue value) {
		// TODO Return NAN if undefined
		if (value.isObject()) {
			value = toPrimitive(ev, value.asObject());
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
	
	public String toString() {
		return properties.keySet().stream()
				.filter(key -> properties.get(key).isEnumerable())
				.map(key -> key + ": " + properties.get(key).getValue().toString())
				.collect(Collectors.joining(", ", "{", "}"));
	}
	
	protected void update() {
		if (ev != null) {
			ResourceLimits limits = ev.getResourceLimits();
			if (limits != null) {
				if (limits.maxObjectSize > 0 && getObjectSize() > limits.maxObjectSize)
					throw new TinyscriptResourceLimitViolation("Object size limit reached");
				ev.monitorObjectSizeChange(this);
			}
		}
	}

	public int getObjectSize() {
		return properties.size();
	}

}
