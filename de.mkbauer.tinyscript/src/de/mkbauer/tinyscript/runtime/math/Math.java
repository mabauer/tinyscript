package de.mkbauer.tinyscript.runtime.math;

import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class Math extends TSObject {

	@Override
	protected void initialize() {
		super.initialize();
		defineDefaultProperty(this, "PI", java.lang.Math.PI);
		defineDefaultProperty(this, "sqrt", new Sqrt());
	}
	
	

}
