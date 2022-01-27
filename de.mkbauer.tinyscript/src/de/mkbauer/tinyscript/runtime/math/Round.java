package de.mkbauer.tinyscript.runtime.math;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

@Deprecated
public class Round extends BuiltinFunction {
	
	private static final String NAME = "round";
	
	public Round(TinyscriptEngine engine) {
		super(engine);
	}

	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		checkArgs(args);
		return new TSValue(java.lang.Math.round(TSObject.toNumber(engine, args[0])));
	}
	

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		return 1;
	}

}