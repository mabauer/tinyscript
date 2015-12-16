package de.mkbauer.tinyscript.interpreter;

import java.util.HashMap;


public class TSObject {
	
	private HashMap<String, TSValue> values;
	
	public TSObject() {
		values = new HashMap<String, TSValue>();
	}
		
	public TSValue get(String key) {
		if (contains(key))
			return values.get(key);
		else
			return TSValue.UNDEFINED;
	}
	
	public void put(String key, TSValue value) {
		values.put(key, value);
	}
		
	public boolean contains(String key) {
		return values.containsKey(key); 
	}
	
	public String toString() {
		String result = "{";
		for (String  key : values.keySet()) {
			result = result + key + ":" + values.get(key) + ",";
		}
		result = result + "}";
		return result;
	}

}
