package de.mkbauer.tinyscript.runtime.array;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class IsArray extends BuiltinFunction {
	
	private final static String NAME = "isArray";

	public IsArray(ExecutionVisitor ev) {
		super(ev);
	}
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self,
			List<TSValue> args) {
		checkArgs(args);
		TSValue object = args.get(0);
		if (object.isObject() && object.asObject() instanceof ArrayObject) 
			return new TSValue(true);
		else 
			return new TSValue(false);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		return 1;
	}


}
