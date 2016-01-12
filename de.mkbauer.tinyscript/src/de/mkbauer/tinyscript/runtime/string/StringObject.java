package de.mkbauer.tinyscript.runtime.string;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class StringObject extends BuiltinType {
	
	private String value;
	


	public StringObject(ExecutionVisitor ev) {
		super(ev);
		setValue(null);
		defineDefaultProperty(this,"length", new TSValue(0));
		defineStringPrototype();
	}
	
	public StringObject(ExecutionVisitor ev, TSValue value) {
		super(ev);
		setValue(value.asString());
		defineDefaultProperty(this,"length", new TSValue(getLength()));
		defineStringPrototype();
	}
	
	private void defineStringPrototype() {
		// TODO
		defineDefaultProperty(getPrototype(), "indexOf", new IndexOf(ev));
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

	@Override
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
		// TODO Auto-generated method stub
		return null;
	}
	

}
