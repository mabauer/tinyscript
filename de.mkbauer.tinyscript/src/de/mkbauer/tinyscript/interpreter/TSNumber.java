package de.mkbauer.tinyscript.interpreter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * A number (always a double, like in Javascript).
 */
public record TSNumber(double value) implements TSValue {

	/**
	 * Javascript's Number::toString: the shortest digits that identify the number, written out
	 * from 10^-7 up to 10^21 ({@code 3}, {@code 0.1}, {@code 12345678.5}), otherwise with an exponent
	 * ({@code 1e+21}, {@code 1e-7}, {@code 1.5e+300}); {@code -0} is {@code 0}.
	 */
	@Override
	public String toString() {
		return format(value);
	}

	public static String format(double value) {
		if (Double.isNaN(value))
			return "NaN";
		if (Double.isInfinite(value))
			return value > 0 ? "Infinity" : "-Infinity";
		if (value == 0)
			return "0";
		if (value < 0)
			return "-" + format(-value);
		// Since Java 19, Double.toString gives the shortest digits; only their layout differs
		BigDecimal decimal = new BigDecimal(Double.toString(value)).stripTrailingZeros();
		// ... and at least two of them: for tiny subnormals (4.9e-324), one digit may already suffice
		if (decimal.precision() == 2) {
			BigDecimal oneDigit = decimal.round(new MathContext(1, RoundingMode.HALF_EVEN));
			if (oneDigit.doubleValue() == value)
				decimal = oneDigit.stripTrailingZeros();
		}
		String digits = decimal.unscaledValue().toString();
		int k = digits.length();
		// value = 0.digits * 10^n
		int n = k - decimal.scale();
		if (k <= n && n <= 21)
			return digits + "0".repeat(n - k);
		if (0 < n && n <= 21)
			return digits.substring(0, n) + "." + digits.substring(n);
		if (-6 < n && n <= 0)
			return "0." + "0".repeat(-n) + digits;
		int exponent = n - 1;
		String mantissa = k == 1 ? digits : digits.charAt(0) + "." + digits.substring(1);
		return mantissa + "e" + (exponent < 0 ? "-" : "+") + Math.abs(exponent);
	}
}
