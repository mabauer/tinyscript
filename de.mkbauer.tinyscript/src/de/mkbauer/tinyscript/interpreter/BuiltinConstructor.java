package de.mkbauer.tinyscript.interpreter;

public abstract class BuiltinConstructor extends BuiltinFunction {

	public BuiltinConstructor(TinyscriptEngine engine) {
		super(engine);
		
		// Like Javascript, the instances get constructor from the prototype ([].constructor is Array)
		TSObject prototype = new TSObject(engine, engine.getDefaultPrototype());
		prototype.defineDefaultProperty("constructor", this);
		setPrototypeProperty(prototype);
	}
	
	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		return self;
	}
	
	public abstract TSObject createObject();

	@Override
	public boolean isConstructor() {
		return true;
	}

}
