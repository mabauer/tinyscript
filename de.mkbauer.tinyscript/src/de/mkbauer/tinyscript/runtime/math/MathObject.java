package de.mkbauer.tinyscript.runtime.math;

import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSNumber;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

public class MathObject extends TSObject {

	public MathObject(TinyscriptEngine engine) {
		super(engine, engine.getDefaultPrototype());

		defineDefaultProperty("PI", java.lang.Math.PI);
		defineBuiltinMethod("sqrt", this::sqrt, 1);
		defineBuiltinMethod("round", this::round, 1);
		defineBuiltinMethod("random", this::random, 0);
		defineBuiltinMethod("min", this::min, 1);
		defineBuiltinMethod("max", this::max, 1);
		defineBuiltinMethod("abs", this::abs, 1);
		defineBuiltinMethod("floor", this::floor, 1);
		defineBuiltinMethod("ceil", this::ceil, 1);
	}

	// Like all builtins, Math does not convert its arguments: they must be numbers
	private static double number(TSValue value) {
		if (!(value instanceof TSNumber number))
			throw new TinyscriptTypeError("Math functions need numbers, not " + value.typeOf());
		return number.value();
	}

	private TSValue sqrt(TSObject self, TSValue[] args) {
		return TSValue.of(java.lang.Math.sqrt(number(args[0])));
	}

	private TSValue round(TSObject self, TSValue[] args) {
		return TSValue.of(java.lang.Math.round(number(args[0])));
	}

	private TSValue random(TSObject self, TSValue[] args) {
		return TSValue.of(java.lang.Math.random());
	}

	// Math.min(a, b, ...), Math.max(a, b, ...): like Javascript, any number of arguments
	private TSValue min(TSObject self, TSValue[] args) {
		double result = Double.POSITIVE_INFINITY;
		for (TSValue arg : args)
			result = java.lang.Math.min(result, number(arg));
		return TSValue.of(result);
	}

	private TSValue max(TSObject self, TSValue[] args) {
		double result = Double.NEGATIVE_INFINITY;
		for (TSValue arg : args)
			result = java.lang.Math.max(result, number(arg));
		return TSValue.of(result);
	}

	private TSValue abs(TSObject self, TSValue[] args) {
		return TSValue.of(java.lang.Math.abs(number(args[0])));
	}

	private TSValue floor(TSObject self, TSValue[] args) {
		return TSValue.of(java.lang.Math.floor(number(args[0])));
	}

	private TSValue ceil(TSObject self, TSValue[] args) {
		return TSValue.of(java.lang.Math.ceil(number(args[0])));
	}

}
