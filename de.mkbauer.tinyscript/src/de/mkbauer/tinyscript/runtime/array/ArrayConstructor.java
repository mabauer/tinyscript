package de.mkbauer.tinyscript.runtime.array;

import java.util.List;

import java.util.stream.Collectors;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSObject;
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

	private TSValue map(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "map");
		ArrayObject result = (ArrayObject) createObject();
		Function function = callbackWithParameters(args[0]);
		if (function != null) {
			for (int i = 0; i < arr.getLength(); i++) {
				result.push(function.apply(null, new TSValue[] { arr.get(String.valueOf(i)) }));
			}
		}
		return result;
	}

	private TSValue filter(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "filter");
		ArrayObject result = (ArrayObject) createObject();
		Function function = callbackWithParameters(args[0]);
		if (function != null) {
			for (int i = 0; i < arr.getLength(); i++) {
				TSValue item = arr.get(String.valueOf(i));
				if (function.apply(null, new TSValue[] { item }).asBoolean())
					result.push(item);
			}
		}
		return result;
	}

	private TSValue join(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "join");
		String separator = (args.length >= 1 && args[0].isString()) ? args[0].asString() : "";
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

	/**
	 * map and filter only call functions that take at least one parameter.
	 */
	private static Function callbackWithParameters(TSValue value) {
		return value instanceof Function function && function.getLength() > 0 ? function : null;
	}

}
