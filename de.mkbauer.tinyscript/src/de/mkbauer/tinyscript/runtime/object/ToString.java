package de.mkbauer.tinyscript.runtime.object;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.TSBuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class ToString extends TSBuiltinFunction {

	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
		return new TSValue(self.toString());
	}

}
