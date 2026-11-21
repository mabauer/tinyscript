package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * tinyscript is deliberately stricter than Javascript: no silent conversions
 * (see "Conversions & strictness" in TODO.md and the README).
 */
public class StrictnessTest extends TinyscriptTranspilerTestBase {

    // =========================================================================
    // Operators only with matching types
    // =========================================================================

    @Test
    public void testArithmeticNeedsNumbers() {
        assertInterpreterThrows(TinyscriptTypeError.class, "print(1 - \"a\");");
        assertInterpreterThrows(TinyscriptTypeError.class, "print(1 * true);");
        assertInterpreterThrows(TinyscriptTypeError.class, "print(\"a\" * 2);");
        assertInterpreterThrows(TinyscriptTypeError.class, "let u; print(u / 2);");
        assertInterpreterThrows(TinyscriptTypeError.class, "print(null % 2);");
    }

    @Test
    public void testArithmeticWithNaNStaysNaN() {
        String code = "let nan = 0 / 0; print(nan - 1); print(nan * 2); print(-nan); print(-(-3));";
        assertEquals("NaN\nNaN\nNaN\n3\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testPlusNeedsMatchingTypes() {
        assertInterpreterThrows(TinyscriptTypeError.class, "print(1 + true);");
        assertInterpreterThrows(TinyscriptTypeError.class, "print(1 + null);");
        assertInterpreterThrows(TinyscriptTypeError.class, "let u; print(u + 1);");
        assertInterpreterThrows(TinyscriptTypeError.class, "print({} + 1);");
        String code = "let u; print(1 + 2); print(\"a\" + 1); print(1 + \"a\"); print(\"x\" + true); print(\"x\" + u); print(\"x\" + null);";
        assertEquals("3\na1\n1a\nxtrue\nxundefined\nxnull\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testUnaryMinusNeedsANumber() {
        TinyscriptRuntimeException e = assertThrows(TinyscriptTypeError.class, () -> interpreterOutput("print(-\"a\");"));
        assertTrue(e.getMessage().contains("'-' needs a number, but \"a\" is a string"), e.getMessage());
    }

    // =========================================================================
    // undefined / null and string conversion as in Javascript
    // =========================================================================

    @Test
    public void testUndefinedAndNullAreSpelledLikeJavascript() {
        String code = "let u; print(u); print(null); print(String(u) + String(null));";
        assertEquals("undefined\nnull\nundefinednull\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testStringConversionUsesAUserDefinedToString() {
        String code = "let o = { toString: function () { return \"O\"; } };\nprint(\"x\" + o); print(o + \"y\"); print(String(o));";
        assertEquals("xO\nOy\nO\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    // =========================================================================
    // Reading characters of a string
    // =========================================================================

    @Test
    public void testStringsCanBeIndexed() {
        String code = "let s = \"abc\"; print(s[1]); print(s[0] + s[2]); print(s[3] == null); print(s[-1] == null);";
        assertEquals("b\nac\ntrue\ntrue\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testStringsCannotBeChanged() {
        assertInterpreterThrows(TinyscriptTypeError.class, "let s = \"abc\"; s[1] = \"x\";");
        assertInterpreterThrows(TinyscriptTypeError.class, "let s = \"abc\"; s.x = 1;");
    }

    // =========================================================================
    // Builtins do not convert silently
    // =========================================================================

    @Test
    public void testBuiltinsNeedArgumentsOfTheRightType() {
        assertInterpreterThrows(TinyscriptTypeError.class, "print(Math.sqrt(\"4\"));");
        assertInterpreterThrows(TinyscriptTypeError.class, "print(Math.max(\"3\", 1));");
        assertInterpreterThrows(TinyscriptTypeError.class, "let u; print(Math.abs(u));");
        assertInterpreterThrows(TinyscriptTypeError.class, "print(\"abc\".charAt(\"1\"));");
        assertInterpreterThrows(TinyscriptTypeError.class, "print(\"abc\".substring(1.5));");
    }

    // =========================================================================
    // Conditions: a boolean, null / undefined (false) or an object (true)
    // =========================================================================

    @Test
    public void testConditionsAcceptBooleansNullAndObjects() {
        String code = "function t(x) { if (x) { return \"t\"; } else { return \"f\"; } }\n"
                + "let u; let node = { next: null };\n"
                + "print(t(true) + t(false) + t(null) + t(u) + t({}) + t([]) + t(t));\n"
                + "let n = 0; while (node) { n = n + 1; node = node.next; } print(n);\n"
                + "print(!null); print(!{}); print(null || true); print({} && false);";
        assertEquals("tfffttt\n1\ntrue\nfalse\ntrue\nfalse\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNumbersAndStringsAreNoConditions() {
        assertInterpreterThrows(TinyscriptTypeError.class, "if (1) { print(1); }");
        assertInterpreterThrows(TinyscriptTypeError.class, "if (\"\") { print(1); }");
        assertInterpreterThrows(TinyscriptTypeError.class, "let a = [1]; while (a.length) { a.pop(); }");
        assertInterpreterThrows(TinyscriptTypeError.class, "print(!0);");
        assertInterpreterThrows(TinyscriptTypeError.class, "print(true && 1);");
        assertInterpreterThrows(TinyscriptTypeError.class, "print(\"a\" || true);");
        assertInterpreterThrows(TinyscriptTypeError.class, "assert(1);");
        assertInterpreterThrows(TinyscriptTypeError.class, "print([1, 2].filter(x => x - 1).length);");
        TinyscriptRuntimeException e = assertThrows(TinyscriptTypeError.class, () -> interpreterOutput("let count = 0;\nif (count) { print(1); }"));
        assertTrue(e.getMessage().contains("The condition count is a number, not a boolean"), e.getMessage());
        assertEquals(2, e.getAffectedLine());
    }

    @Test
    public void testLogicalOperatorsShortCircuit() {
        // The right operand is not evaluated, so it is not checked either
        assertEquals("false\ntrue\n", interpreterOutput("print(false && 1); print(true || \"x\");"));
    }
}
