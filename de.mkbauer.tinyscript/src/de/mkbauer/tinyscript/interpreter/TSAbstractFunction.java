package de.mkbauer.tinyscript.interpreter;

import java.util.stream.Collectors;

public abstract class TSAbstractFunction extends TSObject {
			
	@Override
	protected void initialize() {
		TSObject proto = new TSObject();
		setPrototype(proto);
	}

	public abstract String getName();
	
	public abstract int getLength();

	public String toString() {
		String result = "";
		if (hasEnumerableProperties())
			result = result + "{ ";
		result = result + "[Function";
		if (getName() != null)
			result = result + ": " + getName(); 
		result = result + "]";
		if (hasEnumerableProperties())
			result = result + " " + properties.keySet().stream()
				.filter(key->properties.get(key).isEnumerable())
				.map(key->key+": "+properties.get(key).toString())
				.collect(Collectors.joining(", ")) + " }";
		return result;
	}

}
