package de.mkbauer.tinyscript.runtime.object;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.EmptyFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.function.prototype.Call;
import de.mkbauer.tinyscript.runtime.object.prototype.HasOwnProperty;


public class ObjectConstructor extends BuiltinConstructor {
	
	private final static String NAME = "Object";
	
	public ObjectConstructor(TinyscriptEngine engine) {
		super(engine);
					
	}
	
	public void initialize() {
		
		
		// In order to correctly initialize the Object constructor function,
		// we need to define the prototype of all functions and the prototype of all objects.
		
		// We are the Object constructor, aka function Object()
		// Store ourselves into the global context, because
		// other functions that we will add to our prototype next will need this.
		engine.getGlobalContext().store("Object", new TSValue(this));
		
		// Let's define our prototype (__proto__), the prototype of all functions
		Function proto = new EmptyFunction(engine); 
		// Property: __proto__
		this.setPrototype(proto);

		proto.setPrototype(engine.getDefaultPrototype());
		proto.setPrototypeProperty(proto);
		
		
		// Add all the basic stuff to our prototype, the prototype of all functions
		TSObject.defineDefaultProperty(proto, "toString", new de.mkbauer.tinyscript.runtime.function.prototype.ToString(engine));
		TSObject.defineDefaultProperty(proto, "call", new Call(engine));
		TSObject.defineDefaultProperty(proto, "length", new TSValue(getLength()));
		// TODO: Remove, just for testing
		TSObject.defineDefaultProperty(proto, "isCallable", new TSValue(true));
		
		// Object.create(proto), Object.keys(obj)...
		defineDefaultProperty(this, "create", new Create(engine));
		defineDefaultProperty(this, "getPrototypeOf", new GetPrototypeOf(engine));
		defineDefaultProperty(this, "keys", new Keys(engine));
		
		// Now, that we have the prototype of all functions, we can safely add functions to the prototype of all objects
		initializeDefaultPrototype(engine, engine.getDefaultPrototype());
		
		setPrototypeProperty(engine.getDefaultPrototype());	
	}
	
	private void initializeDefaultPrototype(TinyscriptEngine engine, TSObject defaultPrototype) {
		defineDefaultProperty(defaultPrototype, "toString", new de.mkbauer.tinyscript.runtime.object.prototype.ToString(engine));
		defineDefaultProperty(defaultPrototype, "hasOwnProperty", new HasOwnProperty(engine));
	}
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
		// TODO Handle arguments
		return new TSValue(new TSObject(engine, engine.getDefaultPrototype()));
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		// TODO Find out why?
		return 1;
	}


}
