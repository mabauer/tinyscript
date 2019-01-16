package de.mkbauer.tinyscript.runtime.fs;

import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;

public class FSObject extends TSObject {

	public FSObject(ExecutionVisitor ev) {
		super(ev, ev.getDefaultPrototype());
		
		defineDefaultProperty(this, "readFile", new ReadFile(ev));
		defineDefaultProperty(this, "writeFile", new WriteFile(ev));
	}

}
