package de.mkbauer.tinyscript.runtime.math;

import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.GlobalExecutionContext;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class MathObject extends TSObject {
	
	public MathObject(TinyscriptEngine engine) {
		super(engine, engine.getDefaultPrototype());
		
		defineDefaultProperty(this, "PI", java.lang.Math.PI);
		defineDefaultProperty(this, "sqrt", new Sqrt(engine));
		defineDefaultProperty(this, "round", new Round(engine));
		defineDefaultProperty(this, "random", new Random(engine));
	}
	
}
