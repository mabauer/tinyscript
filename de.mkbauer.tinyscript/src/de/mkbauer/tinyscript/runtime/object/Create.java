package de.mkbauer.tinyscript.runtime.object;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

public class Create extends BuiltinFunction {


		
	public Create(TinyscriptEngine engine) {
		super(engine);
	}

	private final static String NAME = "create";
	
	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		checkArgs(args);
		TSValue objectAsValue = args[0];
	
		TSObject result = null;
		if (objectAsValue.equals(TSValue.NULL)) {
			result = new TSObject(engine, null);
		}
		if (objectAsValue.isObject()) {
			TSObject object = objectAsValue.asObject();
			result = new TSObject(engine, object);
		}
		if (result != null) {
			return new TSValue(result);
		}
		throw new TinyscriptTypeError("Object.create(proto): proto must be an object!");
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