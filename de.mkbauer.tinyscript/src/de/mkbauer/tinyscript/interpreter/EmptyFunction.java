package de.mkbauer.tinyscript.interpreter;

import java.util.List;

public class EmptyFunction extends BuiltinFunction {
	
	public EmptyFunction(ExecutionVisitor ev) {
		super(ev);
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
