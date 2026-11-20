package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptStackOverflowError;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Errors a script can run into should be tinyscript errors with a helpful message
 * (step 5 of the Advent of Code plan in TODO.md).
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
        assertTypeError("Object has no method 'f'", "var o = {}; o.f();");
        assertTypeError("Map has no method 'push'", "new Map().push(1);");
        assertTypeError("Car has no method 'drive'", "function Car() { } var c = new Car(); c.drive();");
        assertTypeError("Object has no method 'g'", "var o = {}; o[\"g\"]();");
    }

    @Test
    public void testObjectsKnowTheirConstructor() {
        String code = "function Car() { } var c = new Car();\n"
                + "print(c.constructor == Car, \" \", Object.keys(Car.prototype).length, \" \", Car.prototype.constructor == Car, \" \", {}.constructor == Object);\n"
                + "print(c.toString());";
        assertEquals("true 0 true true\n[object Object]\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testCallingAPropertyThatIsNoFunction() {
        assertTypeError("'x' of Object is a number, not a function", "var o = { x: 1 }; o.x();");
        assertTypeError("'length' of String is a number, not a function", "\"abc\".length();");
    }

    @Test
    public void testCallingAVariableThatIsNoFunction() {
        assertTypeError("'f' is a number, not a function", "var f = 1; f();");
        assertTypeError("'f' is undefined, not a function", "var f; f();");
        assertTypeError("Only functions can be called, not string", "(\"a\")();");
    }

    // =========================================================================
    // Script arguments
    // =========================================================================

    @Test
    public void testSystemArgsIsEmptyByDefault() {
        String code = "print(System.args.length, \" \", Array.isArray(System.args));";
        assertEquals("0 true\n", interpreterOutput(code));
        assertSameOutput(code);
    }
}
