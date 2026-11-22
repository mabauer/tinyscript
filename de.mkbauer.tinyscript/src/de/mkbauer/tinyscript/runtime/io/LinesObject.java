package de.mkbauer.tinyscript.runtime.io;

import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.Display;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;

/**
 * The remaining lines of the standard input (std:io's lines()): lazy, the for loop reads
 * them one by one. Lines that have been read are gone, so a second loop goes on where the
 * first stopped.
 */
public class LinesObject extends BuiltinType {

	private final IoModule module;

	LinesObject(TinyscriptEngine engine, IoModule module) {
		super(engine);
		setPrototype(engine.getDefaultPrototype());
		this.module = module;
	}

	/** The next line, or null at the end of the input. */
	public TSValue next() {
		String line = module.read("");
		return line == null ? null : TSValue.of(line);
	}

	@Override
	public TSValue valueOf() {
		return TSValue.UNDEFINED;
	}

	@Override
	protected String display(Display display) {
		return "[Lines]";
	}
}
