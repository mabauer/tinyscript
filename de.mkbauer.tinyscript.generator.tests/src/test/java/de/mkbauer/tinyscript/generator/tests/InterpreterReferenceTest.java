package de.mkbauer.tinyscript.generator.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.TinyscriptRuntimeException;

/**
 * Interpreter reference tests, organized by language feature.
 *
 * <p>Each {@code assertEquals(expected, interpreterOutput(tsCode))} call gives
 * {@code expected:<x\n> but was:<y\n>} on failure — the exact wrong output
 * points directly to the buggy interpreter node in {@code TinyscriptEngine}.
 *
 * <p>These tests never require node.js and are never skipped.
 */
public class InterpreterReferenceTest extends TinyscriptTranspilerTestBase {

    // =========================================================================
    // Expressions and operators
    // =========================================================================

    @Test
    public void testArithmeticExpressions() {
        assertEquals("5\n",   interpreterOutput("print(2 + 3);"));
        assertEquals("2.5\n", interpreterOutput("print(10 / 4);"));
        assertEquals("4\n",   interpreterOutput("print(7 - 3);"));
        assertEquals("12\n",  interpreterOutput("print(3 * 4);"));
        assertEquals("1\n",   interpreterOutput("print(10 % 3);"));
    }

    @Test
    public void testStringCoercion() {
        assertEquals("1hello\n", interpreterOutput("print(1 + \"hello\");"));
        assertEquals("hello1\n", interpreterOutput("print(\"hello\" + 1);"));
        assertEquals("hello world\n", interpreterOutput("print(\"hello\" + \" world\");"));
    }

    @Test
    public void testComparisonOperators() {
        assertEquals("true\n",  interpreterOutput("print(5 > 3);"));
        assertEquals("false\n", interpreterOutput("print(5 < 3);"));
        assertEquals("true\n",  interpreterOutput("print(5 == 5);"));
        assertEquals("false\n", interpreterOutput("print(5 == 6);"));
        assertEquals("true\n",  interpreterOutput("print(5 != 6);"));
        assertEquals("true\n",  interpreterOutput("print(5 >= 5);"));
        assertEquals("true\n",  interpreterOutput("print(5 <= 5);"));
    }

    @Test
    public void testLogicalOperators() {
        assertEquals("true\n",  interpreterOutput("print(true && true);"));
        assertEquals("false\n", interpreterOutput("print(true && false);"));
        assertEquals("true\n",  interpreterOutput("print(false || true);"));
        assertEquals("false\n", interpreterOutput("print(false || false);"));
        assertEquals("false\n", interpreterOutput("print(!true);"));
        assertEquals("true\n",  interpreterOutput("print(!false);"));
    }

    @Test
    public void testUnaryMinus() {
        assertEquals("-5\n", interpreterOutput("print(-5);"));
        assertEquals("-5\n", interpreterOutput("var x = 5; print(-x);"));
    }

    // =========================================================================
    // Control structures — if/else
    // =========================================================================

    @Test
    public void testIfElse() {
        assertEquals("big\n",   interpreterOutput(
                "if (5 > 3) { print(\"big\"); } else { print(\"small\"); }"));
        assertEquals("small\n", interpreterOutput(
                "if (1 > 3) { print(\"big\"); } else { print(\"small\"); }"));
    }

    @Test
    public void testIfWithoutElse() {
        assertEquals("yes\n", interpreterOutput("if (true) { print(\"yes\"); }"));
        assertEquals("",      interpreterOutput("if (false) { print(\"yes\"); }"));
    }

    // =========================================================================
    // Control structures — numeric for loop
    // =========================================================================

    @Test
    public void testNumericForLoop() {
        assertEquals("1\n2\n3\n", interpreterOutput(
                "for (var i = 1, 3) { print(i); }"));
    }

    @Test
    public void testNumericForLoopSum() {
        assertEquals("6\n", interpreterOutput(
                "var sum = 0;\n"
                + "for (var i = 1, 3) { sum = sum + i; }\n"
                + "print(sum);"));
    }

    // =========================================================================
    // Control structures — for-in (iterable)
    // =========================================================================

    @Test
    public void testForInArray() {
        assertEquals("a\nb\nc\n", interpreterOutput(
                "var arr = [\"a\", \"b\", \"c\"];\n"
                + "for (var x : arr) { print(x); }"));
    }

    @Test
    public void testForInArrayNumbers() {
        assertEquals("1\n2\n3\n", interpreterOutput(
                "var arr = [1, 2, 3];\n"
                + "for (var x : arr) { print(x); }"));
    }

