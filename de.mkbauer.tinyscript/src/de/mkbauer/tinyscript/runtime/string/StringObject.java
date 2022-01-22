package de.mkbauer.tinyscript.runtime.string;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.string.prototype.IndexOf;

public class StringObject extends BuiltinType {
	
	private static final String CONSTRUCTOR = "String";
	
	private String value;
	
	public StringObject(TinyscriptEngine engine) {
		super(engine);
		setValue(null);
		defineDefaultProperty("length", new TSValue(0));
	}
	
	public StringObject(TinyscriptEngine engine, TSValue value) {
		super(engine);
		setValue(value.asString());
			defineDefaultProperty("length", new TSValue(getLength()));
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

	@Override
	public String getConstructorName() {
		return CONSTRUCTOR;
	}

}
