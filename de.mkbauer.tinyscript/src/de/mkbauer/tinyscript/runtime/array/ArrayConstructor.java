package de.mkbauer.tinyscript.runtime.array;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.interpreter.Display;
import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
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
		arrayPrototype.defineBuiltinMethod("indexOf", this::indexOf, 1, 2);
		arrayPrototype.defineBuiltinMethod("push", this::push, 1, BuiltinFunction.VARIADIC);
		arrayPrototype.defineBuiltinMethod("pop", this::pop, 0);
		arrayPrototype.defineBuiltinMethod("unshift", this::unshift, 1, BuiltinFunction.VARIADIC);
		arrayPrototype.defineBuiltinMethod("shift", this::shift, 0);
		arrayPrototype.defineBuiltinMethod("map", this::map, 1);
		arrayPrototype.defineBuiltinMethod("filter", this::filter, 1);
		arrayPrototype.defineBuiltinMethod("join", this::join, 0, 1);
		arrayPrototype.defineBuiltinMethod("forEach", this::forEach, 1);
		arrayPrototype.defineBuiltinMethod("reduce", this::reduce, 1, 2);
		arrayPrototype.defineBuiltinMethod("some", this::some, 1);
		arrayPrototype.defineBuiltinMethod("every", this::every, 1);
		arrayPrototype.defineBuiltinMethod("find", this::find, 1);
		arrayPrototype.defineBuiltinMethod("findIndex", this::findIndex, 1);
		arrayPrototype.defineBuiltinMethod("includes", this::includes, 1, 2);
		arrayPrototype.defineBuiltinMethod("slice", this::slice, 0, 2);
		arrayPrototype.defineBuiltinMethod("fill", this::fill, 1, 3);
		arrayPrototype.defineBuiltinMethod("sort", this::sort, 0, 1);
		arrayPrototype.defineBuiltinMethod("reverse", this::reverse, 0);
		arrayPrototype.defineBuiltinMethod("toReversed", this::toReversed, 0);
		arrayPrototype.defineBuiltinMethod("splice", this::splice, 0, BuiltinFunction.VARIADIC);
		arrayPrototype.defineBuiltinMethod("lastIndexOf", this::lastIndexOf, 1, 2);
		arrayPrototype.defineBuiltinMethod("findLast", this::findLast, 1);
		arrayPrototype.defineBuiltinMethod("findLastIndex", this::findLastIndex, 1);
		arrayPrototype.defineBuiltinMethod("flat", this::flat, 0, 1);
		arrayPrototype.defineBuiltinMethod("flatMap", this::flatMap, 1);
		arrayPrototype.defineBuiltinMethod("entries", this::entries, 0);
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
		return newObject;
	}

	// Array.isArray(value)
	private TSValue isArray(TSObject self, TSValue[] args) {
		return TSValue.of(args[0] instanceof ArrayObject);
	}

	// Array.prototype methods

	// The same text as print and String(a): [1, "a"] (Javascript: "1,a")
	private TSValue arrayToString(TSObject self, TSValue[] args) {
		String result = new Display(true).own(thisArray(self, "toString"));
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
		// Like Javascript, unshift(a, b) puts a before b
		for (int i = args.length - 1; i >= 0; i--)
			arr.unshift(args[i]);
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

	// Array.prototype.includes(value[, start]): whether an item from start on == value
	// (tinyscript's ==); like indexOf, a negative start counts from the end. Unlike indexOf and
	// as in Javascript, includes finds NaN
	private TSValue includes(TSObject self, TSValue[] args) {
		List<TSValue> items = thisArray(self, "includes").getItems();
		if (isNaN(args[0])) {
			int start = TSObject.toStartIndex(args, 1);
			for (int i = start < 0 ? Math.max(0, items.size() + start) : start; i < items.size(); i++) {
				if (isNaN(items.get(i)))
					return TSValue.of(true);
			}
			return TSValue.of(false);
		}
		return TSValue.of(indexOf(items, args) >= 0);
	}

	private static boolean isNaN(TSValue value) {
		return value instanceof TSNumber number && Double.isNaN(number.value());
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

	// Array.prototype.reverse(): reverses the array in place and returns it, as in Javascript
	private TSValue reverse(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "reverse");
		arr.reverse();
		return arr;
	}

	// Array.prototype.toReversed(): a reversed copy; the array stays as it is
	private TSValue toReversed(TSObject self, TSValue[] args) {
		ArrayObject result = copy(thisArray(self, "toReversed").getItems());
		result.reverse();
		return result;
	}

	// Array.prototype.splice(start[, deleteCount[, item, ...]]): removes deleteCount items from
	// start (by default all to the end) and inserts the items there; returns the removed items.
	// A negative start counts from the end; unlike Javascript, the numbers must be integers.
	private TSValue splice(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "splice");
		int length = arr.getLength();
		int start = args.length > 0 ? sliceIndex(args[0], length, "splice") : length;
		int count = length - start;
		if (args.length > 1) {
			if (!args[1].isMathematicalInteger())
				throw new TinyscriptTypeError("splice() needs an integer count, not " + (args[1].isNumber() ? args[1].asString() : args[1].typeOf()));
			count = (int) Math.max(0, Math.min(args[1].asDouble(), length - start));
		}
		List<TSValue> insert = args.length > 2 ? List.of(args).subList(2, args.length) : List.of();
		return copy(arr.splice(start, count, insert));
	}

	// Array.prototype.lastIndexOf(value[, from]): the last index of an item == value at or before
	// from (by default the last item), or -1; a negative from counts from the end
	private TSValue lastIndexOf(TSObject self, TSValue[] args) {
		List<TSValue> items = thisArray(self, "lastIndexOf").getItems();
		int from = items.size() - 1;
		if (args.length > 1) {
			if (!args[1].isMathematicalInteger())
				throw new TinyscriptTypeError("lastIndexOf() needs an integer index, not " + (args[1].isNumber() ? args[1].asString() : args[1].typeOf()));
			double index = args[1].asDouble();
			from = (int) (index < 0 ? Math.max(-1, items.size() + index) : Math.min(index, items.size() - 1));
		}
		for (int i = from; i >= 0; i--) {
			if (items.get(i).tinyscriptEquals(args[0]))
				return TSValue.of(i);
		}
		return TSValue.of(-1);
	}

	// Array.prototype.findLast(f) / findLastIndex(f): like find / findIndex, from the end
	private TSValue findLast(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "findLast");
		int index = findLastIndex(arr, callback(args[0], "findLast"));
		return index >= 0 ? arr.item(index) : TSValue.UNDEFINED;
	}

	private TSValue findLastIndex(TSObject self, TSValue[] args) {
		return TSValue.of(findLastIndex(thisArray(self, "findLastIndex"), callback(args[0], "findLastIndex")));
	}

	private static int findLastIndex(ArrayObject arr, Function function) {
		for (int i = arr.getLength() - 1; i >= 0; i--) {
			if (i < arr.getLength() && call(function, arr, i).asCondition())
				return i;
		}
		return -1;
	}

	// Array.prototype.flat([depth]): a new array with the items of nested arrays, down to depth
	// levels (1 by default, Infinity for all)
	private TSValue flat(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "flat");
		double depth = 1;
		if (args.length > 0 && args[0] != TSValue.UNDEFINED) {
			if (!(args[0] instanceof TSNumber number) || number.value() < 0
					|| !(args[0].isMathematicalInteger() || number.value() == Double.POSITIVE_INFINITY))
				throw new TinyscriptTypeError("flat() needs a depth of 0 or more, not " + (args[0].isNumber() ? args[0].asString() : args[0].typeOf()));
			depth = number.value();
		}
		ArrayObject result = (ArrayObject) createObject();
		flatten(arr.getItems(), depth, result);
		return result;
	}

	private static void flatten(List<TSValue> items, double depth, ArrayObject result) {
		for (TSValue item : items) {
			if (depth > 0 && item instanceof ArrayObject inner)
				flatten(inner.getItems(), depth - 1, result);
			else
				result.push(item);
		}
	}

	// Array.prototype.flatMap(f): like map, but an array that f returns adds its items
	private TSValue flatMap(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "flatMap");
		Function function = callback(args[0], "flatMap");
		ArrayObject result = (ArrayObject) createObject();
		for (int i = 0, n = arr.getLength(); i < n; i++) {
			TSValue mapped = call(function, arr, i);
			if (mapped instanceof ArrayObject inner)
				for (TSValue item : inner.getItems())
					result.push(item);
			else
				result.push(mapped);
		}
		return result;
	}

	// Array.prototype.entries(): like the entries of a Map, an array of [index, element] pairs
	// (Javascript: an iterator), so for (const [i, x] : a.entries()) goes through both
	private TSValue entries(TSObject self, TSValue[] args) {
		List<TSValue> items = thisArray(self, "entries").getItems();
		ArrayObject result = (ArrayObject) createObject();
		for (int i = 0; i < items.size(); i++)
			result.push(copy(List.of(TSValue.of(i), items.get(i))));
		return result;
	}

	private ArrayObject copy(List<TSValue> items) {
		ArrayObject result = (ArrayObject) createObject();
		for (TSValue item : items)
			result.push(item);
		return result;
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
		return TSValue.of(indexOf(thisArray(self, "indexOf").getItems(), args));
	}

	private static int indexOf(List<TSValue> items, TSValue[] args) {
		int start = TSObject.toStartIndex(args, 1);
		if (start < 0)
			start = Math.max(0, items.size() + start);
		for (int i = start; i < items.size(); i++) {
			if (items.get(i).tinyscriptEquals(args[0]))
				return i;
		}
		return -1;
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
