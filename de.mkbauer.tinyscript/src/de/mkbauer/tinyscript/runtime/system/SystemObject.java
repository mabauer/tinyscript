package de.mkbauer.tinyscript.runtime.system;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.array.ArrayConstructor;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;

public class SystemObject extends TSObject {

	public SystemObject(TinyscriptEngine engine) {
		super(engine, engine.getDefaultPrototype());
		defineBuiltinMethod("currentTimeMillis", this::currentTimeMillis, 0);
		setArguments(List.of());
	}

	private TSValue currentTimeMillis(TSObject self, TSValue[] args) {
		return TSValue.of(java.lang.System.currentTimeMillis());
	}

	/**
	 * System.args: the arguments of the script, an array of strings.
	 */
	public void setArguments(List<String> arguments) {
		ArrayObject args = (ArrayObject) engine.getConstructor(ArrayConstructor.NAME).createObject();
		arguments.forEach(arg -> args.push(TSValue.of(arg)));
		defineDefaultProperty("args", args);
	}

}
