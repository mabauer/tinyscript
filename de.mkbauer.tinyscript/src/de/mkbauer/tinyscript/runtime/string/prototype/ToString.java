package de.mkbauer.tinyscript.runtime.string.prototype;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.runtime.string.StringObject;

public class ToString extends BuiltinFunction {
	
	private final static String NAME = "toString";
	
	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
		if (!(self instanceof StringObject))
			throw new TinyscriptTypeError("Function String.prototype.toString only works for String objects.");
		return new TSValue(((StringObject)self).getValue());
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		return 0;
	}

	public ToString(TinyscriptEngine engine) {
		super(engine);
	}

}