package de.mkbauer.tinyscript.runtime.collection;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.array.ArrayConstructor;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;

/**
 * Set as in Javascript, except that values() returns an array (not an iterator).
 */
public class SetConstructor extends BuiltinConstructor {

	public static final String NAME = "Set";

	public SetConstructor(TinyscriptEngine engine) {
		super(engine);
		TSObject setPrototype = getPrototypeProperty().asObject();
		setPrototype.defineBuiltinMethod("add", this::add, 1);
		setPrototype.defineBuiltinMethod("has", this::has, 1);
		setPrototype.defineBuiltinMethod("delete", this::delete, 1);
		setPrototype.defineBuiltinMethod("clear", this::clear, 0);
		setPrototype.defineBuiltinMethod("values", this::values, 0);
		setPrototype.defineBuiltinMethod("forEach", this::forEach, 1);
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
		SetObject newObject = new SetObject(engine);
		newObject.setPrototype(getPrototypeProperty().asObject());
		newObject.defineDefaultProperty("constructor", this);
		return newObject;
	}

	// new Set(values): the values of an array, a string, a Set or a Map
	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		if (!(self instanceof SetObject set))
			throw new TinyscriptTypeError("Set needs new: new Set()");
		if (args.length > 0 && args[0] != TSValue.UNDEFINED && args[0] != TSValue.NULL) {
			List<TSValue> values = engine.iterableItems(args[0]);
			if (values == null)
				throw new TinyscriptTypeError("new Set() needs an array, not " + args[0].typeOf());
			values.forEach(set::addValue);
		}
		return set;
	}

	// Set.prototype.add(value) returns the set, so that calls can be chained
	private TSValue add(TSObject self, TSValue[] args) {
		SetObject set = thisSet(self, "add");
		set.addValue(args[0]);
		return set;
	}

	private TSValue has(TSObject self, TSValue[] args) {
		return TSValue.of(thisSet(self, "has").hasValue(args[0]));
	}

	private TSValue delete(TSObject self, TSValue[] args) {
		return TSValue.of(thisSet(self, "delete").deleteValue(args[0]));
	}

	private TSValue clear(TSObject self, TSValue[] args) {
		thisSet(self, "clear").clear();
		return TSValue.UNDEFINED;
	}

	private TSValue values(TSObject self, TSValue[] args) {
		ArrayObject result = (ArrayObject) engine.getConstructor(ArrayConstructor.NAME).createObject();
		thisSet(self, "values").getValues().forEach(result::push);
		return result;
	}

	// Set.prototype.forEach(function): function(value, value, set) for each value, like Javascript
	private TSValue forEach(TSObject self, TSValue[] args) {
		SetObject set = thisSet(self, "forEach");
		if (!(args[0] instanceof Function function))
			throw new TinyscriptTypeError("forEach() needs a function, not " + args[0].typeOf());
		// On a copy, so that the function may change the set
		for (TSValue value : List.copyOf(set.getValues()))
			function.apply(null, new TSValue[] { value, value, set });
		return TSValue.UNDEFINED;
	}

	private static SetObject thisSet(TSObject self, String method) {
		if (!(self instanceof SetObject set))
			throw new TinyscriptTypeError("Function Set.prototype." + method + " only works for Sets.");
		return set;
	}

}
