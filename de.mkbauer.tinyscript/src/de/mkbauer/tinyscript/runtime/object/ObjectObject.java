package de.mkbauer.tinyscript.runtime.object;

import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.GlobalExecutionContext;
import de.mkbauer.tinyscript.interpreter.TSObject;

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

}
