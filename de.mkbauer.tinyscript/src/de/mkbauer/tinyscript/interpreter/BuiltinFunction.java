package de.mkbauer.tinyscript.interpreter;

import java.util.List;

public abstract class BuiltinFunction extends Function {
	
	public BuiltinFunction(TinyscriptEngine engine) {
		super(engine);
		
	}
	
	public void checkArgs(List <TSValue> args) {
		if (args.size() < getLength()) {
			throw new TinyscriptArgumentError("Builtin function " + getName() + " requires at least " + getLength() + " arguments.", 
					engine.getCurrentContext().currentExpression);
		}
	}
	
	public void enforceSandboxing() {
		if (engine.isSandboxed()) {
			throw new TinyscriptUnsupportedOperationError("Builtin function " + getName() + " is not available when running sandboxed.",
					engine.getCurrentContext().currentExpression);
		}
	}

}
