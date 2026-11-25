package de.mkbauer.tinyscript.runtime.object;

import java.util.Arrays;
import java.util.List;

import de.mkbauer.tinyscript.interpreter.Display;
import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.EmptyFunction;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSPropertyDescriptor;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.array.ArrayConstructor;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;
import de.mkbauer.tinyscript.runtime.collection.MapObject;


public class ObjectConstructor extends BuiltinConstructor {

	public final static String NAME = "Object";

	public ObjectConstructor(TinyscriptEngine engine) {
		super(engine);
	}

	public void initialize() {

		// In order to correctly initialize the Object constructor function,
		// we need to define the prototype of all functions and the prototype of all objects.

		// We are the Object constructor, aka function Object()
		// Store ourselves into the global context, because
		// other functions that we will add to our prototype next will need this.
		engine.getGlobalContext().store("Object", this);

		// Let's define our prototype (__proto__), the prototype of all functions
		Function proto = new EmptyFunction(engine);
		// Property: __proto__
		this.setPrototype(proto);

		proto.setPrototype(engine.getDefaultPrototype());
		proto.setPrototypeProperty(proto);

		// Add all the basic stuff to our prototype, the prototype of all functions
		proto.defineBuiltinMethod("toString", this::functionToString, 0);
		proto.defineBuiltinMethod("call", this::functionCall, 1, BuiltinFunction.VARIADIC);
		proto.defineBuiltinMethod("apply", this::functionApply, 1, 2);
		proto.defineDefaultProperty("length", TSValue.of(getLength()));

		// Object.create(proto), Object.keys(obj)...
		defineBuiltinMethod("create", this::create, 1);
		defineBuiltinMethod("getPrototypeOf", this::getPrototypeOf, 1);
		defineBuiltinMethod("defineProperty", this::defineProperty, 3);
		defineBuiltinMethod("getOwnPropertyNames", this::getOwnPropertyNames, 1);
		defineBuiltinMethod("getOwnPropertyDescriptor", this::getOwnPropertyDescriptor, 2);
		defineBuiltinMethod("keys", this::keys, 1);
		defineBuiltinMethod("values", this::values, 1);
		defineBuiltinMethod("entries", this::entries, 1);
		defineBuiltinMethod("fromEntries", this::fromEntries, 1);
		defineBuiltinMethod("assign", this::assign, 1, BuiltinFunction.VARIADIC);

		// Now, that we have the prototype of all functions, we can safely add functions to the prototype of all objects
		TSObject defaultPrototype = engine.getDefaultPrototype();
		defaultPrototype.defineBuiltinMethod("toString", this::objectToString, 0);
		defaultPrototype.defineBuiltinMethod("hasOwnProperty", this::hasOwnProperty, 1);
		defaultPrototype.defineDefaultProperty("constructor", this);

		setPrototypeProperty(engine.getDefaultPrototype());
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		// ECMAScript specifies Object.length as 1 (a historical value from ES1)
		return 1;
	}

	// But Object() takes no argument as well
	@Override
	public int getMinArgs() {
		return 0;
	}

