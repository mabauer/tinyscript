package de.mkbauer.tinyscript.runtime.function;

import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.GlobalExecutionContext;

public class FunctionObject extends Function {
	
	public FunctionObject(GlobalExecutionContext globalContext) {
		super(globalContext);
		
		// Property: prototype
		setPrototypeProperty(getPrototype());
	}
	
	public String getName() {
		return "Function";
	}
	
	public int getLength() {
		// TODO: Find out why?
		return 1;
	}

}
