package de.mkbauer.tinyscript.runtime.number;

import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.Display;
import de.mkbauer.tinyscript.interpreter.TSNumber;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * The wrapper of a number for a method call ({@code x.toFixed(2)}), like a String object for a
 * string. It only lives for the call; its properties cannot be set.
 */
public class NumberObject extends BuiltinType {

	private final double value;

	NumberObject(TinyscriptEngine engine, double value, TSObject prototype) {
		super(engine);
		this.value = value;
		setPrototype(prototype);
	}

	public double getValue() {
		return value;
	}

	@Override
	public void put(String key, TSValue value) {
		throw new TinyscriptTypeError("Numbers cannot be changed (setting '" + key + "')");
	}

	@Override
	protected String display(Display display) {
		return TSNumber.format(value);
	}

	@Override
	public TSValue valueOf() {
		return TSValue.of(value);
	}
}
