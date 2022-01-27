package de.mkbauer.tinyscript.runtime.object.prototype;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class HasOwnProperty extends BuiltinFunction {
	
	private final static String NAME = "hasOwnProperty";
	
	public HasOwnProperty(TinyscriptEngine engine) {
		super(engine);
	}

	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		checkArgs(args);
		return new TSValue(self.hasOwnProperty(args[0].asString()));
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
