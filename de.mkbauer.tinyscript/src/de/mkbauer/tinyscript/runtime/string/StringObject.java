package de.mkbauer.tinyscript.runtime.string;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class StringObject extends BuiltinType {
	
	private static final String CONSTRUCTOR = "String";
	
	private String value;
	
	public StringObject(ExecutionVisitor ev) {
		super(ev);
		setValue(null);
		defineDefaultProperty(this,"length", new TSValue(0));
	}
	
	public StringObject(ExecutionVisitor ev, TSValue value) {
		super(ev);
		setValue(value.asString());
		defineDefaultProperty(this,"length", new TSValue(getLength()));
	}
	
	private void defineStringPrototype() {
		// TODO Add other String methods
		defineDefaultProperty(getPrototype(), "indexOf", new IndexOf(ev));
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
