package de.mkbauer.tinyscript.interpreter;

import de.mkbauer.tinyscript.runtime.function.FunctionObject;
import de.mkbauer.tinyscript.runtime.math.MathObject;
import de.mkbauer.tinyscript.runtime.object.ObjectObject;


public class GlobalExecutionContext extends ExecutionContext {
	
	private TSObject globalObject;
	
	private TSObject objectPrototype;

	public GlobalExecutionContext() {
		super("global", null);
		
		globalObject = new TSObject();
		objectPrototype = new TSObject();
		globalObject.setPrototype(objectPrototype);
		
		// TODO: In strict mode, this should be TSValue.undefined
		thisRef = globalObject;
		
		// Caveat: Object needs to be initialized first!
		TSObject.defineDefaultProperty(globalObject, "Object", new ObjectObject(this));

		TSObject.defineDefaultProperty(globalObject, "Function", new FunctionObject(this));
		TSObject.defineDefaultProperty(globalObject, "Math", new MathObject(this));


	}
	
	@Override
	public boolean isGlobal() {
		return true;
	}
	
	public TSObject getGlobalObject() {
		return globalObject;
	}
	
	public TSObject getDefaultPrototype() {
		return objectPrototype;
	}
	
	public TSValue getObjectPrototypeFor(String objectName) {
		TSValue value = get(objectName);
		Function object = null;
		if (value != TSValue.UNDEFINED) {
			object = (Function) value.asObject();
			TSValue prototype = object.getPrototypeProperty();
			return prototype;
		}
		return TSValue.UNDEFINED;
	}

	@Override
	public TSValue get(String identifier) {
		return globalObject.get(identifier);
	}

	@Override
	public void create(String identifier) {
		// TODO: Check if it already exists?
		globalObject.put(identifier, TSValue.UNDEFINED);
	}

	@Override
	public TSValue lookup(String identifier) {
		return globalObject.get(identifier);
	}

	@Override
	public void store(String identifier, TSValue value) {
		globalObject.put(identifier, value);
	}

	@Override
	public boolean contains(String identifier) {
		// TODO Auto-generated method stub
		return !globalObject.get(identifier).equals(TSValue.UNDEFINED);
	}
	
	

}
