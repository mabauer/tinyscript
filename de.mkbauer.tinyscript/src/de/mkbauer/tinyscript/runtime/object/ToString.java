package de.mkbauer.tinyscript.runtime.object;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.GlobalExecutionContext;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class ToString extends BuiltinFunction {
	
	public ToString(GlobalExecutionContext globalContext) {
		super(globalContext);
	}

	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
		return new TSValue(self.toString());
	}

}
