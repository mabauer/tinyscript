package de.mkbauer.tinyscript.runtime.array;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSNumber;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSString;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class ArrayConstructor extends BuiltinConstructor {

	public static final String NAME = "Array";

	public ArrayConstructor(TinyscriptEngine engine) {
		super(engine);
		defineBuiltinMethod("isArray", this::isArray, 1);

		TSObject arrayPrototype = getPrototypeProperty().asObject();
		arrayPrototype.defineBuiltinMethod("toString", this::arrayToString, 0);
		arrayPrototype.defineBuiltinMethod("at", this::at, 1);
		arrayPrototype.defineBuiltinMethod("indexOf", this::indexOf, 1);
		arrayPrototype.defineBuiltinMethod("push", this::push, 1);
		arrayPrototype.defineBuiltinMethod("pop", this::pop, 0);
		arrayPrototype.defineBuiltinMethod("unshift", this::unshift, 1);
		arrayPrototype.defineBuiltinMethod("shift", this::shift, 0);
		arrayPrototype.defineBuiltinMethod("map", this::map, 1);
		arrayPrototype.defineBuiltinMethod("filter", this::filter, 1);
		arrayPrototype.defineBuiltinMethod("join", this::join, 0);
		arrayPrototype.defineBuiltinMethod("forEach", this::forEach, 1);
		arrayPrototype.defineBuiltinMethod("reduce", this::reduce, 1);
		arrayPrototype.defineBuiltinMethod("some", this::some, 1);
		arrayPrototype.defineBuiltinMethod("every", this::every, 1);
		arrayPrototype.defineBuiltinMethod("find", this::find, 1);
		arrayPrototype.defineBuiltinMethod("findIndex", this::findIndex, 1);
		arrayPrototype.defineBuiltinMethod("includes", this::includes, 1);
		arrayPrototype.defineBuiltinMethod("slice", this::slice, 0);
		arrayPrototype.defineBuiltinMethod("fill", this::fill, 1);
		arrayPrototype.defineBuiltinMethod("sort", this::sort, 0);
	}

	// new Array(length) or new Array(item, ...); like Javascript, Array(...) without new works the same
	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		ArrayObject arr = self instanceof ArrayObject a ? a : (ArrayObject) createObject();
		if (args.length == 1 && args[0] instanceof TSNumber)
			arr.put("length", args[0]);
		else
			for (TSValue arg : args)
				arr.push(arg);
		return arr;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		return 0;
	}

	@Override
	public TSObject createObject() {
		TSObject newObject = new ArrayObject(engine);
		newObject.setPrototype(getPrototypeProperty().asObject());
		newObject.defineDefaultProperty("constructor", this);
		return newObject;
	}

	// Array.isArray(value)
	private TSValue isArray(TSObject self, TSValue[] args) {
		return TSValue.of(args[0] instanceof ArrayObject);
	}

	// Array.prototype methods

	private TSValue arrayToString(TSObject self, TSValue[] args) {
		String result = thisArray(self, "toString").getItems().stream()
				.map(item -> TSObject.toString(engine, item))
				.collect(Collectors.joining(", "));
		engine.recordStringCreation(result);
		return TSValue.of(result);
	}

	private TSValue push(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "push");
		for (TSValue arg : args)
			arr.push(arg);
		return TSValue.of(arr.getLength());
	}

	private TSValue pop(TSObject self, TSValue[] args) {
		return thisArray(self, "pop").pop();
	}

	private TSValue unshift(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "unshift");
		for (TSValue arg : args)
			arr.unshift(arg);
		return TSValue.of(arr.getLength());
	}

	private TSValue shift(TSObject self, TSValue[] args) {
		return thisArray(self, "shift").shift();
	}

	// The callbacks of map, filter, forEach, some, every, find and findIndex are called with
	// (item, index, array). Like Javascript, they see the items up to the length the array had
	// when the method was called.
	private TSValue map(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "map");
		Function function = callback(args[0], "map");
		ArrayObject result = (ArrayObject) createObject();
		for (int i = 0, n = arr.getLength(); i < n; i++)
			result.push(call(function, arr, i));
		return result;
	}

	private TSValue filter(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "filter");
		Function function = callback(args[0], "filter");
		ArrayObject result = (ArrayObject) createObject();
		for (int i = 0, n = arr.getLength(); i < n; i++) {
			TSValue item = arr.item(i);
			if (call(function, arr, i).asCondition())
				result.push(item);
		}
		return result;
	}

	private TSValue forEach(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "forEach");
		Function function = callback(args[0], "forEach");
		for (int i = 0, n = arr.getLength(); i < n; i++)
			call(function, arr, i);
		return TSValue.UNDEFINED;
	}

	private TSValue some(TSObject self, TSValue[] args) {
		return TSValue.of(findIndex(thisArray(self, "some"), callback(args[0], "some")) >= 0);
	}

	private TSValue every(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "every");
		Function function = callback(args[0], "every");
		for (int i = 0, n = arr.getLength(); i < n; i++) {
			if (!call(function, arr, i).asCondition())
				return TSValue.of(false);
		}
		return TSValue.of(true);
	}

	private TSValue find(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "find");
		int index = findIndex(arr, callback(args[0], "find"));
		return index >= 0 ? arr.item(index) : TSValue.UNDEFINED;
	}

	private TSValue findIndex(TSObject self, TSValue[] args) {
		return TSValue.of(findIndex(thisArray(self, "findIndex"), callback(args[0], "findIndex")));
	}

	private static int findIndex(ArrayObject arr, Function function) {
		for (int i = 0, n = arr.getLength(); i < n; i++) {
			if (call(function, arr, i).asCondition())
				return i;
		}
		return -1;
	}

	// Array.prototype.reduce(function[, initial]): function(accumulator, item, index, array);
	// without initial, the first item is the start value, so an empty array is a TypeError
	private TSValue reduce(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "reduce");
		Function function = callback(args[0], "reduce");
		int n = arr.getLength();
		int i = 0;
		TSValue accumulator;
		if (args.length > 1)
			accumulator = args[1];
		else if (n > 0)
			accumulator = arr.item(i++);
		else
			throw new TinyscriptTypeError("reduce() of an empty array needs an initial value");
		for (; i < n; i++)
			accumulator = function.apply(null, new TSValue[] { accumulator, arr.item(i), TSValue.of(i), arr });
		return accumulator;
	}

	// Array.prototype.includes(value): whether an item == value (tinyscript's ==)
	private TSValue includes(TSObject self, TSValue[] args) {
		return TSValue.of(thisArray(self, "includes").getItems().stream().anyMatch(item -> item.tinyscriptEquals(args[0])));
	}

	// Array.prototype.slice([start[, end]]): a copy of the items from start to end;
	// negative indexes count from the end
	private TSValue slice(TSObject self, TSValue[] args) {
		List<TSValue> items = thisArray(self, "slice").getItems();
		int start = args.length > 0 ? sliceIndex(args[0], items.size(), "slice") : 0;
		int end = args.length > 1 ? sliceIndex(args[1], items.size(), "slice") : items.size();
		ArrayObject result = (ArrayObject) createObject();
		for (int i = start; i < end; i++)
			result.push(items.get(i));
		return result;
	}

	// Array.prototype.fill(value[, start[, end]]): sets the items from start to end to value
	private TSValue fill(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "fill");
		int start = args.length > 1 ? sliceIndex(args[1], arr.getLength(), "fill") : 0;
		int end = args.length > 2 ? sliceIndex(args[2], arr.getLength(), "fill") : arr.getLength();
		for (int i = start; i < end; i++)
			arr.setItem(i, args[0]);
		return arr;
	}

	private static int sliceIndex(TSValue value, int length, String function) {
		if (!value.isMathematicalInteger())
			throw new TinyscriptTypeError(function + "() needs integer indexes, not " + (value.isNumber() ? value.asString() : value.typeOf()));
		double index = value.asDouble();
		return (int) (index < 0 ? Math.max(0, length + index) : Math.min(index, length));
	}

	// Array.prototype.sort([compare]): sorts in place (stable) and returns the array. compare(a, b)
	// returns a number: negative if a comes first. Without compare only strings are sorted; unlike
	// Javascript, numbers are not sorted as strings ([10, 9, 1].sort() is a TypeError).
	private TSValue sort(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "sort");
		List<TSValue> sorted = new ArrayList<>(arr.getItems());
		if (args.length == 0 || args[0] == TSValue.UNDEFINED) {
			for (TSValue item : sorted) {
				if (!(item instanceof TSString))
					throw new TinyscriptTypeError("sort() without a comparison function only sorts strings, not " + item.typeOf());
			}
			sorted.sort((a, b) -> a.asString().compareTo(b.asString()));
		}
		else {
			Function compare = callback(args[0], "sort");
			try {
				sorted.sort((a, b) -> {
					if (!(compare.apply(null, new TSValue[] { a, b }) instanceof TSNumber result))
						throw new TinyscriptTypeError("The comparison function of sort() must return a number");
					return Double.isNaN(result.value()) ? 0 : (int) Math.signum(result.value());
				});
			}
			catch (IllegalArgumentException e) {
				// Java's sort detected that the comparison function contradicts itself
				throw new TinyscriptTypeError("The comparison function of sort() is inconsistent");
			}
		}
		// Sorted on a copy, so a comparison function that changes the array cannot break the sort
		for (int i = 0; i < sorted.size(); i++)
			arr.setItem(i, sorted.get(i));
		return arr;
	}

	// Array.prototype.join([separator]): like Javascript, the separator is "," by default
	private TSValue join(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "join");
		String separator = ",";
		if (args.length > 0 && args[0] != TSValue.UNDEFINED) {
			if (!(args[0] instanceof TSString s))
				throw new TinyscriptTypeError("join() needs a string as separator, not " + args[0].typeOf());
			separator = s.value();
		}
		String result = arr.getItems().stream()
				.map(item -> TSObject.toString(engine, item))
				.collect(Collectors.joining(separator));
		engine.recordStringCreation(result);
		return TSValue.of(result);
	}

	/**
	 * The Array.prototype methods only work on arrays.
	 */
	// Array.prototype.at(index): like a[index], but a negative index counts from the end
	private TSValue at(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "at");
		int index = TSObject.toRelativeIndex(args[0], arr.getLength());
		return index < 0 ? TSValue.UNDEFINED : arr.item(index);
	}

	// Array.prototype.indexOf(value[, start]): the first index of an item == value (tinyscript's ==),
	// or -1; like Javascript, a negative start counts from the end
	private TSValue indexOf(TSObject self, TSValue[] args) {
		List<TSValue> items = thisArray(self, "indexOf").getItems();
		int start = TSObject.toStartIndex(args, 1);
		if (start < 0)
			start = Math.max(0, items.size() + start);
		for (int i = start; i < items.size(); i++) {
			if (items.get(i).tinyscriptEquals(args[0]))
				return TSValue.of(i);
		}
		return TSValue.of(-1);
	}

	private static ArrayObject thisArray(TSObject self, String method) {
		if (!(self instanceof ArrayObject))
			throw new TinyscriptTypeError("Function Array.prototype." + method + " only works for Array objects.");
		return (ArrayObject) self;
	}

	private static Function callback(TSValue value, String method) {
		if (!(value instanceof Function function))
			throw new TinyscriptTypeError(method + "() needs a function, not " + value.typeOf());
		return function;
	}

	private static TSValue call(Function function, ArrayObject arr, int index) {
		return function.apply(null, new TSValue[] { arr.item(index), TSValue.of(index), arr });
	}

}
