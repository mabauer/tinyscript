package de.mkbauer.tinyscript.runtime.collection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.Display;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSValue;

/**
 * A Map: keys of any type (see {@link Keys}) in insertion order. Unlike Javascript, a Map has
 * no properties of its own besides size: m[k] = v (instead of m.set(k, v)) is a TypeError.
 */
public class MapObject extends BuiltinType {

	private final Map<TSValue, TSValue> entries = new LinkedHashMap<>();

	MapObject(TinyscriptEngine engine) {
		super(engine);
	}

	public TSValue getEntry(TSValue key) {
		TSValue value = entries.get(Keys.normalize(key));
		return value != null ? value : TSValue.UNDEFINED;
	}

	public boolean hasEntry(TSValue key) {
		return entries.containsKey(Keys.normalize(key));
	}

	public void setEntry(TSValue key, TSValue value) {
		entries.put(Keys.normalize(key), value);
		update();
	}

	public boolean deleteEntry(TSValue key) {
		boolean found = entries.remove(Keys.normalize(key)) != null;
		update();
		return found;
	}

	public void clear() {
		entries.clear();
		update();
	}

	public Map<TSValue, TSValue> getEntries() {
		return entries;
	}

	@Override
	public TSValue get(String key) {
		if (key.equals("size"))
			return TSValue.of(entries.size());
		return super.get(key);
	}

	@Override
	public void put(String key, TSValue value) {
		throw new TinyscriptTypeError("Maps have no properties, cannot set '" + key + "' (use set(key, value))");
	}

	@Override
	public TSValue valueOf() {
		return TSValue.UNDEFINED;
	}

	@Override
	public int getObjectSize() {
		// Called by the superclass constructor, before entries is initialized
		return (entries != null ? entries.size() : 0) + properties.size();
	}

	@Override
	protected String display(Display display) {
		return entries.entrySet().stream()
				.map(entry -> display.format(entry.getKey()) + " => " + display.format(entry.getValue()))
				.collect(Collectors.joining(", ", "Map {", "}"));
	}

}
