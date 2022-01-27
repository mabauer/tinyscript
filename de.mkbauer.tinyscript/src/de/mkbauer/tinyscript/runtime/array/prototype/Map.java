package de.mkbauer.tinyscript.runtime.array.prototype;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;

public class Map extends BuiltinFunction {
	
	private final static String NAME = "map";
	
	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		// TODO: Should be generic 
		checkArgs(args);
		if (!(self instanceof ArrayObject))
			throw new TinyscriptTypeError("Function Array.prototype.map only works for Array objects.");
		ArrayObject arr = (ArrayObject) self;
		ArrayObject result = (ArrayObject) engine.getConstructor("Array").createObject();
		TSValue functionAsValue = args[0];
		if (functionAsValue.isCallable()) {
			Function function = (Function) functionAsValue.asObject();
			if (function.getLength() > 0) {
				for (int i = 0; i < arr.getLength(); i++) {
					TSValue[] argsToFunction = new TSValue[1];
					argsToFunction[0] = arr.get(String.valueOf(i));
					TSValue newItem = function.apply(null, argsToFunction);
					result.push(newItem);
				}
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
		return 1;
	}

	public Map(TinyscriptEngine engine) {
		super(engine);
	}

}