    // =========================================================================
    // Functions
    // =========================================================================

    @Test
    public void testFunctionDeclarationAndCall() {
        assertEquals("6\n", interpreterOutput(
                "function double(x) { return x * 2; }\n"
                + "print(double(3));"));
    }

    @Test
    public void testRecursiveFactorial() {
        assertEquals("120\n", interpreterOutput(
                "function f(n) { if (n <= 1) { return 1; } return n * f(n-1); }\n"
                + "print(f(5));"));
    }

    @Test
    public void testRecursiveFibonacci() {
        assertEquals("8\n", interpreterOutput(
                "function fibonacci(n) {\n"
                + "  if (n == 0 || n == 1) { return 1; }\n"
                + "  else { return fibonacci(n-2) + fibonacci(n-1); }\n"
                + "}\n"
                + "print(fibonacci(5));"));
    }

    @Test
    public void testFunctionExpression() {
        assertEquals("9\n", interpreterOutput(
                "var square = function(x) { return x * x; };\n"
                + "print(square(3));"));
    }

    // =========================================================================
    // Arrow functions
    // =========================================================================

    @Test
    public void testArrowFunctionSingleParam() {
        assertEquals("6\n", interpreterOutput("var f = x => x * 2; print(f(3));"));
    }

    @Test
    public void testArrowFunctionParenthesisedParam() {
        assertEquals("6\n", interpreterOutput("var f = (x) => x * 2; print(f(3));"));
    }

    @Test
    public void testArrowFunctionMultipleParams() {
        assertEquals("7\n", interpreterOutput(
                "var add = (a, b) => a + b; print(add(3, 4));"));
    }

    // =========================================================================
    // Closures and lexical scope
    // =========================================================================

    @Test
    public void testClosure() {
        assertEquals("5\n", interpreterOutput(
                "function makeAdder(x) { return function(y) { return x + y; }; }\n"
                + "print(makeAdder(3)(2));"));
    }

    @Test
    public void testClosureCounter() {
        assertEquals("3\n", interpreterOutput(
                "var makeCounter = function() {\n"
                + "  var value = 0;\n"
                + "  return {\n"
                + "    inc: function() { value = value + 1; },\n"
                + "    val: function() { return value; }\n"
                + "  };\n"
                + "};\n"
                + "var c = makeCounter();\n"
                + "c.inc(); c.inc(); c.inc();\n"
                + "print(c.val());"));
    }

    // =========================================================================
    // Block scoping
    // =========================================================================

    @Test
    public void testBlockScoping() {
        assertEquals("10\n5\n", interpreterOutput(
                "var x = 5;\n"
                + "{ var x = 10; print(x); }\n"
                + "print(x);"));
    }

    // =========================================================================
    // Objects and prototypal inheritance
    // =========================================================================

    @Test
    public void testObjectLiteral() {
        assertEquals("42\n", interpreterOutput(
                "var obj = { x: 42 };\n"
                + "print(obj.x);"));
    }

    @Test
    public void testPrototypalInheritance() {
        assertEquals("42\n", interpreterOutput(
                "var proto = { getX: function() { return this.x; } };\n"
                + "var obj = Object.create(proto);\n"
                + "obj.x = 42;\n"
                + "print(obj.getX());"));
    }

    @Test
    public void testObjectPropertyAssignment() {
        assertEquals("hello\n", interpreterOutput(
                "var obj = {};\n"
                + "obj.name = \"hello\";\n"
                + "print(obj.name);"));
    }

    // =========================================================================
    // Arrays
    // =========================================================================

    @Test
    public void testArrayLiteral() {
        assertEquals("1\n", interpreterOutput(
                "var arr = [1, 2, 3];\n"
                + "print(arr[0]);"));
    }

    @Test
    public void testArrayLength() {
        assertEquals("3\n", interpreterOutput(
                "var arr = [1, 2, 3];\n"
                + "print(arr.length);"));
    }

    @Test
    public void testArrayPushPop() {
        assertEquals("4\n3\n", interpreterOutput(
                "var arr = [1, 2, 3];\n"
                + "arr.push(4);\n"
                + "print(arr.pop());\n"
                + "print(arr.length);"));
    }

    // =========================================================================
    // Error / exception cases
    // =========================================================================

    @Test
    public void testAssertFalseThrows() {
        assertInterpreterThrows(TinyscriptAssertationError.class, "assert(false);");
    }

    @Test
    public void testAssertTrueDoesNotThrow() {
        // Should complete without error
        interpreterOutput("assert(true);");
    }
}
