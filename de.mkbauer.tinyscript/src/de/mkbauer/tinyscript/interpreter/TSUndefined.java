package de.mkbauer.tinyscript.interpreter;

/**
 * The value {@code undefined}; use {@link TSValue#UNDEFINED}.
 */
public enum TSUndefined implements TSValue {
	INSTANCE;

	@Override
	public String toString() {
		return "Undefined";
	}
}
