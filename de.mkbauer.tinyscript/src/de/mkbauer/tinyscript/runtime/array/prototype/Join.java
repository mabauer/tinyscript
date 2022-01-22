package de.mkbauer.tinyscript.runtime.array.prototype;

import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;

public class Join extends BuiltinFunction {
	
	private final static String NAME = "join";
	
	public Join(TinyscriptEngine engine) {
		super(engine);
	}
	
	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
		if (!(self instanceof ArrayObject))
			throw new TinyscriptTypeError("Function Array.prototype.join only works for Array objects.");
		ArrayObject arr = (ArrayObject) self;
		String separator = "";
		if (args.size() >= 1) {
			if (args.get(0).isString()) {
				separator = args.get(0).asString();
			}
		}
		String result = arr.getItems().stream()
				.map(item->TSObject.toString(engine, item))
				.collect(Collectors.joining(separator));
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

}