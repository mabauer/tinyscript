package de.mkbauer.tinyscript.interpreter;

import java.util.Objects;

/**
 * A primitive string. {@link #toString()} quotes it, {@link #asString()} does not.
 */
public record TSString(String value) implements TSValue {

	public TSString {
		Objects.requireNonNull(value);
	}

	/**
	 * The string as a string literal, as print shows it inside arrays and objects: "a\"b".
	 */
	@Override
	public String toString() {
		return quote(value);
	}

	/**
	 * A string in double quotes, escaped exactly as Javascript's JSON.stringify does it (so the
	 * generated Javascript prints the same): \" \\ \b \f \n \r \t, other control characters and
	 * lone surrogates as \\u00xx; everything else as it is.
	 */
	public static String quote(String value) {
		StringBuilder result = new StringBuilder(value.length() + 2).append('"');
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '"' -> result.append("\\\"");
				case '\\' -> result.append("\\\\");
				case '\b' -> result.append("\\b");
				case '\f' -> result.append("\\f");
				case '\n' -> result.append("\\n");
				case '\r' -> result.append("\\r");
				case '\t' -> result.append("\\t");
				default -> {
					boolean pair = Character.isHighSurrogate(c) && i + 1 < value.length() && Character.isLowSurrogate(value.charAt(i + 1));
					boolean lone = Character.isSurrogate(c) && !pair && !(Character.isLowSurrogate(c) && i > 0 && Character.isHighSurrogate(value.charAt(i - 1)));
					if (c < 0x20 || lone)
						result.append(String.format("\\u%04x", (int) c));
					else
						result.append(c);
				}
			}
		}
		return result.append('"').toString();
	}
}
