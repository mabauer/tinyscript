package de.mkbauer.tinyscript.runtime.math;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSNumber;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

public class MathObject extends TSObject {

	public MathObject(TinyscriptEngine engine) {
		super(engine, engine.getDefaultPrototype());

		// Like Javascript: the constants cannot be assigned (the methods can)
		defineConstant("PI", java.lang.Math.PI);
		defineConstant("E", java.lang.Math.E);
		defineBuiltinMethod("sqrt", this::sqrt, 1);
		defineBuiltinMethod("round", this::round, 1);
		defineBuiltinMethod("random", this::random, 0);
		defineBuiltinMethod("min", this::min, 0, BuiltinFunction.VARIADIC);
		defineBuiltinMethod("max", this::max, 0, BuiltinFunction.VARIADIC);
		defineBuiltinMethod("abs", this::abs, 1);
		defineBuiltinMethod("floor", this::floor, 1);
		defineBuiltinMethod("ceil", this::ceil, 1);
		defineBuiltinMethod("trunc", (self, args) -> TSValue.of(trunc(number(args[0]))), 1);
		defineBuiltinMethod("sign", (self, args) -> TSValue.of(Math.signum(number(args[0]))), 1);
		defineBuiltinMethod("cbrt", (self, args) -> TSValue.of(FreeBsdMath.cbrt(number(args[0]))), 1);
		defineBuiltinMethod("pow", (self, args) -> TSValue.of(pow(number(args[0]), number(args[1]))), 2);
		defineBuiltinMethod("exp", (self, args) -> TSValue.of(StrictMath.exp(number(args[0]))), 1);
		defineBuiltinMethod("log", (self, args) -> TSValue.of(StrictMath.log(number(args[0]))), 1);
		defineBuiltinMethod("log2", (self, args) -> TSValue.of(FreeBsdMath.log2(number(args[0]))), 1);
		defineBuiltinMethod("log10", (self, args) -> TSValue.of(StrictMath.log10(number(args[0]))), 1);
		defineBuiltinMethod("hypot", this::hypot, 0, BuiltinFunction.VARIADIC);
		defineBuiltinMethod("sin", (self, args) -> TSValue.of(StrictMath.sin(number(args[0]))), 1);
		defineBuiltinMethod("cos", (self, args) -> TSValue.of(StrictMath.cos(number(args[0]))), 1);
		defineBuiltinMethod("tan", (self, args) -> TSValue.of(StrictMath.tan(number(args[0]))), 1);
		defineBuiltinMethod("asin", (self, args) -> TSValue.of(StrictMath.asin(number(args[0]))), 1);
		defineBuiltinMethod("acos", (self, args) -> TSValue.of(StrictMath.acos(number(args[0]))), 1);
		defineBuiltinMethod("atan", (self, args) -> TSValue.of(StrictMath.atan(number(args[0]))), 1);
		defineBuiltinMethod("atan2", (self, args) -> TSValue.of(StrictMath.atan2(number(args[0]), number(args[1]))), 2);
	}

	// The transcendental functions use StrictMath (fdlibm), the algorithms Javascript engines such
	// as V8 use as well; log2 and cbrt are ports of the FreeBSD versions V8 uses (FreeBsdMath).
	// Compared with node on 20,000 inputs (2026-11-21): log2 and cbrt agree to the last bit; sin,
	// cos, tan, asin, acos, atan, atan2, exp and log in more than 99 % of the cases, pow in about
	// 92 %; where they differ, it is by one unit in the last digit.

	// Towards zero: trunc(-2.5) is -2, trunc(-0.5) is -0
	private static double trunc(double x) {
		return x < 0 ? Math.ceil(x) : Math.floor(x);
	}

	// Like Javascript: 1 ** NaN and (-1) ** Infinity are NaN (Java: 1.0)
	private static double pow(double base, double exponent) {
		if (Double.isNaN(exponent) || (Math.abs(base) == 1 && Double.isInfinite(exponent)))
			return Double.NaN;
		return StrictMath.pow(base, exponent);
	}

	// Math.hypot(a, b, ...): the square root of the sum of squares, scaled and compensated as V8
	// does it (no overflow for large numbers); Infinity wins over NaN
	private TSValue hypot(TSObject self, TSValue[] args) {
		double max = 0;
		boolean nan = false;
		double[] values = new double[args.length];
		for (int i = 0; i < args.length; i++) {
			values[i] = Math.abs(number(args[i]));
			if (Double.isInfinite(values[i]))
				return TSValue.of(Double.POSITIVE_INFINITY);
			nan |= Double.isNaN(values[i]);
			max = Math.max(max, values[i]);
		}
		if (nan)
			return TSValue.NAN;
		if (max == 0)
			return TSValue.of(0);
		double sum = 0;
		double compensation = 0;
		for (double value : values) {
			double n = value / max;
			double summand = n * n - compensation;
			double preliminary = sum + summand;
			compensation = (preliminary - sum) - summand;
			sum = preliminary;
		}
		return TSValue.of(Math.sqrt(sum) * max);
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

	// Like Javascript: to the nearest integer, halves up (round(-2.5) is -2); NaN, infinite and
	// large numbers stay as they are (Java's Math.round returns a long: 1e20 became 2^63 - 1)
	private TSValue round(TSObject self, TSValue[] args) {
		double x = number(args[0]);
		if (Double.isNaN(x) || Double.isInfinite(x) || Math.abs(x) >= 0x1p52)
			return TSValue.of(x);
		double floor = Math.floor(x);
		double result = x - floor >= 0.5 ? floor + 1 : floor;
		// -0.5 <= x < 0 rounds to -0, as in Javascript
		return TSValue.of(result == 0 && (x < 0 || 1 / x < 0) ? -0.0 : result);
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
