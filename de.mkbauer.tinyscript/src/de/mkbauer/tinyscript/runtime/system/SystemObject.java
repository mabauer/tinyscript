package de.mkbauer.tinyscript.runtime.system;

import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class SystemObject extends TSObject {

	public SystemObject(TinyscriptEngine engine) {
		super(engine, engine.getDefaultPrototype());
		defineBuiltinMethod("currentTimeMillis", this::currentTimeMillis, 0);
	}

	private TSValue currentTimeMillis(TSObject self, TSValue[] args) {
		return new TSValue(java.lang.System.currentTimeMillis());
	}

}
