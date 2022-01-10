package de.mkbauer.tinyscript.interpreter;

public abstract class BuiltinType extends TSObject {
	
	public BuiltinType(TinyscriptEngine engine) {
		super(engine);
		
		TSValue proto = engine.getObjectPrototypeFor(getConstructorName());
		if (proto == TSValue.UNDEFINED) {
			// This should not happen! => Error handling
			throw new TinyscriptReferenceError("Undefined constructor for " + getClass());
		}
		setPrototype(proto.asObject());
	}
	
	public abstract TSValue valueOf();
	
	public abstract String getConstructorName();

}
