package de.mkbauer.tinyscript.runtime.collection;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.Display;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSValue;

/**
 * A Set: values of any type (see {@link Keys}) in insertion order. Like a Map, a Set has no
 * properties of its own besides size.
 */
public class SetObject extends BuiltinType {

	private final Set<TSValue> values = new LinkedHashSet<>();

	SetObject(TinyscriptEngine engine) {
		super(engine);
	}

	public boolean hasValue(TSValue value) {
		return values.contains(Keys.normalize(value));
	}

	public void addValue(TSValue value) {
		values.add(Keys.normalize(value));
		update();
	}

	public boolean deleteValue(TSValue value) {
		boolean found = values.remove(Keys.normalize(value));
		update();
		return found;
	}

	public void clear() {
		values.clear();
		update();
	}

	public Set<TSValue> getValues() {
		return values;
	}

	@Override
	public TSValue get(String key) {
		if (key.equals("size"))
			return TSValue.of(values.size());
		return super.get(key);
	}

	@Override
	public void put(String key, TSValue value) {
		throw new TinyscriptTypeError("Sets have no properties, cannot set '" + key + "' (use add(value))");
	}

	@Override
	public void delete(String key) {
		throw new TinyscriptTypeError("Sets have no properties: use s.delete(value)");
	}

	@Override
	public TSValue valueOf() {
		return TSValue.UNDEFINED;
	}

	@Override
	public int getObjectSize() {
		// Called by the superclass constructor, before values is initialized
		return (values != null ? values.size() : 0) + properties.size();
	}

	@Override
	protected String display(Display display) {
		return values.stream().map(display::format).collect(Collectors.joining(", ", "Set {", "}"));
	}

}
