package de.mkbauer.tinyscript.interpreter;

import java.util.List;

public abstract class BuiltinType extends BuiltinFunction {

	public BuiltinType(ExecutionVisitor ev) {
		super(ev);
		
		TSValue prototype = ev.getObjectPrototypeFor(getName());
		if (prototype == TSValue.UNDEFINED) {
			setPrototypeProperty(new TSObject(ev.getDefaultPrototype()));
		}
	}
	


}
