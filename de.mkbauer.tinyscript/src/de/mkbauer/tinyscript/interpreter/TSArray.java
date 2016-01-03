package de.mkbauer.tinyscript.interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TSArray extends TSObject {
	
	private List<TSValue> items;
	
	public TSArray() {
		super();	
	}
	
	@Override
	protected void initialize() {
		items = new ArrayList<TSValue>();
		setPrototype(new TSObject());
		defineDefaultProperty(this, "length", new TSValue(0));
	}

	@Override
	public TSValue get(String key) {
		try {
			int index = Integer.parseInt(key);
			if (index < getLength()) 
				return items.get(index);
			else
				return TSValue.UNDEFINED;
		}
		catch (NumberFormatException e) {
			return super.get(key);
		}
	}

	@Override
	public void put(String key, TSValue value) {
		try {
			int index = Integer.parseInt(key);
			if (index < getLength()) 
				items.set(index, value);
			else {
				for (int i = getLength(); i < index; i++) 
					items.set(i, TSValue.UNDEFINED);
				items.add(value);
			}
			put("length", new TSValue(getLength()));
		}
		catch (NumberFormatException e) {
			super.put(key, value);
		}
	}
	
	public void add(TSValue value) {
		items.add(value);
		put("length", new TSValue(getLength()));
	}
	
	public TSArray clone() {
		TSArray result = new TSArray();
		result.items.addAll(items);
		result.put("length", new TSValue(result.getLength()));
		return result;
	}
	
	public static TSArray concat(TSArray arr1, TSArray arr2) {
		TSArray result = new TSArray();
		result.items.addAll(arr1.items);
		result.items.addAll(arr2.items);
		// TODO: Handle named properties
		result.put("length", new TSValue(result.getLength()));
		return result;
	}

	@Override
	public String toString() {
		// TODO: Handle named properties
		return items.stream()
				.map(item->item.toString())
				.collect(Collectors.joining(", ", "[ ", " ]"));
	}
	
	public int getLength() {
		return items.size();
	}

}
