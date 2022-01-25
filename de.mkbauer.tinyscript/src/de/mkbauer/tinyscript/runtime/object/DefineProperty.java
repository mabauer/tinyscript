package de.mkbauer.tinyscript.runtime.object;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSPropertyDescriptor;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

public class DefineProperty extends BuiltinFunction {
	
	private final static String NAME = "defineProperty";
	
	public DefineProperty(TinyscriptEngine engine) {
		super(engine);
	}
	
	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		checkArgs(args);
		TSValue objectAsValue = args[0];
		String propertyName = args[1].asString();
		TSValue propertyDescriptorAsValue = args[2];
		if (objectAsValue.isObject()) {
			TSObject object = objectAsValue.asObject();
			if (propertyDescriptorAsValue.isObject()) {
				TSPropertyDescriptor propertyDescriptor = TSPropertyDescriptor.fromObject(propertyDescriptorAsValue.asObject());
				object.setOwnPropertyDescriptor(propertyName, propertyDescriptor);
			}
			return new TSValue(object);
		}
		throw new TinyscriptTypeError("Object.defineProperty called on non-object");
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		return 3;
	}

}
