package de.mkbauer.tinyscript.runtime.array;

import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
	

public class Shift extends BuiltinFunction {
	
	private final static String NAME = "shift";

	public Shift(ExecutionVisitor ev) {
		super(ev);
	}
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self,
			List<TSValue> args) {
		if (!(self instanceof ArrayObject))
			throw new TinyscriptTypeError("Function Array.prototype.shift only works for Array objects.");
		ArrayObject arr = (ArrayObject) self;
		TSValue first = arr.shift();
		return first; 
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		return 0;
	}

}