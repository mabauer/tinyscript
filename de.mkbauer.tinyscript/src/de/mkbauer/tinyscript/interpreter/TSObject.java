package de.mkbauer.tinyscript.interpreter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.runtime.string.StringConstructor;


public non-sealed class TSObject implements TSValue {
	
	// Like Javascript, the properties keep their insertion order (Object.keys, printing)
	protected HashMap<String, TSPropertyDescriptor> properties;
	
	private TSObject proto = null;
	protected TinyscriptEngine engine;
	
	protected TSObject(TinyscriptEngine engine) {
		this.engine = engine;
		properties = new LinkedHashMap<String, TSPropertyDescriptor>();
		engine.recordObjectCreation(this);
	}
	
	public TSObject(TinyscriptEngine engine, TSObject proto) {
		this.engine = engine;
		properties = new LinkedHashMap<String, TSPropertyDescriptor>();
		engine.recordObjectCreation(this);
		if (proto != null)
			setPrototype(proto);
	}
	
	/**
	 * String conversion for print, string concatenation and String(x): a string stays as it is,
	 * anything else gets its display form (see {@link Display}), which uses a user-defined
	 * toString (a function written in tinyscript), also for the objects inside arrays and objects.
	 */
	public static String stringify(TSValue value) {
		return value instanceof TSString s ? s.value() : new Display(true).format(value);
	}

	public static String toString(TinyscriptEngine engine, TSValue value) {
		if (value == TSValue.UNDEFINED)
			return "[object Undefined]";
		if (value == TSValue.NULL)
			return "[object Null]";
		if (value instanceof TSObject object && object.get("toString") instanceof Function toString) {
			TSValue result = toString.apply(object, new TSValue[0]);
			if (result.isString())
				return result.asString();
		}
		return value.asString();
	}
	
	/**
	 * The object used for a property access on {@code value}. Only strings have a
	 * wrapper object (with length, charAt, ...); unlike Javascript, numbers and
	 * booleans have no properties at all.
	 */
	public static TSObject toObject(TinyscriptEngine engine, TSValue value) throws TinyscriptTypeError {
		return switch (value) {
			case TSObject object -> object;
			case TSString string -> ((StringConstructor) engine.getConstructor(StringConstructor.NAME)).createObject(string);
			case TSUndefined u -> throw new TinyscriptTypeError("Cannot convert 'undefined' to an object");
			case TSNull n -> throw new TinyscriptTypeError("Cannot convert 'null' to an object");
			case TSNumber n -> throw new TinyscriptTypeError("Numbers have no properties");
			case TSBoolean b -> throw new TinyscriptTypeError("Booleans have no properties");
		};
	}
	
	/**
	 * Resolves the index argument of at(): a negative index counts from the end.
	 * Returns -1 if it is out of range; unlike Javascript, a non-integer index is a TypeError.
	 */
	public static int toRelativeIndex(TSValue index, int length) {
		if (!index.isMathematicalInteger())
			throw new TinyscriptTypeError("at() needs an integer index, not " + index.asString());
		double position = index.asDouble() < 0 ? length + index.asDouble() : index.asDouble();
		return (position >= 0 && position < length) ? (int) position : -1;
	}

	/**
	 * The optional start index argument of indexOf() (0 if missing). Unlike Javascript,
	 * a non-integer start index is a TypeError.
	 */
	public static int toStartIndex(TSValue[] args, int position) {
		if (args.length <= position)
			return 0;
		TSValue start = args[position];
		if (!start.isMathematicalInteger())
			throw new TinyscriptTypeError("indexOf() needs an integer start index, not " + start.asString());
		return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, start.asDouble()));
	}

	public void defineDefaultProperty(String key, Object value) {
		TSPropertyDescriptor desc = new TSPropertyDescriptor();
		desc.setValue(TSValue.fromJava(value));
		// This differs form ECMAScript specs.
		desc.setConfigurable(true);
		desc.setEnumerable(false);
		desc.setWritable(true);
		setOwnPropertyDescriptor(key, desc);
	}
	
	/**
	 * delete o.key: removes an own property; like Javascript, a missing one is fine. Built-in
	 * properties (Math.PI, f.prototype, …), which are not enumerable, cannot be deleted.
	 * Arrays, Maps, Sets and strings refuse (see their overrides).
	 */
	public void delete(String key) {
		TSPropertyDescriptor desc = properties.get(key);
		if (desc == null)
			return;
		if (!desc.isEnumerable() || !desc.isConfigurable())
			throw new TinyscriptTypeError("Cannot delete '" + key + "': it is a built-in (not enumerable) property");
		properties.remove(key);
		update();
	}

	public TSPropertyDescriptor getOwnPropertyDescriptor(String key) {
		return properties.get(key);
	}
	
	/**
	 * Defines or replaces an own property. A non-configurable property cannot be redefined
	 * (simpler than Javascript, which allows a few compatible changes).
	 */
	public void setOwnPropertyDescriptor(String key, TSPropertyDescriptor desc) {
		TSPropertyDescriptor existing = properties.get(key);
		if (existing != null && !existing.isConfigurable())
			throw new TinyscriptTypeError("Cannot redefine property: " + key);
		properties.put(key,  desc);
	}
	
	public boolean hasOwnProperty(String key) {
		return getOwnPropertyDescriptor(key) != null;
	}
	
	/**
	 * Defines a builtin method that takes exactly {@code length} arguments.
	 */
	public void defineBuiltinMethod(String name, BuiltinFunctionImplementation implementation, int length) {
		defineDefaultProperty(name, engine.defineBuiltinFunction(name, implementation, length));
	}

	/**
	 * Defines a builtin method that takes {@code minArgs} to {@code maxArgs} arguments.
	 */
	public void defineBuiltinMethod(String name, BuiltinFunctionImplementation implementation, int minArgs, int maxArgs) {
		defineDefaultProperty(name, engine.defineBuiltinFunction(name, implementation, minArgs, maxArgs));
	}
	
	
	public void setPrototype(TSObject proto) {
		this.proto = proto;
		defineDefaultProperty("__proto__", proto != null ? proto : TSValue.NULL);
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
		if (desc != null) {
			if (desc.isWritable()) 
				desc.setValue(value);
			else {
				// log.debug("Error: assigning to a non-writable key: " + key);
			}
		}
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
	
	public List<String> getOwnPropertyNames() {
		return (properties.keySet().stream()
				.filter(key -> !key.startsWith("__"))
				.collect(Collectors.toList()));
	}
	
	/**
	 * The display form without user-defined toString methods (REPL, error messages).
	 * Subclasses override {@link #display(Display)}, not this method.
	 */
	@Override
	public final String toString() {
		return new Display(false).format(this);
	}

	/**
	 * This object's display form: {x: 1, y: "a"}. The values inside go through {@code display.format}.
	 */
	protected String display(Display display) {
		return classPrefix() + properties.keySet().stream()
				.filter(key -> properties.get(key).isEnumerable())
				.map(key -> key + ": " + display.format(properties.get(key).getValue()))
				.collect(Collectors.joining(", ", "{", "}"));
	}

	// An instance of a class shows its name first, as node does: Num {value: 3}
	private String classPrefix() {
		TSObject prototype = getPrototype();
		if (prototype == null)
			return "";
		TSPropertyDescriptor constructor = prototype.getOwnPropertyDescriptor("constructor");
		if (constructor != null && constructor.getValue() instanceof ClassObject cls && cls.getPrototypeProperty() == prototype)
			return cls.getName() + " ";
		return "";
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
