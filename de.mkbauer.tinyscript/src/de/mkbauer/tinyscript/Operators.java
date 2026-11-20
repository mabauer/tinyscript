package de.mkbauer.tinyscript;

import de.mkbauer.tinyscript.ts.BinaryOperator;
import de.mkbauer.tinyscript.ts.ExpressionStatement;
import de.mkbauer.tinyscript.ts.IncrementOperator;

/**
 * What the operators of the syntax tree (the enums BinaryOperator, UnaryOperator and
 * IncrementOperator, generated from the grammar) mean, in one place.
 */
public final class Operators {

	private Operators() {
	}

	/**
	 * The operator of x op= y, e.g. ADD for +=; null for = and the other operators.
	 */
	public static BinaryOperator baseOf(BinaryOperator op) {
		return switch (op) {
			case ADD_ASSIGN -> BinaryOperator.ADD;
			case SUBTRACT_ASSIGN -> BinaryOperator.SUBTRACT;
			case MULTIPLY_ASSIGN -> BinaryOperator.MULTIPLY;
			case DIVIDE_ASSIGN -> BinaryOperator.DIVIDE;
			case REMAINDER_ASSIGN -> BinaryOperator.REMAINDER;
			case BIT_AND_ASSIGN -> BinaryOperator.BIT_AND;
			case BIT_OR_ASSIGN -> BinaryOperator.BIT_OR;
			case BIT_XOR_ASSIGN -> BinaryOperator.BIT_XOR;
			case SHIFT_LEFT_ASSIGN -> BinaryOperator.SHIFT_LEFT;
			case SHIFT_RIGHT_ASSIGN -> BinaryOperator.SHIFT_RIGHT;
			default -> null;
		};
	}

	/**
	 * = and the compound assignments like +=
	 */
	public static boolean isAssignment(BinaryOperator op) {
		return op == BinaryOperator.ASSIGN || baseOf(op) != null;
	}

	/**
	 * &, |, ^, << and >> (tinyscript's work on 53 bits, Javascript's on 32)
	 */
	public static boolean isBitwise(BinaryOperator op) {
		return switch (op) {
			case BIT_AND, BIT_OR, BIT_XOR, SHIFT_LEFT, SHIFT_RIGHT -> true;
			default -> false;
		};
	}

	/**
	 * The ++ or -- of i++; or i--;, null for an expression statement without one.
	 */
	public static IncrementOperator increment(ExpressionStatement statement) {
		return statement.getIncrement() != null ? statement.getIncrement().getOp() : null;
	}

}
