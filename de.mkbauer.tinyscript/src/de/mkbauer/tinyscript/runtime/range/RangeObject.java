package de.mkbauer.tinyscript.runtime.range;

import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.Display;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

/**
 * A range of integers from start (inclusive) to end (exclusive) in steps: lazy, it does not
 * create its numbers, the for loop counts them (for (const i : range(n))).
 */
public class RangeObject extends BuiltinType {

	private final double start;
	private final double end;
	private final double step;

	RangeObject(TinyscriptEngine engine, TSObject prototype, double start, double end, double step) {
		super(engine);
		setPrototype(prototype);
		this.start = start;
		this.end = end;
		this.step = step;
	}

	public double getStart() {
		return start;
	}

	public double getStep() {
		return step;
	}

	/**
	 * Whether value (counting from start in steps) is still in the range
	 */
	public boolean contains(double value) {
		return step > 0 ? value < end : value > end;
	}

	/**
	 * The number of integers in the range
	 */
	public double length() {
		return Math.max(0, Math.ceil((end - start) / step));
	}

	@Override
	public TSValue get(String key) {
		if (key.equals("length"))
			return TSValue.of(length());
		return super.get(key);
	}

	@Override
	public void put(String key, TSValue value) {
		throw new TinyscriptTypeError("Ranges cannot be changed, cannot set '" + key + "'");
	}

	@Override
	public TSValue valueOf() {
		return TSValue.UNDEFINED;
	}

	// range(0, 3) or range(1, 9, 2)
	@Override
	protected String display(Display display) {
		return "range(" + TSValue.of(start).asString() + ", " + TSValue.of(end).asString()
				+ (step != 1 ? ", " + TSValue.of(step).asString() : "") + ")";
	}

}
