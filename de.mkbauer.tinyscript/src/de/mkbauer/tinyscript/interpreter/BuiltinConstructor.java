package de.mkbauer.tinyscript.interpreter;

import java.util.List;

public abstract class BuiltinConstructor extends BuiltinFunction {

	public BuiltinConstructor(TinyscriptEngine engine) {
		super(engine);
		
		TSValue prototype = engine.getObjectPrototypeFor(getName());
		if (prototype == TSValue.UNDEFINED) {
			setPrototypeProperty(new TSObject(engine, engine.getDefaultPrototype()));
		}
	}

}
