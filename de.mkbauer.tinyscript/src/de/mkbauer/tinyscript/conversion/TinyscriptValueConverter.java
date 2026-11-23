package de.mkbauer.tinyscript.conversion;

import java.math.BigInteger;
import java.util.Map;

import org.eclipse.xtext.common.services.DefaultTerminalConverters;
import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.nodemodel.INode;

import de.mkbauer.tinyscript.interpreter.TSNumber;

/**
 * The values of number literals as in Javascript: decimal (3.14, 1e-9), hexadecimal (0xff),
 * binary (0b1010) and octal (0o17), each with separators between digits (1_000_000). Large
 * hexadecimal, binary and octal numbers round to the nearest double, as in Javascript.
 */
public class TinyscriptValueConverter extends DefaultTerminalConverters {

	@ValueConverter(rule = "DOUBLE")
	public IValueConverter<Double> DOUBLE() {
		return new IValueConverter<Double>() {
			@Override
			public Double toValue(String string, INode node) {
				if (string == null || string.isEmpty())
					throw new ValueConverterException("A number is missing", node, null);
				return parse(string, node);
			}

			@Override
			public String toString(Double value) {
				return TSNumber.format(value);
			}
		};
	}

	static Double parse(String text, INode node) {
		boolean prefixed = text.length() > 1 && text.charAt(0) == '0' && "xXbBoO".indexOf(text.charAt(1)) >= 0;
		int radix = !prefixed ? 10 : switch (Character.toLowerCase(text.charAt(1))) {
			case 'x' -> 16;
			case 'b' -> 2;
			default -> 8;
		};
		String digits = prefixed ? text.substring(2) : text;
		if (digits.isEmpty())
			throw error(text + " needs digits: " + EXAMPLES.get(radix), node);
		for (int i = 0; i < digits.length(); i++) {
			char c = digits.charAt(i);
			if (c == '_' && !(i > 0 && isDigit(digits.charAt(i - 1), radix) && i + 1 < digits.length()
					&& isDigit(digits.charAt(i + 1), radix)))
				throw error("A _ in a number must stand between two digits, as in 1_000_000", node);
			if (prefixed && c != '_' && !isDigit(c, radix))
				throw error(text + ": " + NAMES.get(radix) + " number has only the digits " + DIGITS.get(radix), node);
		}
		// 017 is the octal number 15 in Javascript (outside of strict mode), so it is no number here
		if (!prefixed && text.length() > 1 && text.charAt(0) == '0' && (Character.isDigit(text.charAt(1)) || text.charAt(1) == '_')) {
			String plain = text.replace("_", "");
			String decimal = plain.replaceFirst("^0+(?=[0-9])", "");
			throw error(text + ": a number cannot start with 0 (in Javascript, it may be an octal number): write " + decimal
					+ (plain.matches("[0-7]+") ? ", or 0o" + plain.substring(1) + " for an octal number" : ""), node);
		}
		try {
			if (prefixed)
				return new BigInteger(digits.replace("_", ""), radix).doubleValue();
			return Double.valueOf(digits.replace("_", ""));
		}
		catch (NumberFormatException e) {
			throw error("Not a number: " + text, node);
		}
	}

	private static boolean isDigit(char c, int radix) {
		return Character.digit(c, radix) >= 0;
	}

	private static ValueConverterException error(String message, INode node) {
		return new ValueConverterException(message, node, null);
	}

	private static final Map<Integer, String> EXAMPLES = Map.of(16, "0xff", 2, "0b1010", 8, "0o17");
	private static final Map<Integer, String> NAMES = Map.of(16, "a hexadecimal", 2, "a binary", 8, "an octal");
	private static final Map<Integer, String> DIGITS = Map.of(16, "0 to 9 and a to f", 2, "0 and 1", 8, "0 to 7");
}
