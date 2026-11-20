package de.mkbauer.tinyscript.runtime.number;

import java.util.regex.Pattern;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSBoolean;
import de.mkbauer.tinyscript.interpreter.TSNumber;
import de.mkbauer.tinyscript.interpreter.TSString;
import de.mkbauer.tinyscript.interpreter.TSUndefined;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.string.StringConstructor;

/**
 * The explicit conversions to numbers: the global functions Number(value), parseInt(string[, radix])
 * and isNaN(number), and Number.isInteger(value). Number is a function, not a constructor.
 */
public final class NumberFunctions {

	// A decimal number literal as Javascript reads it from a string, e.g. "-1.5e3", ".5", "Infinity"
	private static final Pattern DECIMAL = Pattern.compile("[+-]?(Infinity|(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?)");
	private static final Pattern NON_DECIMAL = Pattern.compile("0([xX][0-9a-fA-F]+|[oO][0-7]+|[bB][01]+)");

	private NumberFunctions() {
	}

	public static BuiltinFunction createNumber(TinyscriptEngine engine) {
		BuiltinFunction number = engine.defineBuiltinFunction("Number", (self, args) -> toNumber(args[0]), 1);
		number.defineBuiltinMethod("isInteger", (self, args) -> TSValue.of(args[0].isMathematicalInteger() && Double.isFinite(args[0].asDouble())), 1);
		return number;
	}

	public static BuiltinFunction createParseInt(TinyscriptEngine engine) {
		return engine.defineBuiltinFunction("parseInt", (self, args) -> parseInt(args), 1, 2);
	}

	public static BuiltinFunction createIsNaN(TinyscriptEngine engine) {
		return engine.defineBuiltinFunction("isNaN", (self, args) -> {
			if (!(args[0] instanceof TSNumber number))
				throw new TinyscriptTypeError("isNaN() needs a number, not " + args[0].typeOf());
			return TSValue.of(Double.isNaN(number.value()));
		}, 1);
	}

	// Number(value): numbers stay as they are, booleans are 1 or 0 and strings are read as a whole
	// number literal; unlike Javascript, a blank string is NaN (not 0) and anything else is a TypeError
	private static TSValue toNumber(TSValue value) {
		return switch (value) {
			case TSNumber number -> number;
			case TSBoolean b -> TSValue.of(b.value() ? 1 : 0);
			case TSString s -> TSValue.of(parseNumber(StringConstructor.trim(s.value())));
			default -> throw new TinyscriptTypeError("Number() needs a string, a number or a boolean, not " + value.typeOf());
		};
	}

	private static double parseNumber(String str) {
		if (DECIMAL.matcher(str).matches())
			return Double.parseDouble(str);
		if (NON_DECIMAL.matcher(str).matches()) {
			int radix = switch (str.charAt(1)) {
				case 'x', 'X' -> 16;
				case 'o', 'O' -> 8;
				default -> 2;
			};
			return digits(str.substring(2), radix);
		}
		return Double.NaN;
	}

	// parseInt(string[, radix]): like Javascript, reads the leading integer and ignores the rest,
	// e.g. "42abc" is 42; NaN if there is none. The radix must be an integer from 2 to 36.
	private static TSValue parseInt(TSValue[] args) {
		if (!(args[0] instanceof TSString string))
			throw new TinyscriptTypeError("parseInt() needs a string, not " + args[0].typeOf());
		int radix = 10;
		boolean radixGiven = args.length > 1 && !(args[1] instanceof TSUndefined);
		if (radixGiven) {
			if (!args[1].isMathematicalInteger() || args[1].asDouble() < 2 || args[1].asDouble() > 36)
				throw new TinyscriptTypeError("parseInt() needs a radix from 2 to 36, not " + args[1].asString());
			radix = (int) args[1].asDouble();
		}
		String str = StringConstructor.trim(string.value());
		double sign = 1;
		if (str.startsWith("-") || str.startsWith("+")) {
			sign = str.startsWith("-") ? -1 : 1;
			str = str.substring(1);
		}
		if ((radix == 16 || !radixGiven) && (str.startsWith("0x") || str.startsWith("0X"))) {
			radix = 16;
			str = str.substring(2);
		}
		int end = 0;
		while (end < str.length() && digit(str.charAt(end), radix) >= 0)
			end++;
		return TSValue.of(end == 0 ? Double.NaN : sign * digits(str.substring(0, end), radix));
	}

	private static double digits(String digits, int radix) {
		double result = 0;
		for (int i = 0; i < digits.length(); i++)
			result = result * radix + digit(digits.charAt(i), radix);
		return result;
	}

	// Only ASCII digits and letters; Character.digit would also accept other Unicode digits
	private static int digit(char c, int radix) {
		return c < 128 ? Character.digit(c, radix) : -1;
	}

}
