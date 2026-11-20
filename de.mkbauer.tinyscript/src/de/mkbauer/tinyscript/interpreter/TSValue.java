package de.mkbauer.tinyscript.interpreter;

import de.mkbauer.tinyscript.runtime.string.StringObject;

/**
 * A value in Tinyscript: {@code undefined}, {@code null}, a boolean, a number,
 * a string or an object (including arrays and functions).
 *
 * <p>The interface is sealed, so a {@code switch} over a {@code TSValue} is
 * checked for exhaustiveness by the compiler.
 */
public sealed interface TSValue permits TSUndefined, TSNull, TSBoolean, TSNumber, TSString, TSObject {

	TSValue UNDEFINED = TSUndefined.INSTANCE;
	TSValue NULL = TSNull.INSTANCE;
	TSValue NAN = new TSNumber(Double.NaN);

	static TSValue of(double value) {
		return new TSNumber(value);
	}

	static TSValue of(boolean value) {
		return value ? TSBoolean.TRUE : TSBoolean.FALSE;
	}

	static TSValue of(String value) {
		return new TSString(value);
	}

	/**
	 * Converts a Java value (a {@code TSValue}, number, boolean or string) into a {@code TSValue}.
	 */
	static TSValue fromJava(Object value) {
		return switch (value) {
			case TSValue v -> v;
			case Number n -> of(n.doubleValue());
			case Boolean b -> of(b.booleanValue());
			case String s -> of(s);
			default -> throw new IllegalArgumentException("Not a Tinyscript value: " + value);
		};
	}

	default String asString() {
		return switch (this) {
			case TSUndefined u -> "undefined";
			case TSNull n -> "null";
			case TSBoolean b -> String.valueOf(b.value());
			case TSNumber n -> n.toString();
			case TSString s -> s.value();
			case TSObject o -> o.toString();
		};
	}

	default boolean asBoolean() {
		return switch (this) {
			case TSUndefined u -> false;
			case TSNull n -> false;
			case TSBoolean b -> b.value();
			case TSNumber n -> n.value() != 0.0 && !Double.isNaN(n.value());
			case TSString s -> !s.value().isEmpty();
			case TSObject o -> true;
		};
	}

	/**
	 * The value of a condition (if, while, !, &&, ||, assert, filter callbacks): a boolean,
	 * null / undefined (false) or an object (true). Numbers and strings are no conditions,
	 * because 0, "" and NaN would be false although they are valid values.
	 */
	default boolean asCondition() {
		return switch (this) {
			case TSBoolean b -> b.value();
			case TSUndefined u -> false;
			case TSNull n -> false;
			case TSObject o -> true;
			case TSNumber n -> throw new TinyscriptTypeError("Condition must be a boolean, not number");
			case TSString s -> throw new TinyscriptTypeError("Condition must be a boolean, not string");
		};
	}

	default String typeOf() {
		return switch (this) {
			case TSUndefined u -> "undefined";
			case TSNull n -> "object";
			case TSBoolean b -> "boolean";
			case TSNumber n -> "number";
			case TSString s -> "string";
			case Function f -> "function";
			case TSObject o -> "object";
		};
	}

	/**
	 * Tinyscript's {@code ==}: no type coercion, but {@code null == undefined},
	 * and numbers compare like in Javascript ({@code NaN != NaN}, {@code 0 == -0}).
	 */
	default boolean tinyscriptEquals(TSValue other) {
		if (this == UNDEFINED || this == NULL)
			return other == UNDEFINED || other == NULL;
		if (this instanceof TSNumber a && other instanceof TSNumber b)
			return a.value() == b.value();
		if (isString() && other.isString())
			return asString().equals(other.asString());
		return equals(other);
	}

	default boolean isString() {
		return this instanceof TSString || this instanceof StringObject;
	}

	default boolean isPrimitiveString() {
		return this instanceof TSString;
	}

	default boolean isNumber() {
		return this instanceof TSNumber n && !Double.isNaN(n.value());
	}

	default boolean isMathematicalInteger() {
		return this instanceof TSNumber n && Math.rint(n.value()) == n.value();
	}

	default double asDouble() {
		return ((TSNumber) this).value();
	}

	default int asInt() {
		return (int) asDouble();
	}

	default boolean isBoolean() {
		return this instanceof TSBoolean;
	}

	default boolean isObject() {
		return this instanceof TSObject;
	}

	default TSObject asObject() {
		return (TSObject) this;
	}
}
