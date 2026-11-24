package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * tinyscript is deliberately stricter than Javascript: no silent conversions
 * (see "Design decisions" in the README).
 */
public class StrictnessTest extends TinyscriptTranspilerTestBase {

    // =========================================================================
    // Operators only with matching types
    // =========================================================================

    @Test
    public void testArithmeticNeedsNumbers() {
        assertKnownGap("print(1 - \"a\");");
        assertKnownGap("print(1 * true);");
        assertKnownGap("print(\"a\" * 2);");
        assertKnownGap("let u; print(u / 2);");
        assertKnownGap("print(null % 2);");
    }

    @Test
    public void testArithmeticWithNaNStaysNaN() {
        String code = "let nan = 0 / 0; print(nan - 1); print(nan * 2); print(-nan); print(-(-3));";
        assertEquals("NaN\nNaN\nNaN\n3\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testPlusNeedsMatchingTypes() {
        assertKnownGap("print(1 + true);");
        assertKnownGap("print(1 + null);");
        assertKnownGap("let u; print(u + 1);");
        assertKnownGap("print({} + 1);");
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
        assertSameFailure("let s = \"abc\"; s[1] = \"x\";");
        assertSameFailure("let s = \"abc\"; s.x = 1;");
    }

    // =========================================================================
    // Builtins do not convert silently
    // =========================================================================

    @Test
    public void testBuiltinsNeedArgumentsOfTheRightType() {
        assertSameFailure("print(Math.sqrt(\"4\"));");
        assertSameFailure("print(Math.max(\"3\", 1));");
        assertSameFailure("let u; print(Math.abs(u));");
        assertSameFailure("print(\"abc\".charAt(\"1\"));");
        assertSameFailure("print(\"abc\".substring(1.5));");
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
                + "const o = {}; print(!null); print(!o); print(null || true); print(o && false);";
        assertEquals("tfffttt\n1\ntrue\nfalse\ntrue\nfalse\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNumbersAndStringsAreNoConditions() {
        assertKnownGap("if (1) { print(1); }");
        assertKnownGap("if (\"\") { print(1); }");
        assertKnownGap("let a = [1]; while (a.length) { a.pop(); }");
        assertKnownGap("print(!0);");
        assertKnownGap("const one = 1; print(true && one);");
        assertKnownGap("const a = \"a\"; print(a || true);");
        assertKnownGap("assert(1);");
        assertKnownGap("print([1, 2].filter(x => x - 1).length);");
        TinyscriptRuntimeException e = assertThrows(TinyscriptTypeError.class, () -> interpreterOutput("let count = 0;\nif (count) { print(1); }"));
        assertTrue(e.getMessage().contains("The condition count is a number, not a boolean"), e.getMessage());
        assertEquals(2, e.getAffectedLine());
    }

    @Test
    public void testLogicalOperatorsShortCircuit() {
        // The right operand is not evaluated, so it is not checked either (as a literal, 1 or "x"
        // would be a compile-time error, see ErrorMessagesTest)
        assertEquals("false\ntrue\n", interpreterOutput("const one = 1; const x = \"x\"; print(false && one); print(true || x);"));
    }
}
