package de.mkbauer.tinyscript.runtime.string;

import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class StringObject extends BuiltinType {
	
	
	private String value;
	
	StringObject(TinyscriptEngine engine) {
		super(engine);
		setValue(null);
		defineDefaultProperty("length", new TSValue(0));
	}
	
	StringObject(TinyscriptEngine engine, TSValue value) {
		super(engine);
		if (value == null) {
			setValue(null);
			defineDefaultProperty("length", new TSValue(0));
		}
		else {
			setValue(value.asString());
			defineDefaultProperty("length", new TSValue(getLength()));
		}
	}
	
	public int getLength() {
		return value.length();
	}
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {	
		this.value = value;
	}
	
	public String toString() {
		return value;
	}
	

	@Override
	public TSValue valueOf() {
		return new TSValue(value);
	}

}
