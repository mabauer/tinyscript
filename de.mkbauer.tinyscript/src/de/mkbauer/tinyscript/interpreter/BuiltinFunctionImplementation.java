package de.mkbauer.tinyscript.interpreter;

import java.util.List;

public interface BuiltinFunctionImplementation {
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args);
}
