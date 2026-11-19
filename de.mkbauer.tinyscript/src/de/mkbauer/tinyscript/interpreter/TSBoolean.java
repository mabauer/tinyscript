package de.mkbauer.tinyscript.interpreter;

/**
 * A boolean value; use {@link TSValue#of(boolean)}.
 */
public record TSBoolean(boolean value) implements TSValue {

	static final TSBoolean TRUE = new TSBoolean(true);
	static final TSBoolean FALSE = new TSBoolean(false);

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
