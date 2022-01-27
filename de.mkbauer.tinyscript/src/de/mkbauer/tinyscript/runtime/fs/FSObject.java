package de.mkbauer.tinyscript.runtime.fs;

import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;

public class FSObject extends TSObject {

	public FSObject(TinyscriptEngine engine) {
		super(engine, engine.getDefaultPrototype());
		
		defineDefaultProperty("readFile", new ReadFile(engine));
		defineDefaultProperty("writeFile", new WriteFile(engine));
	}

}
