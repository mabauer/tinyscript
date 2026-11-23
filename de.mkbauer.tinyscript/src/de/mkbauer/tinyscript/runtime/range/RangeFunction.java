package de.mkbauer.tinyscript.runtime.range;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.array.ArrayConstructor;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;

/**
 * The global function range(end) / range(start, end[, step]) (decided 2026-11-20): like
 * Python's range, a lazy range of integers with an exclusive end, e.g. for counting in the
 * iterable for loop, for (const i : range(n)). Its methods: length, toArray(), map(f), forEach(f),
 * reverse() (lazy as well: for (const i : range(n).reverse()) counts down from n - 1 to 0).
 */
public final class RangeFunction {

	private RangeFunction() {
	}

	public static BuiltinFunction create(TinyscriptEngine engine) {
		TSObject prototype = new TSObject(engine, engine.getDefaultPrototype());
		prototype.defineBuiltinMethod("toArray", (self, args) -> toArray(engine, thisRange(self, "toArray")), 0);
		prototype.defineBuiltinMethod("map", (self, args) -> map(engine, thisRange(self, "map"), callback(args[0], "map")), 1);
		prototype.defineBuiltinMethod("forEach", (self, args) -> forEach(thisRange(self, "forEach"), callback(args[0], "forEach")), 1);
		prototype.defineBuiltinMethod("reverse", (self, args) -> reverse(engine, prototype, thisRange(self, "reverse")), 0);
		BuiltinFunction range = engine.defineBuiltinFunction("range", (self, args) -> range(engine, prototype, args), 1, 3);
		// range(3).constructor is range (messages name the type as "range", see TinyscriptEngine.typeName)
		prototype.defineDefaultProperty("constructor", range);
		return range;
	}

	private static TSValue range(TinyscriptEngine engine, TSObject prototype, TSValue[] args) {
		for (TSValue arg : args) {
			if (!arg.isMathematicalInteger())
				throw new TinyscriptTypeError("range() needs integers, not " + (arg.isNumber() ? arg.asString() : arg.typeOf()));
		}
		double start = args.length > 1 ? args[0].asDouble() : 0;
		double end = args.length > 1 ? args[1].asDouble() : args[0].asDouble();
		double step = args.length > 2 ? args[2].asDouble() : 1;
		if (step == 0)
			throw new TinyscriptTypeError("range() needs a step other than 0");
		return new RangeObject(engine, prototype, start, end, step);
	}

	// The same integers from the last one back to start, again lazy: range(5).reverse() is
	// range(4, -1, -1), range(1, 10, 3).reverse() is range(7, -2, -3); an empty range stays empty
	private static RangeObject reverse(TinyscriptEngine engine, TSObject prototype, RangeObject range) {
		double last = range.getStart() + (range.length() - 1) * range.getStep();
		return new RangeObject(engine, prototype, last, range.getStart() - range.getStep(), -range.getStep());
	}

	private static ArrayObject toArray(TinyscriptEngine engine, RangeObject range) {
		ArrayObject result = (ArrayObject) engine.getConstructor(ArrayConstructor.NAME).createObject();
		for (double i = range.getStart(); range.contains(i); i += range.getStep())
			result.push(TSValue.of(i));
		return result;
	}

	// range.map(f): an array of f(i) for each i, called with (i, index, range) like the callbacks of arrays
	private static ArrayObject map(TinyscriptEngine engine, RangeObject range, Function function) {
		ArrayObject result = (ArrayObject) engine.getConstructor(ArrayConstructor.NAME).createObject();
		int index = 0;
		for (double i = range.getStart(); range.contains(i); i += range.getStep())
			result.push(function.apply(null, new TSValue[] { TSValue.of(i), TSValue.of(index++), range }));
		return result;
	}

	private static TSValue forEach(RangeObject range, Function function) {
		int index = 0;
		for (double i = range.getStart(); range.contains(i); i += range.getStep())
			function.apply(null, new TSValue[] { TSValue.of(i), TSValue.of(index++), range });
		return TSValue.UNDEFINED;
	}

	private static RangeObject thisRange(TSObject self, String method) {
		if (!(self instanceof RangeObject range))
			throw new TinyscriptTypeError("Function range." + method + " only works for ranges.");
		return range;
	}

	private static Function callback(TSValue value, String method) {
		if (!(value instanceof Function function))
			throw new TinyscriptTypeError(method + "() needs a function, not " + value.typeOf());
		return function;
	}

}
