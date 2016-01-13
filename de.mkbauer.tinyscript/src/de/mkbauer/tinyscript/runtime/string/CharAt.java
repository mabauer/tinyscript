package de.mkbauer.tinyscript.runtime.string;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class CharAt extends BuiltinFunction {

	private static final String NAME = "charAt";
	
	public CharAt(ExecutionVisitor ev) {
		super(ev);
	}

	@Override
	public TSValue apply(boolean asConstructor, TSObject self,
			List<TSValue> args) {
		checkArgs(args);
		String result = "";
		if (self instanceof StringObject) {
			String str = TSObject.toString(ev, new TSValue(self));
			int pos = TSObject.toInteger(ev, args.get(0)); 
			if (pos >=0 && pos < str.length()) {
				result = str.substring(pos, pos+1);
			} 
		}
		return new TSValue(result);
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
