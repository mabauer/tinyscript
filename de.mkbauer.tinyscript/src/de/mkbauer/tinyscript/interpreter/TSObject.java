package de.mkbauer.tinyscript.interpreter;

import java.util.HashMap;
import java.util.stream.Collectors;


public class TSObject {
	
	HashMap<String, TSValue> values;
	
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
		
		return values.keySet().stream()
				.map(key->key+": "+values.get(key).toString())
				.collect(Collectors.joining(", ", "{ ", " }"));
	}

}
