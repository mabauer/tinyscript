package de.mkbauer.tinyscript.runtime.object;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;

public class GetOwnPropertyDescriptor extends BuiltinFunction {
	
	private final static String NAME = "getOwnPropertyDescriptor";
	
	public GetOwnPropertyDescriptor(TinyscriptEngine engine) {
		super(engine);
	}
	
	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
		checkArgs(args);
		TSValue objectAsValue = args.get(0);
		String propertyName = args.get(1).asString();
		TSObject result = null;
		if (objectAsValue.isObject()) {
			TSObject object = objectAsValue.asObject();
			result = object.getOwnPropertyDescriptor(propertyName).toObject(engine);
		}
		return new TSValue(result);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		return 2;
	}

}
