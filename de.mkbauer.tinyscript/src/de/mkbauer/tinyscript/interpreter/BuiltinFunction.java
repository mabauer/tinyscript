package de.mkbauer.tinyscript.interpreter;

import java.util.List;

public abstract class BuiltinFunction extends Function {
	
	public BuiltinFunction(ExecutionVisitor ev) {
		super(ev);
		
	}

	
	public void checkArgs(List <TSValue> args) {
		if (args.size() < getLength()) {
			throw new TinyscriptArgumentError("Builtin function " + getName() + " requires at least " + getLength() + " arguments.");
		}
	}

}
