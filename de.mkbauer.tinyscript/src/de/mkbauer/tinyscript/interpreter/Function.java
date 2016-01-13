package de.mkbauer.tinyscript.interpreter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.runtime.function.Call;
import de.mkbauer.tinyscript.runtime.function.ToString;

public abstract class Function extends TSObject {
	
	protected ExecutionVisitor ev;
	
	public Function(ExecutionVisitor ev) {
		this.ev = ev;
		
		TSObject proto = null;
		// Object Object is a function as well, so we can get it's prototype and use it.
		TSObject objectobject = ev.getGlobalContext().get("Object").asObject();
		if (objectobject != null) {
			proto = objectobject.getPrototype();
			setPrototype(proto);
		}
		else {
			// We don't have the Object Object yet, so we are Object Object (hopefully!)
			// ... and we have to create the prototype for all functions
			proto = new TSObject(ev.getDefaultPrototype());
			// Property: __proto__
			setPrototype(proto);
			// Store our unfinished Object Object into the global context, because
			// other functions that we will add to our prototype next will need it.
			ev.getGlobalContext().store("Object", new TSValue(this));
			TSObject.defineDefaultProperty(proto, "toString", new ToString(ev));
			TSObject.defineDefaultProperty(proto, "call", new Call(ev));
			TSObject.defineDefaultProperty(proto, "length", new TSValue(getLength()));
			// TODO: Remove, just for testing
			TSObject.defineDefaultProperty(proto, "isCallable", new TSValue(true));
		}
	}
	
	public void setPrototypeProperty(Object prototype) {
		defineDefaultProperty(this, "prototype", prototype);
	}
	
	public TSValue getPrototypeProperty() {
		return get("prototype");
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

}
