package de.mkbauer.tinyscript.runtime.function.prototype;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class Apply extends BuiltinFunction {
	
	private final static String NAME = "apply";

	public Apply(TinyscriptEngine engine) {
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
			
			if (args.length > 1 && args[1].isArray()) {
				ArrayObject argsArray = args[1].asArray();
				return function.apply(thisObject, argsArray.asJavaArray());	
			}
			else {
				return function.apply(thisObject, new TSValue[0]);
			}
					
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
