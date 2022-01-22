package de.mkbauer.tinyscript.interpreter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.runtime.function.prototype.Call;
import de.mkbauer.tinyscript.runtime.function.prototype.ToString;

public abstract class Function extends TSObject {
	
	protected TinyscriptEngine engine;
	private boolean isArrowFunction = false;
	
	public Function(TinyscriptEngine engine) {
		super(engine);
		this.engine = engine;
		
		TSObject proto = null;
		// Object is a function as well, so we can get it's prototype and use it.
		Function objectobject = (Function) engine.getGlobalContext().get("Object").asObject();
		if (objectobject != null) {
			proto = objectobject.getPrototype();
			setPrototype(proto);
		}
		
	}
	
	public void setPrototypeProperty(Object prototype) {
		defineDefaultProperty("prototype", prototype);
	}
	
	public TSValue getPrototypeProperty() {
		return get("prototype");
	}
	
	public boolean hasInstance(TSValue value) {
		if (!value.isObject())
			return false;
		TSObject object = null;
		TSValue prototypePropertyAsValue = getPrototypeProperty();
		if (!prototypePropertyAsValue.isObject())
			throw new TinyscriptTypeError("Constructor " + getName() + " does not have a prototype property");
		TSObject prototypeProperty = prototypePropertyAsValue.asObject();
		for (object = value.asObject() ; object != null; object = object.getPrototype()) {
			if (object == prototypeProperty) 
				return true;
		}
		return false;
	}
	
	public TSValue call(boolean asConstructor, TSObject self, TSValue... args) {
		return apply(asConstructor, self, Arrays.<TSValue>asList(args));
	}
	
	public abstract TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args);
			
	public abstract String getName();
	
	public abstract int getLength();

	public String toString() {
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
				.map(key->key+": "+properties.get(key).toString())
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
