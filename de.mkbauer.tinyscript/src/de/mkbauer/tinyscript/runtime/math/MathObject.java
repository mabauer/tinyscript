package de.mkbauer.tinyscript.runtime.math;

import de.mkbauer.tinyscript.interpreter.GlobalExecutionContext;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class MathObject extends TSObject {
	
	public MathObject(GlobalExecutionContext globalContext) {
		super(globalContext.getDefaultPrototype());
		
		defineDefaultProperty(this, "PI", java.lang.Math.PI);
		defineDefaultProperty(this, "sqrt", new Sqrt(globalContext));
	}
	
}