package de.mkbauer.tinyscript.runtime.object;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;

public class Keys extends BuiltinFunction {
		
	public Keys(TinyscriptEngine engine) {
		super(engine);
	}

	private final static String NAME = "keys";
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self,
			List<TSValue> args) {
		checkArgs(args);
		TSValue objectAsValue = args.get(0);
		ArrayObject result = new ArrayObject(engine);
		if (objectAsValue.isObject()) {
			TSObject object = objectAsValue.asObject();
			for (String key : object.getKeys()) {
				result.push(new TSValue(key));
				engine.recordStringCreation(key);
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