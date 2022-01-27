package de.mkbauer.tinyscript.interpreter;

public class EmptyFunction extends BuiltinFunction {
	
	public EmptyFunction(TinyscriptEngine engine) {
		super(engine);
	}

	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		return TSValue.UNDEFINED;
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public int getLength() {
		return 0;
	}

}
