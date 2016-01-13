package de.mkbauer.tinyscript.interpreter;

import java.util.List;

public abstract class BuiltinConstructor extends BuiltinFunction {

	public BuiltinConstructor(ExecutionVisitor ev) {
		super(ev);
		
		TSValue prototype = ev.getObjectPrototypeFor(getName());
		if (prototype == TSValue.UNDEFINED) {
			setPrototypeProperty(new TSObject(ev.getDefaultPrototype()));
		}
	}

}
