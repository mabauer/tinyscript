package de.mkbauer.tinyscript.runtime.function.prototype;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.InterpretedFunction;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class ToString extends BuiltinFunction {
	
	private final static String NAME = "toString";
	
	@Override
	public TSValue apply(TSObject self, TSValue args[]) {
		/* 
		if (!(self instanceof Function)) {
			throw new TinyscriptTypeError("Function Function.prototype.toString only works for Function objects");
		}
		*/
		String result;
		if (self instanceof InterpretedFunction) {
			InterpretedFunction function = (InterpretedFunction) self;
			result = function.getCodeAsString();
		}
		else {
			if (self instanceof Function) {
				Function function = (Function) self;
				result = "function " + function.getName() + "() { // builtin }";
			}
			else {
				result = "function " + "() { // builtin }";
			}
		}
		return new TSValue(result);
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
