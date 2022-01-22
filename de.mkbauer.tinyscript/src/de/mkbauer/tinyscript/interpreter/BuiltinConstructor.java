package de.mkbauer.tinyscript.interpreter;

import java.util.List;

public abstract class BuiltinConstructor extends BuiltinFunction {

	public BuiltinConstructor(TinyscriptEngine engine) {
		super(engine);
		
		setPrototypeProperty(new TSObject(engine, engine.getDefaultPrototype()));

	}
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
		return new TSValue(self);
	}
	
	public abstract TSObject createObject();

}
