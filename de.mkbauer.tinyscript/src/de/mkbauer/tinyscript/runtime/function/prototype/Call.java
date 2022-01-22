package de.mkbauer.tinyscript.runtime.function.prototype;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class Call extends BuiltinFunction {
	
	private final static String NAME = "call";

	public Call(TinyscriptEngine engine) {
		super(engine);
	}

	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
		checkArgs(args);
		if (self instanceof Function) {
			Function function = (Function) self;
			TSValue this_ = args.get(0);
			return function.apply(this_.asObject(), args.subList(1, args.size()));			
		}
		return TSValue.UNDEFINED;
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
