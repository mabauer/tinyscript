package de.mkbauer.tinyscript.runtime.array;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.ResourceMonitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class ArrayObject extends BuiltinType {
	
	private static final String CONSTRUCTOR = "Array";
		
	private List<TSValue> items;
	
	// TODO: Check prototype property -- this should equal [].__proto
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
				int len = getLength();
				for (int i = len; i < index; i++) 
					items.add(TSValue.UNDEFINED);
				items.add(value);
				update();
			}
		}
		catch (NumberFormatException e) {
			super.put(key, value);
		}
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
	
	public ArrayObject clone() {
		ArrayObject result = new ArrayObject(ev);
		result.items.addAll(items);
		result.update();
		return result;
	}
	
	public static ArrayObject concat(ArrayObject arr1, ArrayObject arr2) {
		ArrayObject result = new ArrayObject(arr1.ev);
		result.items.addAll(arr1.items);
		result.items.addAll(arr2.items);
		// TODO: Handle named properties
		result.update();
		return result;
	}

	@Override
	public String toString() {
		// TODO: Handle named properties
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

	@Override
	public String getConstructorName() {
		return CONSTRUCTOR;
	}

	protected List<TSValue> getItems() {
		return items;
	}
	
	protected void update() {
		super.update();
		put("length", new TSValue(getLength()));
	}
	
	@Override
	public int getObjectSize() {

		if (items != null)
			return (items.size() + properties.size());
		else 
			return 0;
	}

}
