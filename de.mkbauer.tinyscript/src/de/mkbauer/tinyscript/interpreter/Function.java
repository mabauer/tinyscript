package de.mkbauer.tinyscript.interpreter;

import java.util.List;
import java.util.stream.Collectors;

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
	
	public abstract TSValue apply(TSObject self, List<TSValue> args);
			
	public String getName() {
		return "";
	}
	
	public int getLength() {
		// TODO: Find out why?
		return 0;
	}

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
