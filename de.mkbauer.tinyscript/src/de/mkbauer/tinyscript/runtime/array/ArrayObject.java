package de.mkbauer.tinyscript.runtime.array;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
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
	 * Returns the array index a property key stands for, or -1 if the key is a
	 * named property (like in Javascript, "-1" or "01" are not array indexes).
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

	@Override
	public void put(String key, TSValue value) {
		// TODO(TODO.md: Arrays: length and named properties): Special case "length" => resize array and fill with TSValue.UNDEFINED
		if (key.equals("length"))
			return;
		int index = arrayIndex(key);
		if (index < 0)
			super.put(key, value);
		else
			setItem(index, value);
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
	public String toString() {
		// TODO(TODO.md: Arrays: length and named properties): Handle named properties
		return items.stream()
				.map(item->item.toString())
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
