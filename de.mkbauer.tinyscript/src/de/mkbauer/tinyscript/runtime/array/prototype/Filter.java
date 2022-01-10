package de.mkbauer.tinyscript.runtime.array.prototype;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.ResourceMonitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;

public class Filter extends BuiltinFunction {
	
	private final static String NAME = "filter";
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self,
			List<TSValue> args) {
		// TODO: Should be generic 
		checkArgs(args);
		if (!(self instanceof ArrayObject))
			throw new TinyscriptTypeError("Function Array.prototype.filter only works for Array objects.");
		ArrayObject arr = (ArrayObject) self;
		ArrayObject result = new ArrayObject(engine);
		TSValue functionAsValue = args.get(0);
		if (functionAsValue.isCallable()) {
			Function function = (Function) functionAsValue.asObject();
			if (function.getLength() > 0) {
				for (int i = 0; i < arr.getLength(); i++) {
					List<TSValue> argsToFunction =new ArrayList<TSValue>();
					argsToFunction.add(arr.get(String.valueOf(i)));
					TSValue test = function.apply(false, null, argsToFunction);
					if (test.asBoolean())
						result.push(arr.get(String.valueOf(i)));
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

	public Filter(TinyscriptEngine engine) {
		super(engine);
	}

}