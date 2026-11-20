package de.mkbauer.tinyscript.runtime.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.array.ArrayConstructor;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;

/**
 * Map as in Javascript, except that keys(), values() and entries() return arrays (not
 * iterators), so that length, map, sort etc. work on them.
 */
public class MapConstructor extends BuiltinConstructor {

	public static final String NAME = "Map";

	public MapConstructor(TinyscriptEngine engine) {
		super(engine);
		TSObject mapPrototype = getPrototypeProperty().asObject();
		mapPrototype.defineBuiltinMethod("get", this::get, 1);
		mapPrototype.defineBuiltinMethod("set", this::set, 2);
		mapPrototype.defineBuiltinMethod("has", this::has, 1);
		mapPrototype.defineBuiltinMethod("delete", this::delete, 1);
		mapPrototype.defineBuiltinMethod("clear", this::clear, 0);
		mapPrototype.defineBuiltinMethod("keys", this::keys, 0);
		mapPrototype.defineBuiltinMethod("values", this::values, 0);
		mapPrototype.defineBuiltinMethod("entries", this::entries, 0);
		mapPrototype.defineBuiltinMethod("forEach", this::forEach, 1);
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
		MapObject newObject = new MapObject(engine);
		newObject.setPrototype(getPrototypeProperty().asObject());
		newObject.defineDefaultProperty("constructor", this);
		return newObject;
	}

	// new Map([[key, value], ...]): the entries of an array of [key, value] pairs or of another Map
	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		if (!(self instanceof MapObject map))
			throw new TinyscriptTypeError("Map needs new: new Map()");
		if (args.length > 0 && args[0] != TSValue.UNDEFINED && args[0] != TSValue.NULL) {
			List<TSValue> entries = engine.iterableItems(args[0]);
			if (entries == null)
				throw new TinyscriptTypeError("new Map() needs an array of [key, value] pairs, not " + args[0].typeOf());
			for (TSValue entry : entries) {
				if (!(entry instanceof ArrayObject pair))
					throw new TinyscriptTypeError("new Map() needs an array of [key, value] pairs, not of " + entry.typeOf());
				map.setEntry(pair.item(0), pair.item(1));
			}
		}
		return map;
	}

	private TSValue get(TSObject self, TSValue[] args) {
		return thisMap(self, "get").getEntry(args[0]);
	}

	// Map.prototype.set(key, value) returns the map, so that calls can be chained
	private TSValue set(TSObject self, TSValue[] args) {
		MapObject map = thisMap(self, "set");
		map.setEntry(args[0], args[1]);
		return map;
	}

	private TSValue has(TSObject self, TSValue[] args) {
		return TSValue.of(thisMap(self, "has").hasEntry(args[0]));
	}

	private TSValue delete(TSObject self, TSValue[] args) {
		return TSValue.of(thisMap(self, "delete").deleteEntry(args[0]));
	}

	private TSValue clear(TSObject self, TSValue[] args) {
		thisMap(self, "clear").clear();
		return TSValue.UNDEFINED;
	}

	private TSValue keys(TSObject self, TSValue[] args) {
		return array(thisMap(self, "keys").getEntries().keySet());
	}

	private TSValue values(TSObject self, TSValue[] args) {
		return array(thisMap(self, "values").getEntries().values());
	}

	private TSValue entries(TSObject self, TSValue[] args) {
		List<TSValue> pairs = new ArrayList<>();
		for (Map.Entry<TSValue, TSValue> entry : thisMap(self, "entries").getEntries().entrySet())
			pairs.add(array(List.of(entry.getKey(), entry.getValue())));
		return array(pairs);
	}

	// Map.prototype.forEach(function): function(value, key, map) for each entry
	private TSValue forEach(TSObject self, TSValue[] args) {
		MapObject map = thisMap(self, "forEach");
		if (!(args[0] instanceof Function function))
			throw new TinyscriptTypeError("forEach() needs a function, not " + args[0].typeOf());
		// On a copy, so that the function may change the map
		for (Map.Entry<TSValue, TSValue> entry : List.copyOf(map.getEntries().entrySet()))
			function.apply(null, new TSValue[] { entry.getValue(), entry.getKey(), map });
		return TSValue.UNDEFINED;
	}

	private ArrayObject array(Iterable<TSValue> items) {
		ArrayObject result = (ArrayObject) engine.getConstructor(ArrayConstructor.NAME).createObject();
		for (TSValue item : items)
			result.push(item);
		return result;
	}

	private static MapObject thisMap(TSObject self, String method) {
		if (!(self instanceof MapObject map))
			throw new TinyscriptTypeError("Function Map.prototype." + method + " only works for Maps.");
		return map;
	}

}
