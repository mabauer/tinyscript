package de.mkbauer.tinyscript.runtime.math;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.GlobalExecutionContext;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class Sqrt extends BuiltinFunction {
	
	public Sqrt(ExecutionVisitor ev) {
		super(ev);
	}

	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
		checkArgs(args);
		return new TSValue(java.lang.Math.sqrt(args.get(0).asDouble()));
	}

	@Override
	public int getLength() {
		return 1;
	}
	
	

}
