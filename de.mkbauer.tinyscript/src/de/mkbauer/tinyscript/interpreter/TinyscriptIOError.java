package de.mkbauer.tinyscript.interpreter;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;

/**
 * Reading or writing a file failed (std:fs), or reading the standard input (std:io).
 */
public class TinyscriptIOError extends TinyscriptRuntimeException {

	private static final long serialVersionUID = 1L;

	public TinyscriptIOError(String message) {
		super(message);
	}
}