	// Object() and new Object() make an empty object, Object(o) is o; tinyscript has no objects
	// for numbers, strings or booleans, so anything else is an error (Javascript wraps it)
	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		if (args.length == 0)
			return self != null ? self : createObject();
		return object(args[0], "Object");
	}

	@Override
	public TSObject createObject() {
		TSObject newObject = new TSObject(engine, engine.getDefaultPrototype());
		newObject.defineDefaultProperty("constructor", this);
		return newObject;
	}

	// Object.create(proto), Object.getPrototypeOf(object) ...

	private TSValue create(TSObject self, TSValue[] args) {
		if (args[0].equals(TSValue.NULL))
			return new TSObject(engine, null);
		if (args[0] instanceof TSObject proto)
			return new TSObject(engine, proto);
		throw new TinyscriptTypeError("Object.create(proto): proto must be an object!");
	}

	// These need objects (Javascript: the indexes of a string, Number.prototype for a number)
	private TSValue getPrototypeOf(TSObject self, TSValue[] args) {
		TSObject prototype = object(args[0], "getPrototypeOf").getPrototype();
		return prototype != null ? prototype : TSValue.NULL;
	}

	private TSValue defineProperty(TSObject self, TSValue[] args) {
		TSObject object = object(args[0], "defineProperty");
		if (object instanceof ArrayObject)
			throw new TinyscriptTypeError("Object.defineProperty is not supported for arrays");
		String key = propertyKey(args[1]);
		if (!(args[2] instanceof TSObject descriptor))
			throw new TinyscriptTypeError("defineProperty() needs a descriptor object, not " + args[2].typeOf());
		object.setOwnPropertyDescriptor(key, TSPropertyDescriptor.fromObject(descriptor));
		return object;
	}

	private TSValue getOwnPropertyNames(TSObject self, TSValue[] args) {
		return stringArray(object(args[0], "getOwnPropertyNames").getOwnPropertyNames());
	}

	private TSValue getOwnPropertyDescriptor(TSObject self, TSValue[] args) {
		TSObject object = object(args[0], "getOwnPropertyDescriptor");
		if (object.getOwnPropertyDescriptor(propertyKey(args[1])) instanceof TSPropertyDescriptor desc)
			return desc.toObject(engine);
		return TSValue.UNDEFINED;
	}

	private TSValue keys(TSObject self, TSValue[] args) {
		return stringArray(object(args[0], "keys").getKeys());
	}

	// Object.values(obj), Object.entries(obj): the values / the [key, value] pairs of the own
	// enumerable properties, in the order of Object.keys
	private TSValue values(TSObject self, TSValue[] args) {
		TSObject object = object(args[0], "values");
		ArrayObject result = newArray();
		for (String key : object.getKeys())
			result.push(object.get(key));
		return result;
	}

	private TSValue entries(TSObject self, TSValue[] args) {
		TSObject object = object(args[0], "entries");
		ArrayObject result = newArray();
		for (String key : object.getKeys()) {
			ArrayObject pair = newArray();
			pair.push(TSValue.of(key));
			pair.push(object.get(key));
			result.push(pair);
		}
		return result;
	}

	// Object.fromEntries(pairs): an object from [key, value] pairs, an array of them or a Map;
	// the keys must be strings or numbers
	private TSValue fromEntries(TSObject self, TSValue[] args) {
		TSObject result = new TSObject(engine, engine.getDefaultPrototype());
		if (args[0] instanceof MapObject map) {
			for (var entry : map.getEntries().entrySet())
				result.put(propertyKey(entry.getKey()), entry.getValue());
			return result;
		}
		if (!(args[0] instanceof ArrayObject pairs))
			throw new TinyscriptTypeError("fromEntries() needs an array of [key, value] pairs or a Map, not " + args[0].typeOf());
		for (TSValue item : pairs.getItems()) {
			if (!(item instanceof ArrayObject pair) || pair.getLength() < 2)
				throw new TinyscriptTypeError("fromEntries() needs [key, value] pairs");
			result.put(propertyKey(pair.item(0)), pair.item(1));
		}
		return result;
	}

	private static String propertyKey(TSValue key) {
		if (!(key.isString() || key.isNumber()))
			throw new TinyscriptTypeError("A property key must be a string or a number, not " + key.typeOf());
		return key.asString();
	}

	// Object.assign(target, source, ...): copies the own enumerable properties of the sources to
	// target and returns it
	private TSValue assign(TSObject self, TSValue[] args) {
		TSObject target = object(args[0], "assign");
		for (int i = 1; i < args.length; i++) {
			if (args[i] == TSValue.NULL || args[i] == TSValue.UNDEFINED)
				continue;
			TSObject source = object(args[i], "assign");
			for (String key : source.getKeys())
				target.put(key, source.get(key));
		}
		return target;
	}

	private static TSObject object(TSValue value, String function) {
		if (!(value instanceof TSObject object))
			throw new TinyscriptTypeError(function + "() needs an object, not " + value.typeOf());
		return object;
	}

	private ArrayObject newArray() {
		return (ArrayObject) engine.getConstructor(ArrayConstructor.NAME).createObject();
	}

	private TSValue stringArray(List<String> strings) {
		ArrayObject result = (ArrayObject) engine.getConstructor(ArrayConstructor.NAME).createObject();
		for (String str : strings) {
			result.push(TSValue.of(str));
			engine.recordStringCreation(str);
		}
		return result;
	}

	// Object.prototype methods (the prototype of all objects)

	// The same text as print and String(o): {x: 1}, Point {x: 1, y: 2} (Javascript: "[object Object]")
	private TSValue objectToString(TSObject self, TSValue[] args) {
		String result = new Display(true).own(self);
		engine.recordStringCreation(result);
		return TSValue.of(result);
	}

	private TSValue hasOwnProperty(TSObject self, TSValue[] args) {
		return TSValue.of(self.hasOwnProperty(args[0].asString()));
	}

	// Function.prototype methods (the prototype of all functions)

	// The same text as print and String(f): [Function: add], [class Point] (Javascript: the source)
	private TSValue functionToString(TSObject self, TSValue[] args) {
		return TSValue.of(new Display(false).own(self));
	}

	private TSValue functionCall(TSObject self, TSValue[] args) {
		if (!(self instanceof Function))
			return TSValue.UNDEFINED;
		return ((Function) self).apply(thisObject(args[0]), Arrays.copyOfRange(args, 1, args.length));
	}

	private TSValue functionApply(TSObject self, TSValue[] args) {
		if (!(self instanceof Function))
			return TSValue.UNDEFINED;
		TSValue[] arguments = (args.length > 1 && args[1] instanceof ArrayObject array) ? array.asJavaArray() : new TSValue[0];
		return ((Function) self).apply(thisObject(args[0]), arguments);
	}

	/**
	 * call and apply use the global object as this, if no object is given.
	 */
	private TSObject thisObject(TSValue value) {
		return value instanceof TSObject object ? object : engine.getGlobalContext().getGlobalObject();
	}

}
