package de.mkbauer.tinyscript.runtime.string;

import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.GlobalExecutionContext;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.object.HasOwnProperty;
import de.mkbauer.tinyscript.runtime.object.ToString;

public class StringObject extends Function {
	
	private String value;
	


	public StringObject(GlobalExecutionContext globalContext) {
		super(globalContext);
		setValue(null);
		defineDefaultProperty(this,"length", new TSValue(0));
		defineStringPrototype();
	}
	
	public StringObject(GlobalExecutionContext globalContext, TSValue value) {
		super(globalContext);
		setValue(value.asString());
		defineDefaultProperty(this,"length", new TSValue(getLength()));
		defineStringPrototype();
	}
	
	private void defineStringPrototype() {
		// TODO
		defineDefaultProperty(getPrototype(), "indexOf", new IndexOf(globalContext));
	}
	

	@Override
	public String getName() {
		return "String";
	}

	@Override
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
	

}
