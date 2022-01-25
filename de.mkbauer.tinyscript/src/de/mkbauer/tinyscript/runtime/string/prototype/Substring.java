package de.mkbauer.tinyscript.runtime.string.prototype;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.string.StringObject;

public class Substring extends BuiltinFunction {
	
	private static final String NAME = "substring";

	public Substring(TinyscriptEngine engine) {
		super(engine);
	}

	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		checkArgs(args);
		String result = "";
		if (self instanceof StringObject) {
			String str = TSObject.toString(engine, new TSValue(self));
			int start = TSObject.toInteger(engine, args[0]); 
			int end = str.length();
			if (args.length > 1) {
				end = TSObject.toInteger(engine, args[1]); 
			}
			if (start < 0)
				start = 0;
			if (start >= str.length())
				start = str.length();
			if (end < 0)
				end = 0;
			if (end >= str.length()) 
				end = str.length();
			if (start <= end) 
				result = str.substring(start, end);
			else
				result = str.substring(end, start);
		}
		engine.recordStringCreation(result);
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
