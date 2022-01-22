package de.mkbauer.tinyscript.runtime.object.prototype;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class ToString extends BuiltinFunction {
	
	private final static String NAME = "toString";
	
	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
		String name = "Object";
		
		TSValue ctorValue = self.get("constructor");
		if (ctorValue.isObject()) {
			TSObject ctor = ctorValue.asObject();
			if (ctor instanceof Function) {
				name = ((Function) ctor).getName();
			}
		}
		String result = "[object " + name + "]";
		return new TSValue(result);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		return 0;
	}

	public ToString(TinyscriptEngine engine) {
		super(engine);
	}

}
