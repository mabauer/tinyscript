package de.mkbauer.tinyscript.interpreter;

/**
 * A number (always a double, like in Javascript).
 */
public record TSNumber(double value) implements TSValue {

	/**
	 * Integral numbers are printed without decimal places ({@code 3}, not {@code 3.0}).
	 */
	@Override
	public String toString() {
		if (Math.rint(value) == value)
			return String.format("%.0f", value);
		return Double.toString(value);
	}
}
