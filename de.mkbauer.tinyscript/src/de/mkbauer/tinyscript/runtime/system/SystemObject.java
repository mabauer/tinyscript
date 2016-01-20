package de.mkbauer.tinyscript.runtime.system;

import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.runtime.system.CurrentTimeMillis;

public class SystemObject extends TSObject {

	public SystemObject(ExecutionVisitor ev) {
		super(ev, ev.getDefaultPrototype());
		
		defineDefaultProperty(this, "currentTimeMillis", new CurrentTimeMillis(ev));
	}

}
