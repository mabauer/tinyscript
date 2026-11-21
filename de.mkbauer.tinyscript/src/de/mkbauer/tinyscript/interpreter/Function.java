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

	// Like Javascript, f.length is the number of parameters (it came from Function.prototype)
	@Override
	public TSValue get(String key) {
		if (key.equals("length") && !hasOwnProperty("length"))
			return TSValue.of(getLength());
		return super.get(key);
	}

	@Override
	protected String display(Display display) {
		String result = "";
		if (hasEnumerableProperties())
			result = result + "{ ";
		result = result + "[Function";
		if (getName() != null)
			result = result + ": " + getName(); 
		result = result + "]";
		if (hasEnumerableProperties())
			result = result + " " + properties.keySet().stream()
				.filter(key->properties.get(key).isEnumerable())
				.map(key -> key + ": " + display.format(properties.get(key).getValue()))
				.collect(Collectors.joining(", ")) + " }";
		return result;
	}

	public boolean isArrowFunction() {
		return isArrowFunction;
	}

	public void setArrowFunction(boolean isArrowFunction) {
		this.isArrowFunction = isArrowFunction;
	}

}
