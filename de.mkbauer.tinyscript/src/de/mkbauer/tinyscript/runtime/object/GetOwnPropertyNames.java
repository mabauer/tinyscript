package de.mkbauer.tinyscript.runtime.object;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.runtime.array.ArrayConstructor;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;

public class GetOwnPropertyNames extends BuiltinFunction {
	
	private final static String NAME = "getOwnPropertyNames";
	
	public GetOwnPropertyNames(TinyscriptEngine engine) {
		super(engine);
	}
	
	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
		checkArgs(args);
		TSValue objectAsValue = args.get(0);
		
		ArrayObject result = (ArrayObject) engine.getConstructor(ArrayConstructor.NAME).createObject();
		if (objectAsValue.isObject()) {
			TSObject object = objectAsValue.asObject();
			for (String propertyName : object.getOwnPropertyNames()) {
				result.push(new TSValue(propertyName));
				engine.recordStringCreation(propertyName);
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

}
