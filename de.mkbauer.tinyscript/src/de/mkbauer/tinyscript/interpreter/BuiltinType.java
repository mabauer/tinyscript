package de.mkbauer.tinyscript.interpreter;

public abstract class BuiltinType extends TSObject {
	
	public BuiltinType(TinyscriptEngine engine) {
		super(engine);
	}
	
	public abstract TSValue valueOf();

}
