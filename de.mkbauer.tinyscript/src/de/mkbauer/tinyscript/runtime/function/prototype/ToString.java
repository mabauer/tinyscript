package de.mkbauer.tinyscript.runtime.function.prototype;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.InterpretedFunction;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

public class ToString extends BuiltinFunction {
	
	private final static String NAME = "toString";
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self,
			List<TSValue> args) {
		if (!(self instanceof Function))
			throw new TinyscriptTypeError("Function Function.prototype.toString only works for Function objects.");
		String result;
		if (self instanceof InterpretedFunction) {
			InterpretedFunction function = (InterpretedFunction) self;
			result = function.getCodeAsString();
		}
		else {
			Function function = (Function) self;
			result = "function " + function.getName() + "() + { // builtin }";
		}
		return new TSValue(result);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		return 0;
	}

	public ToString(ExecutionVisitor ev) {
		super(ev);
	}

}
