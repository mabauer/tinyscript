package de.mkbauer.tinyscript.runtime.array.prototype;


import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.ResourceMonitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;

public class ToString extends BuiltinFunction {
	
	private final static String NAME = "toString";
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self,
			List<TSValue> args) {
		// TODO: Should be generic (implemented by calling join)
		if (!(self instanceof ArrayObject))
			throw new TinyscriptTypeError("Function Array.prototype.toString only works for Array objects.");
		ArrayObject arr = (ArrayObject) self;
		String result = arr.getItems().stream()
				.map(item->TSObject.toString(engine, item))
				.collect(Collectors.joining(", "));
		ResourceMonitor monitor = engine.getResourceMonitor();
		engine.recordStringCreation(result);
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