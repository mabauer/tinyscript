package de.mkbauer.tinyscript.runtime.array;

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
		newObject.defineDefaultProperty("constructor", new TSValue(this));
		return newObject;
	}

	// Array.isArray(value)
	private TSValue isArray(TSObject self, TSValue[] args) {
		return new TSValue(args[0].isObject() && args[0].asObject() instanceof ArrayObject);
	}

	// Array.prototype methods

	private TSValue arrayToString(TSObject self, TSValue[] args) {
		String result = thisArray(self, "toString").getItems().stream()
				.map(item -> TSObject.toString(engine, item))
				.collect(Collectors.joining(", "));
		engine.recordStringCreation(result);
		return new TSValue(result);
	}

	private TSValue push(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "push");
		for (TSValue arg : args)
			arr.push(arg);
		return new TSValue(arr.getLength());
	}

	private TSValue pop(TSObject self, TSValue[] args) {
		return thisArray(self, "pop").pop();
	}

	private TSValue unshift(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "unshift");
		for (TSValue arg : args)
			arr.unshift(arg);
		return new TSValue(arr.getLength());
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
		return new TSValue(result);
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
		return new TSValue(result);
	}

	private TSValue join(TSObject self, TSValue[] args) {
		ArrayObject arr = thisArray(self, "join");
		String separator = (args.length >= 1 && args[0].isString()) ? args[0].asString() : "";
		String result = arr.getItems().stream()
				.map(item -> TSObject.toString(engine, item))
				.collect(Collectors.joining(separator));
		engine.recordStringCreation(result);
		return new TSValue(result);
	}

	/**
	 * The Array.prototype methods only work on arrays.
	 */
	private static ArrayObject thisArray(TSObject self, String method) {
		if (!(self instanceof ArrayObject))
			throw new TinyscriptTypeError("Function Array.prototype." + method + " only works for Array objects.");
		return (ArrayObject) self;
	}

	/**
	 * map and filter only call functions that take at least one parameter.
	 */
	private static Function callbackWithParameters(TSValue value) {
		if (value.isCallable() && ((Function) value.asObject()).getLength() > 0)
			return (Function) value.asObject();
		return null;
	}

}
