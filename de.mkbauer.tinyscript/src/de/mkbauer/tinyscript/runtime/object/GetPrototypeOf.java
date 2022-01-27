package de.mkbauer.tinyscript.runtime.object;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;

public class GetPrototypeOf extends BuiltinFunction {

	public GetPrototypeOf(TinyscriptEngine engine) {
		super(engine);
	}

	private final static String NAME = "getPrototypeOf";
	
	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		checkArgs(args);
		TSValue objectAsValue = args[0];
		if (objectAsValue.isObject()) {
			TSObject object = objectAsValue.asObject();
			TSObject result = object.getPrototype();
			return new TSValue(result);
		}
		return TSValue.NULL;
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
