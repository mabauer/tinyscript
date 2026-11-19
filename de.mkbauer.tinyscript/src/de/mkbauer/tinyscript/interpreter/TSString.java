package de.mkbauer.tinyscript.interpreter;

import java.util.Objects;

/**
 * A primitive string. {@link #toString()} quotes it, {@link #asString()} does not.
 */
public record TSString(String value) implements TSValue {

	public TSString {
		Objects.requireNonNull(value);
	}

	@Override
	public String toString() {
		return "'" + value + "'";
	}
}
