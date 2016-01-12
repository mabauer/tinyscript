package de.mkbauer.tinyscript.runtime.object;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class ToString extends BuiltinFunction {
	
	private final static String NAME = "toString";
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self,
			List<TSValue> args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	public ToString(ExecutionVisitor ev) {
		super(ev);
	}

}
