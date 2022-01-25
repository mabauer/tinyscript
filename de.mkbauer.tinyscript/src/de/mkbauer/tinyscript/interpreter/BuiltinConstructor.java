package de.mkbauer.tinyscript.interpreter;

public abstract class BuiltinConstructor extends BuiltinFunction {

	public BuiltinConstructor(TinyscriptEngine engine) {
		super(engine);
		
		setPrototypeProperty(new TSObject(engine, engine.getDefaultPrototype()));

	}
	
	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		return new TSValue(self);
	}
	
	public abstract TSObject createObject();

}
