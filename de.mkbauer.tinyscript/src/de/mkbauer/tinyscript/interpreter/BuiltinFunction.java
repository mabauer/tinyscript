package de.mkbauer.tinyscript.interpreter;

import java.util.List;

public abstract class BuiltinFunction extends FunctionObject {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLength() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void checkArgs(List <TSValue> args) {
		if (args.size() < getLength()) {
			throw new TinyscriptArgumentError("Builtin function " + getName() + " requires at least " + getLength() + " arguments.");
		}
	}

	public abstract TSValue apply(TSObject self, List<TSValue> args);

}
