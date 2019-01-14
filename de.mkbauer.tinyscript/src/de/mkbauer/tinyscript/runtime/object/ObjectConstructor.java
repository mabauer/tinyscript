package de.mkbauer.tinyscript.runtime.object;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.EmptyFunction;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.InterpretedFunction;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.function.FunctionConstructor;
import de.mkbauer.tinyscript.runtime.function.prototype.Call;
import de.mkbauer.tinyscript.runtime.object.prototype.HasOwnProperty;


public class ObjectConstructor extends BuiltinConstructor {
	
	private final static String NAME = "Object";
	
	public ObjectConstructor(ExecutionVisitor ev) {
		super(ev);
					
	}
	
	public void initialize() {
		
		
		// In order to correctly initialize the Object constructor function,
		// we need to define the prototype of all functions and the prototype of all objects.
		
		// We are the Object constructor, aka function Object()
		// Store ourselves into the global context, because
		// other functions that we will add to our prototype next will need this.
		ev.getGlobalContext().store("Object", new TSValue(this));
		
		// Let's define our prototype (__proto__), the prototype of all functions
		Function proto = new EmptyFunction(ev); 
		// Property: __proto__
		this.setPrototype(proto);

		proto.setPrototype(ev.getDefaultPrototype());
		proto.setPrototypeProperty(proto);
		
		
		// Add all the basic stuff our prototype, the prototype of all functions
		TSObject.defineDefaultProperty(proto, "toString", new de.mkbauer.tinyscript.runtime.function.prototype.ToString(ev));
		TSObject.defineDefaultProperty(proto, "call", new Call(ev));
		TSObject.defineDefaultProperty(proto, "length", new TSValue(getLength()));
		// TODO: Remove, just for testing
		TSObject.defineDefaultProperty(proto, "isCallable", new TSValue(true));
		
		// Object.keys(obj)...
		defineDefaultProperty(this, "keys", new Keys(ev));
		
		// Now, that we have the prototype of all functions, we can safely add functions to the prototype of all objects
		initializeDefaultPrototype(ev, ev.getDefaultPrototype());
		
		setPrototypeProperty(ev.getDefaultPrototype());	
	}
	
	private void initializeDefaultPrototype(ExecutionVisitor ev, TSObject defaultPrototype) {
		defineDefaultProperty(defaultPrototype, "toString", new de.mkbauer.tinyscript.runtime.object.prototype.ToString(ev));
		defineDefaultProperty(defaultPrototype, "hasOwnProperty", new HasOwnProperty(ev));
	}
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
		// TODO Handle arguments
		return new TSValue(new TSObject(ev, ev.getDefaultPrototype()));
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
