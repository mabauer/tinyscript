package de.mkbauer.tinyscript.runtime.function;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.GlobalExecutionContext;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class FunctionObject extends BuiltinType {
	
	public FunctionObject(ExecutionVisitor ev) {
		super(ev);
		
		// Property: prototype
		setPrototypeProperty(getPrototype());
	}
	
	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String getName() {
		return "Function";
	}
	
	public int getLength() {
		// TODO: Find out why?
		return 1;
	}



}
