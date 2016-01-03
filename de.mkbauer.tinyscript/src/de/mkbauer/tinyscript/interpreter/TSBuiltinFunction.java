package de.mkbauer.tinyscript.interpreter;

import java.util.List;

public abstract class TSBuiltinFunction extends TSAbstractFunction {

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

	public abstract TSValue apply(TSObject self, List<TSValue> args);

}
