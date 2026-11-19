package de.mkbauer.tinyscript.runtime.math;

import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class MathObject extends TSObject {

	public MathObject(TinyscriptEngine engine) {
		super(engine, engine.getDefaultPrototype());

		defineDefaultProperty("PI", java.lang.Math.PI);
		defineBuiltinMethod("sqrt", this::sqrt, 1);
		defineBuiltinMethod("round", this::round, 1);
		defineBuiltinMethod("random", this::random, 0);
	}

	private TSValue sqrt(TSObject self, TSValue[] args) {
		return TSValue.of(java.lang.Math.sqrt(TSObject.toNumber(engine, args[0])));
	}

	private TSValue round(TSObject self, TSValue[] args) {
		return TSValue.of(java.lang.Math.round(TSObject.toNumber(engine, args[0])));
	}

	private TSValue random(TSObject self, TSValue[] args) {
		return TSValue.of(java.lang.Math.random());
	}

}
