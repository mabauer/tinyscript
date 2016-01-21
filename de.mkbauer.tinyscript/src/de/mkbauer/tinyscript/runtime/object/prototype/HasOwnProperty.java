package de.mkbauer.tinyscript.runtime.object.prototype;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class HasOwnProperty extends BuiltinFunction {
	
	private final static String NAME = "hasOwnProperty";
	
	public HasOwnProperty(ExecutionVisitor ev) {
		super(ev);
	}

	@Override
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
		checkArgs(args);
		return new TSValue(self.hasOwnProperty(args.get(0).asString()));
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
