package de.mkbauer.tinyscript.runtime.object;

import java.util.Arrays;
import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.EmptyFunction;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.InterpretedFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSPropertyDescriptor;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.array.ArrayConstructor;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;


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
		proto.defineBuiltinMethod("call", this::functionCall, 1);
		proto.defineBuiltinMethod("apply", this::functionApply, 1);
		proto.defineDefaultProperty("length", TSValue.of(getLength()));

		// Object.create(proto), Object.keys(obj)...
		defineBuiltinMethod("create", this::create, 1);
		defineBuiltinMethod("getPrototypeOf", this::getPrototypeOf, 1);
		defineBuiltinMethod("defineProperty", this::defineProperty, 3);
		defineBuiltinMethod("getOwnPropertyNames", this::getOwnPropertyNames, 1);
		defineBuiltinMethod("getOwnPropertyDescriptor", this::getOwnPropertyDescriptor, 2);
		defineBuiltinMethod("keys", this::keys, 1);

		// Now, that we have the prototype of all functions, we can safely add functions to the prototype of all objects
		TSObject defaultPrototype = engine.getDefaultPrototype();
		defaultPrototype.defineBuiltinMethod("toString", this::objectToString, 0);
		defaultPrototype.defineBuiltinMethod("hasOwnProperty", this::hasOwnProperty, 1);

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

	private TSValue getPrototypeOf(TSObject self, TSValue[] args) {
		if (args[0] instanceof TSObject object && object.getPrototype() != null)
			return object.getPrototype();
		return TSValue.NULL;
	}

	private TSValue defineProperty(TSObject self, TSValue[] args) {
		if (!(args[0] instanceof TSObject object))
			throw new TinyscriptTypeError("Object.defineProperty called on non-object");
		if (args[2] instanceof TSObject descriptor)
			object.setOwnPropertyDescriptor(args[1].asString(), TSPropertyDescriptor.fromObject(descriptor));
		return object;
	}

	private TSValue getOwnPropertyNames(TSObject self, TSValue[] args) {
		return stringArray(args[0] instanceof TSObject object ? object.getOwnPropertyNames() : List.of());
	}

	private TSValue getOwnPropertyDescriptor(TSObject self, TSValue[] args) {
		if (args[0] instanceof TSObject object && object.getOwnPropertyDescriptor(args[1].asString()) instanceof TSPropertyDescriptor desc)
			return desc.toObject(engine);
		return TSValue.UNDEFINED;
	}

	private TSValue keys(TSObject self, TSValue[] args) {
		return stringArray(args[0] instanceof TSObject object ? object.getKeys() : List.of());
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

	private TSValue objectToString(TSObject self, TSValue[] args) {
		String name = "Object";
		if (self.get("constructor") instanceof Function ctor)
			name = ctor.getName();
		return TSValue.of("[object " + name + "]");
	}

	private TSValue hasOwnProperty(TSObject self, TSValue[] args) {
		return TSValue.of(self.hasOwnProperty(args[0].asString()));
	}

	// Function.prototype methods (the prototype of all functions)

	private TSValue functionToString(TSObject self, TSValue[] args) {
		if (self instanceof InterpretedFunction)
			return TSValue.of(((InterpretedFunction) self).getCodeAsString());
		String name = (self instanceof Function) ? ((Function) self).getName() : "";
		return TSValue.of("function " + name + "() { // builtin }");
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
