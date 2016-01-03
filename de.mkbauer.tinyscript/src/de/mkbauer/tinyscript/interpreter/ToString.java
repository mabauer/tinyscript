package de.mkbauer.tinyscript.interpreter;

import java.util.List;

public class ToString extends TSBuiltinFunction {

	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
		return new TSValue(self.toString());
	}

}
