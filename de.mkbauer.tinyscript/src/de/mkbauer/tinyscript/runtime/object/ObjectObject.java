package de.mkbauer.tinyscript.runtime.object;

import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.GlobalExecutionContext;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.runtime.string.StringObject;

public class ObjectObject extends Function {
	
	public ObjectObject(GlobalExecutionContext globalContext) {
		super(globalContext);
		
		initializeDefaultPrototype(globalContext, globalContext.getDefaultPrototype());
		setPrototypeProperty(globalContext.getDefaultPrototype());
	}
	
	public static void initializeDefaultPrototype(GlobalExecutionContext globalContext, TSObject defaultPrototype) {
		defineDefaultProperty(defaultPrototype, "toString", new ToString(globalContext));
		defineDefaultProperty(defaultPrototype, "hasOwnProperty", new HasOwnProperty(globalContext));
		// defaultPrototype.put("toString", new TSValue(new ToString()));
	}

	@Override
	public String getName() {
		return "Object";
	}

	@Override
	public int getLength() {
		// TODO Find out why?
		return 1;
	}
	
	public static TSObject toObject(GlobalExecutionContext globalContext, TSValue value) throws TinyscriptTypeError {
		if (value == TSValue.UNDEFINED)  {
			throw new TinyscriptTypeError("Cannot convert 'undefined' to an object");
		}
		if (value == TSValue.NULL) {
			throw new TinyscriptTypeError("Cannot convert 'null' to an object");
		}
		if (value.isPrimitiveString()) {
			return new StringObject(globalContext, value);
		}
		if (value.isNumber()) {
			// TODO Create a NumberObject object
			TSObject result = new TSObject(globalContext.getDefaultPrototype()); 
			result.put("value", value);
			return result; 
		}
		if (value.isBoolean()) {
			// TODO Create a BooleanObject object
			TSObject result = new TSObject(globalContext.getDefaultPrototype()); 
			result.put("value", value);
			return result;
		}
		return value.asObject();
	}

}
