package de.mkbauer.tinyscript.runtime.array;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class ArrayObject extends Function {
	
	private List<TSValue> items;
	
	public ArrayObject(ExecutionVisitor ev) {
		super(ev);	
		items = new ArrayList<TSValue>();
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
	
	public ArrayObject clone() {
		ArrayObject result = new ArrayObject(ev);
		result.items.addAll(items);
		result.put("length", new TSValue(result.getLength()));
		return result;
	}
	
	public static ArrayObject concat(ArrayObject arr1, ArrayObject arr2) {
		ArrayObject result = new ArrayObject(arr1.ev);
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

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
