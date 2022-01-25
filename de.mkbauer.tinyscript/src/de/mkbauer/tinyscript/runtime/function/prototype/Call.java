package de.mkbauer.tinyscript.runtime.function.prototype;

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
	public TSValue apply(TSObject self, TSValue[] args) {
		checkArgs(args);
		if (self instanceof Function) {
			Function function = (Function) self;
			TSValue this_ = args[0];
			TSObject thisObject = null;
			if (this_.isObject())
				thisObject = this_.asObject();
			else 
				thisObject = engine.getGlobalContext().getGlobalObject();
			return function.apply(thisObject, java.util.Arrays.copyOfRange(args, 1, args.length));			
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
