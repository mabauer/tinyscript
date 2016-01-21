package de.mkbauer.tinyscript.runtime.string.prototype;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.string.StringObject;

public class IndexOf extends BuiltinFunction {
	
	private final static String NAME = "indexOf";
	
	public IndexOf(ExecutionVisitor ev) {
		super(ev);
	}

	@Override
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
		checkArgs(args);
		// TODO: Should try to convert to string (using asString())
		if (self instanceof StringObject) {
			String search = args.get(0).asString(); 
			String value = ((StringObject) self).getValue();
			int result = value.indexOf(search);
			return new TSValue(result);
		}
		return new TSValue(-1);
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
