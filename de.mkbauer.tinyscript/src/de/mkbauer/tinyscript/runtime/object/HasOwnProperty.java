package de.mkbauer.tinyscript.runtime.object;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.TSBuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class HasOwnProperty extends TSBuiltinFunction {

	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
		checkArgs(args);
		return new TSValue(self.hasOwnProperty(args.get(0).asString()));
	}

	@Override
	public int getLength() {
		return 1;
	}
	
	

}
