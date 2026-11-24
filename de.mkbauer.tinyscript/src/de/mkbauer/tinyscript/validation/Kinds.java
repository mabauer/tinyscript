package de.mkbauer.tinyscript.validation;

import de.mkbauer.tinyscript.ts.ArrayInitializer;
import de.mkbauer.tinyscript.ts.BinaryExpression;
import de.mkbauer.tinyscript.ts.BooleanLiteral;
import de.mkbauer.tinyscript.ts.CallOrPropertyAccess;
import de.mkbauer.tinyscript.ts.CallSuffix;
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.FunctionDefinition;
import de.mkbauer.tinyscript.ts.GroupingExpression;
import de.mkbauer.tinyscript.ts.NewExpression;
import de.mkbauer.tinyscript.ts.NullObject;
import de.mkbauer.tinyscript.ts.NumberLiteral;
import de.mkbauer.tinyscript.ts.ObjectInitializer;
import de.mkbauer.tinyscript.ts.StringLiteral;
import de.mkbauer.tinyscript.ts.Unary;
import de.mkbauer.tinyscript.ts.UndefinedLiteral;

/**
 * The kind of value an expression has, from the syntax alone (no type inference): literals, and
 * the results of operators that guarantee their kind because they check their operands (a - b is
 * a number or a TypeError before). Null where only the run shows it (variables, properties,
 * calls). For the checks before the run of operators and conditions.
 */
final class Kinds {

	static final String NUMBER = "a number", STRING = "a string", BOOLEAN = "a boolean", ARRAY = "an array",
			OBJECT = "an object", FUNCTION = "a function", NULL = "null", UNDEFINED = "undefined";

	private Kinds() {
	}

	static String kind(Expression expression) {
		while (expression instanceof GroupingExpression grouping)
			expression = grouping.getExpr();
		return switch (expression) {
			case null -> null;
			case NumberLiteral literal -> NUMBER;
			case StringLiteral literal -> STRING;
			case BooleanLiteral literal -> BOOLEAN;
			case NullObject literal -> NULL;
			case UndefinedLiteral literal -> UNDEFINED;
			case ArrayInitializer literal -> ARRAY;
			case ObjectInitializer literal -> OBJECT;
			case FunctionDefinition function -> FUNCTION;
			case NewExpression creation -> OBJECT;
			// new Map() is the call of new Map
			case CallOrPropertyAccess call when call.getSuffix() instanceof CallSuffix && call.getExpr() instanceof NewExpression -> OBJECT;
			case Unary unary -> switch (unary.getOp()) {
				case NEGATE, BIT_NOT -> NUMBER;
				case NOT, DELETE -> BOOLEAN;
				case TYPEOF -> STRING;
			};
			case BinaryExpression binary -> switch (binary.getOp()) {
				case SUBTRACT, MULTIPLY, DIVIDE, REMAINDER, BIT_AND, BIT_OR, BIT_XOR, SHIFT_LEFT, SHIFT_RIGHT -> NUMBER;
				case LESS, LESS_OR_EQUAL, GREATER, GREATER_OR_EQUAL, EQUALS, NOT_EQUALS, INSTANCEOF, AND, OR -> BOOLEAN;
				case ADD -> addition(kind(binary.getLeft()), kind(binary.getRight()));
				case ASSIGN -> kind(binary.getRight());
				default -> null;
			};
			default -> null;
		};
	}

	// + gives a string if one side is a string, a number for two numbers, an array for two arrays
	private static String addition(String left, String right) {
		if (STRING.equals(left) || STRING.equals(right))
			return STRING;
		if (NUMBER.equals(left) && NUMBER.equals(right))
			return NUMBER;
		if (ARRAY.equals(left) && ARRAY.equals(right))
			return ARRAY;
		return null;
	}
}
