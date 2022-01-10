package de.mkbauer.tinyscript.interpreter;

import java.util.List;

public class EmptyFunction extends BuiltinFunction {
	
	public EmptyFunction(TinyscriptEngine engine) {
		super(engine);
	}

	@Override
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
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
