package de.mkbauer.tinyscript.runtime.system;

import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;

public class SystemObject extends TSObject {

	public SystemObject(TinyscriptEngine engine) {
		super(engine, engine.getDefaultPrototype());
		
		defineDefaultProperty(this, "currentTimeMillis", new CurrentTimeMillis(engine));
	}

}
