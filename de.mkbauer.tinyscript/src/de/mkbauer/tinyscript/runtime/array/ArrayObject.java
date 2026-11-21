package de.mkbauer.tinyscript.runtime.array;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.Display;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class ArrayObject extends BuiltinType {
		
	private List<TSValue> items;
	
	ArrayObject(TinyscriptEngine engine) {
		super(engine);
		items = new ArrayList<TSValue>();
		// defineDefaultProperty(this, "length", TSValue.of(0));
	}
	
	public TSValue item(int index) {
		if (index < getLength()) 
			return items.get(index);
		else
			return TSValue.UNDEFINED;
	}

	public void setItem(int index, TSValue value) {
		if (index < getLength()) {
			items.set(index, value);
			return;
		}
		// Check the size limit before filling the gap with undefined, not after
		engine.checkObjectSize(index + 1 + properties.size());
		while (items.size() < index)
			items.add(TSValue.UNDEFINED);
		items.add(value);
		update();
	}

	/**
	 * Returns the array index a property key stands for, or -1 if the key is not
	 * an index (like in Javascript, "-1" or "01" are not array indexes).
	 */
	private static int arrayIndex(String key) {
		try {
			int index = Integer.parseInt(key);
			return (index >= 0 && String.valueOf(index).equals(key)) ? index : -1;
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	@Override
	public TSValue get(String key) {
		if (key.equals("length"))
			return TSValue.of(getLength());
		int index = arrayIndex(key);
		if (index < 0)
			return super.get(key);
		return item(index);
	}

	/**
	 * Arrays are lists: besides their items only length can be set. Unlike Javascript,
	 * named properties (including a[-1] or a[1.5]) are a TypeError.
	 */
	@Override
	public void put(String key, TSValue value) {
		if (key.equals("length")) {
			setLength(value);
			return;
		}
		int index = arrayIndex(key);
		if (index < 0)
			throw new TinyscriptTypeError("Arrays only have integer indexes, cannot set '" + key + "'");
		setItem(index, value);
	}

	/**
	 * Like Javascript, setting length truncates the array or fills it with undefined.
	 */
	private void setLength(TSValue value) {
		if (!(value.isMathematicalInteger() && value.asDouble() >= 0 && value.asDouble() <= Integer.MAX_VALUE))
			throw new TinyscriptTypeError("Invalid array length: " + value.asString());
		int length = value.asInt();
		if (length < items.size()) {
			items.subList(length, items.size()).clear();
		}
		else {
			// Check the size limit before filling with undefined, not after
			engine.checkObjectSize(length + properties.size());
			while (items.size() < length)
				items.add(TSValue.UNDEFINED);
		}
		update();
	}
	
	public void push(TSValue value) {
		items.add(value);
		update();
	}
	
	public TSValue pop() {
		if (items.size() == 0)
			return TSValue.UNDEFINED;
		else {
			TSValue result = items.remove(items.size()-1);
			update();
			return result;
		}
	}
	
	public void unshift(TSValue value) {
		items.add(0, value);
		update();
	}
	
	public TSValue shift() {
		if (items.size() == 0)
			return TSValue.UNDEFINED;
		else {
			TSValue result = items.remove(0);
			update();
			return result;
		}
	}
	
	@Override
	protected String display(Display display) {
		return items.stream()
				.map(display::format)
				.collect(Collectors.joining(", ", "[", "]"));
	}
	
	public int getLength() {
		return items.size();
	}

	@Override
	public TSValue valueOf() {
		return TSValue.UNDEFINED;
	}

	public List<TSValue> getItems() {
		return items;
	}
	
	public TSValue[] asJavaArray() {
		TSValue[] result = new  TSValue[items.size()];
		items.toArray(result);
		return result;
	}
	
	@Override
	public List<String> getKeys() {
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < items.size(); i++) {
			result.add(String.valueOf(i));
		}
		result.addAll(super.getKeys());
		return result; 
	}
	
	protected void update() {
		super.update();
		//put("length", TSValue.of(getLength()));
	}
	
	@Override
	public int getObjectSize() {

		if (items != null)
			return (items.size() + properties.size());
		else 
			return 0;
	}

}
