package de.mkbauer.tinyscript.interpreter;

@FunctionalInterface
public interface BuiltinFunctionImplementation {
	
	public TSValue apply(TSObject self, TSValue[] args);

}
