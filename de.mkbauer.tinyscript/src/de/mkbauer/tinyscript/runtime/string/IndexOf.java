package de.mkbauer.tinyscript.runtime.string;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.GlobalExecutionContext;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class IndexOf extends BuiltinFunction {
	
	public IndexOf(GlobalExecutionContext globalContext) {
		super(globalContext);
	}

	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
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
		return "toString";
	}

	@Override
	public int getLength() {
		return 1;
	}
	
	

}
