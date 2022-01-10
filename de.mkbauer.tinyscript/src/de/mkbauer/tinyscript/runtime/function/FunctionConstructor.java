package de.mkbauer.tinyscript.runtime.function;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.GlobalExecutionContext;
import de.mkbauer.tinyscript.interpreter.InterpretedFunction;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class FunctionConstructor extends BuiltinConstructor {
	
	private static final String NAME = "Function";
	
	public FunctionConstructor(TinyscriptEngine engine) {
		super(engine);
		
		// Object is a function, so its prototype is the prototype of all functions
		// which we will use to create new functions
		setPrototypeProperty(engine.getGlobalContext().get("Object").asObject().getPrototype());
	}
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
		// TODO Auto-generated method stub
		// ev.checkAndIncreaseObjectCreations();
		return new TSValue(new InterpretedFunction(engine));
	}
	
	public String getName() {
		return NAME;
	}
	
	public int getLength() {
		// TODO: Find out why?
		return 1;
	}



}
