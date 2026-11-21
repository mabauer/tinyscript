package de.mkbauer.tinyscript.runtime.string;

import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.Display;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

public class StringObject extends BuiltinType {
	
	
	private String value;
	
	StringObject(TinyscriptEngine engine) {
		super(engine);
		setValue("");
		defineDefaultProperty("length", TSValue.of(0));
	}
	
	StringObject(TinyscriptEngine engine, TSValue value) {
		super(engine);
		if (value == null) {
			setValue("");
			defineDefaultProperty("length", TSValue.of(0));
		}
		else {
			setValue(value.asString());
			defineDefaultProperty("length", TSValue.of(getLength()));
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
	
	@Override
	protected String display(Display display) {
		return value;
	}
	

	/**
	 * Strings cannot be changed: neither characters nor other properties can be set.
	 */
	@Override
	public void put(String key, TSValue value) {
		throw new TinyscriptTypeError("Strings cannot be changed (setting '" + key + "')");
	}

	@Override
	public TSValue valueOf() {
		return TSValue.of(value);
	}

}
