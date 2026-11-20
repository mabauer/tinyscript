package de.mkbauer.tinyscript.runtime.collection;

import de.mkbauer.tinyscript.interpreter.TSNumber;
import de.mkbauer.tinyscript.interpreter.TSValue;

/**
 * Keys of Maps and Sets are compared like in Javascript (SameValueZero): numbers, strings and
 * booleans by value, objects by identity, NaN equals NaN and -0 equals 0. The records TSNumber,
 * TSString and TSBoolean already compare by value (NaN included), so only -0 needs care.
 */
final class Keys {

	private Keys() {
	}

	static TSValue normalize(TSValue key) {
		return key instanceof TSNumber number && number.value() == 0 ? TSValue.of(0) : key;
	}

}
