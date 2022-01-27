package de.mkbauer.tinyscript.runtime.function;


import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.InterpretedFunction;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class FunctionConstructor extends BuiltinConstructor {
	
	public static final String NAME = "Function";
	
	public FunctionConstructor(TinyscriptEngine engine) {
		super(engine);
		
		// Object is a function, so its prototype is the prototype of all functions
		// which we will use to create new functions
		setPrototypeProperty(engine.getGlobalContext().get("Object").asObject().getPrototype());
	}
	
	public String getName() {
		return NAME;
	}
	
	public int getLength() {
		// TODO: Find out why?
		return 1;
	}

	@Override
	public TSObject createObject() {
		TSObject newObject = new InterpretedFunction(engine);
		newObject.defineDefaultProperty("constructor", new TSValue(this));
		return newObject;
	}



}
