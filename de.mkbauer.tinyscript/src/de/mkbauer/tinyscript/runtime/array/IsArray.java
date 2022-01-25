package de.mkbauer.tinyscript.runtime.array;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class IsArray extends BuiltinFunction {
	
	private final static String NAME = "isArray";

	public IsArray(TinyscriptEngine engine) {
		super(engine);
	}
	
	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		checkArgs(args);
		TSValue object = args[0];
		if (object.isObject() && object.asObject() instanceof ArrayObject) 
			return new TSValue(true);
		else 
			return new TSValue(false);
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
