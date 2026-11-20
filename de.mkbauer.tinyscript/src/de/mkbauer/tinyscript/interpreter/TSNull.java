package de.mkbauer.tinyscript.interpreter;

/**
 * The value {@code null}; use {@link TSValue#NULL}.
 */
public enum TSNull implements TSValue {
	INSTANCE;

	@Override
	public String toString() {
		return "null";
	}
}
