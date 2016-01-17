package de.mkbauer.tinyscript.runtime.math;

import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.GlobalExecutionContext;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class MathObject extends TSObject {
	
	public MathObject(ExecutionVisitor ev) {
		super(ev, ev.getDefaultPrototype());
		
		defineDefaultProperty(this, "PI", java.lang.Math.PI);
		defineDefaultProperty(this, "sqrt", new Sqrt(ev));
		defineDefaultProperty(this, "round", new Round(ev));
		defineDefaultProperty(this, "random", new Random(ev));
	}
	
}
