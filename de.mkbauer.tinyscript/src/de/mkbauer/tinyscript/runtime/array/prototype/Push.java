package de.mkbauer.tinyscript.runtime.array.prototype;

import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;
	

public class Push extends BuiltinFunction {
	
	private final static String NAME = "push";

	public Push(TinyscriptEngine engine) {
		super(engine);
	}
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self,
			List<TSValue> args) {
		checkArgs(args);
		if (!(self instanceof ArrayObject))
			throw new TinyscriptTypeError("Function Array.prototype.push only works for Array objects.");
		ArrayObject arr = (ArrayObject) self;
		for (TSValue arg : args) 
			arr.push(arg);
		return new TSValue(arr.getLength());
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