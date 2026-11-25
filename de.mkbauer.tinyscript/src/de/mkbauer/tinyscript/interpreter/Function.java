package de.mkbauer.tinyscript.interpreter;

import java.util.stream.Collectors;

public abstract class Function extends TSObject {
	
	private boolean isArrowFunction = false;
	
	public Function(TinyscriptEngine engine) {
		super(engine);
		
		// Object is a function as well, so we can get it's prototype and use it.
		// (While the builtins are set up, Object may not exist yet.)
		if (engine.getGlobalContext().get("Object") instanceof Function objectConstructor)
			setPrototype(objectConstructor.getPrototype());

	}
	
	public void setPrototypeProperty(Object prototype) {
		defineDefaultProperty("prototype", prototype);
	}
	
	public TSValue getPrototypeProperty() {
		return get("prototype");
	}
	
	public boolean hasInstance(TSValue value) {
		if (!(value instanceof TSObject start))
			return false;
		if (!(getPrototypeProperty() instanceof TSObject prototypeProperty))
			throw new TinyscriptTypeError("Constructor " + getName() + " does not have a prototype property");
		for (TSObject object = start; object != null; object = object.getPrototype()) {
			if (object == prototypeProperty) 
				return true;
		}
		return false;
	}
	
	public abstract TSValue apply(TSObject self, TSValue[] args);
			
	public abstract String getName();
	
	public abstract int getLength();

	// Like Javascript, f.length is the number of parameters (it came from Function.prototype) and
	// f.name the name of the function ("" for an anonymous one)
	@Override
	public TSValue get(String key) {
		if (key.equals("length") && !hasOwnProperty("length"))
			return TSValue.of(getLength());
		if (key.equals("name") && !hasOwnProperty("name"))
			return TSValue.of(getName() != null ? getName() : "");
		return super.get(key);
	}

	// Like Javascript, length and name are own properties of each function
	@Override
	public java.util.List<String> getOwnPropertyNames() {
		java.util.List<String> result = new java.util.ArrayList<>();
		for (String key : java.util.List.of("length", "name")) {
			if (!hasOwnProperty(key))
				result.add(key);
		}
		result.addAll(super.getOwnPropertyNames());
		return result;
	}

	// Like Javascript's strict mode: f.length and f.name are read-only
	@Override
	public void put(String key, TSValue value) {
		if ((key.equals("length") || key.equals("name")) && !hasOwnProperty(key))
			throw new TinyscriptTypeError("Cannot assign to the read-only property '" + key + "' of a function");
		super.put(key, value);
	}

	/**
	 * Whether new can call this function: as in Javascript, classes, functions written with
	 * function and the builtin constructors, but not arrow functions, methods and other builtins.
	 */
	public boolean isConstructor() {
		return false;
	}

	// "an arrow function", "a method" ..., for the error of new
	public String kind() {
		return "a function";
	}

	@Override
	protected String display(Display display) {
		String result = "";
		if (hasEnumerableProperties())
			result = result + "{ ";
		result = result + label();
		if (hasEnumerableProperties())
			result = result + " " + orderedKeys().stream()
				.filter(key->properties.get(key).isEnumerable())
				.map(key -> key + ": " + display.format(properties.get(key).getValue()))
				.collect(Collectors.joining(", ")) + " }";
		return result;
	}

	// [Function: name], or [Function] without a name
	protected String label() {
		return "[Function" + (getName() != null && !getName().isEmpty() ? ": " + getName() : "") + "]";
	}

	public boolean isArrowFunction() {
		return isArrowFunction;
	}

	public void setArrowFunction(boolean isArrowFunction) {
		this.isArrowFunction = isArrowFunction;
	}

}
