package de.mkbauer.tinyscript.interpreter;

import java.util.List;

@FunctionalInterface
public interface BuiltinFunctionImplementation {
	
	public TSValue apply(TSObject self, List<TSValue> args);

}
