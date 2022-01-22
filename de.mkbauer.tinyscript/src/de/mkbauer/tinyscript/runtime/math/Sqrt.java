package de.mkbauer.tinyscript.runtime.math;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

@Deprecated
public class Sqrt extends BuiltinFunction {
	
	private static final String NAME = "sqrt";
	
	public Sqrt(TinyscriptEngine engine) {
		super(engine);
	}

	@Override
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
		checkArgs(args);
		return new TSValue(java.lang.Math.sqrt(TSObject.toNumber(engine, args.get(0))));
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
