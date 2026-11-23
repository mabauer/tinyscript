package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import de.mkbauer.tinyscript.TSStacktraceElement;
import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptStackOverflowError;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Errors a script can run into should be tinyscript errors with a helpful message.
 */
public class RobustnessTest extends TinyscriptTranspilerTestBase {

    // =========================================================================
    // Too deep recursion
    // =========================================================================

    @Test
    @Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testEndlessRecursionIsAStackOverflowError() {
        TinyscriptRuntimeException e = assertThrows(TinyscriptStackOverflowError.class,
                () -> interpreterOutput("function f(n) { return f(n + 1); }\nf(0);"));
        assertTrue(e.getMessage().startsWith("Stack overflow"), e.getMessage());
        // The stack trace is shortened, not thousands of lines
        long lines = e.getTinyscriptStacktraceAsString().lines().count();
        assertTrue(lines > 0 && lines < 30, e.getTinyscriptStacktraceAsString());
    }

    // "at f(4) at g(3) at global(5)": each call with its function and line
    private static String frames(TinyscriptRuntimeException e) {
        StringBuilder result = new StringBuilder();
        for (TSStacktraceElement frame : e.getTinyscriptStacktrace())
            result.append(result.isEmpty() ? "at " : " at ").append(frame.getFunctionName() + "(" + frame.getLine() + ")");
        return result.toString();
    }

    @Test
    public void testStackFramesAreNamedAfterTheFunctionsAlsoInBlocks() {
        // The calls are in blocks with variables of their own (which run in contexts of their own)
        TinyscriptRuntimeException e = assertThrows(TinyscriptTypeError.class, () -> interpreterOutput(
                "function g(x) {\n"
                + "    if (x > 0) {\n"
                + "        const y = x * 2;\n"
                + "        return h(y);\n"
                + "    }\n"
                + "}\n"
                + "function h(x) {\n"
                + "    for (const i : range(1)) {\n"
                + "        return x * \"a\";\n"
                + "    }\n"
                + "}\n"
                + "if (true) {\n"
                + "    const z = 1;\n"
                + "    g(z);\n"
                + "}\n"));
        assertEquals("at h(9) at g(4) at global(14)", frames(e));
    }

    @Test
    @Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testStackOverflowInABlockIsNamedAfterTheFunction() {
        TinyscriptRuntimeException e = assertThrows(TinyscriptStackOverflowError.class, () -> interpreterOutput(
                "function valueOf(n) {\n"
                + "    if (n >= 0) {\n"
                + "        const next = n + 1;\n"
                + "        return valueOf(next);\n"
                + "    }\n"
                + "}\n"
                + "valueOf(0);\n"));
        assertTrue(frames(e).startsWith("at valueOf("), frames(e));
        assertTrue(frames(e).contains(" at valueOf(4) "), frames(e));
        assertTrue(frames(e).endsWith(" at global(7)"), frames(e));
    }

    @Test
    @Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testStackOverflowInACallback() {
        assertThrows(TinyscriptStackOverflowError.class,
                () -> interpreterOutput("function f(n) { return [n].map(x => f(x + 1)); }\nf(0);"));
    }

    // =========================================================================
    // Calling something that is not a function
    // =========================================================================

    private void assertTypeError(String expectedMessage, String code) {
        TinyscriptRuntimeException e = assertThrows(TinyscriptTypeError.class, () -> interpreterOutput(code));
        assertEquals(expectedMessage, e.getMessage());
    }

    @Test
    public void testCallingAMissingMethod() {
        assertTypeError("String has no method 'splitt'", "\"a,b\".splitt(\",\");");
        assertTypeError("Array has no method 'foo'", "[1].foo();");
        assertTypeError("Object has no method 'f'", "let o = {}; o.f();");
        assertTypeError("Map has no method 'push'", "new Map().push(1);");
        assertTypeError("Car has no method 'drive'", "function Car() { } let c = new Car(); c.drive();");
        assertTypeError("Object has no method 'g'", "let o = {}; o[\"g\"]();");
        assertTypeError("Point has no method 'move'", "class Point { } new Point().move();");
        // A range has only some of the methods of arrays: the message says how to get the others
        assertTypeError("range has no method 'filter': for the methods of arrays, make it an array with toArray(): range(…).toArray().filter(…)",
                "range(5).filter(i => i > 2);");
        assertTypeError("range has no method 'foo'", "range(5).foo();");
        // A method that is only called constructor does not name the object
        assertTypeError("Object has no method 'f'", "const o = { constructor() { } }; o.f();");
        assertTypeError("Object has no method 'f'", "const base = { constructor() { } }; Object.create(base).f();");
    }

    @Test
    public void testObjectsKnowTheirConstructor() {
        String code = "function Car() { } let c = new Car();\n"
                + "print(c.constructor == Car, Object.keys(Car.prototype).length, Car.prototype.constructor == Car, {}.constructor == Object);\n"
                + "print(c.toString());";
        assertEquals("true 0 true true\n{}\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testCallingAPropertyThatIsNoFunction() {
        assertTypeError("'x' of Object is a number, not a function", "let o = { x: 1 }; o.x();");
        assertTypeError("'length' of String is a number, not a function", "\"abc\".length();");
    }

    @Test
    public void testCallingAVariableThatIsNoFunction() {
        assertTypeError("'f' is a number, not a function", "let f = 1; f();");
        assertTypeError("'f' is undefined, not a function", "let f; f();");
        assertTypeError("Only functions can be called, not string", "(\"a\")();");
    }

    // =========================================================================
    // Script arguments
    // =========================================================================

    @Test
    public void testSystemArgsIsEmptyByDefault() {
        String code = "print(System.args.length, Array.isArray(System.args));";
        assertEquals("0 true\n", interpreterOutput(code));
        assertSameOutput(code);
    }
}
