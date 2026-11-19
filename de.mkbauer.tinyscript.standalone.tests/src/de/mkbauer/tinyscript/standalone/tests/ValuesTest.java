package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.interpreter.TSValue;

/**
 * Characterization tests for the semantics of values: {@code typeof}, truthiness,
 * equality and how numbers are printed.
 *
 * <p>They pin down the behavior before {@link TSValue} is restructured. Where
 * Javascript behaves the same, the snippets also run as differential tests.
 */
public class ValuesTest extends TinyscriptTranspilerTestBase {

    private static final String VALUES =
            "var u; var n = null; var b = true; var i = 1; var nan = 0 / 0; var s = \"s\";\n"
            + "var f = function() { return 1; }; var o = {}; var a = [];\n";

    @Test
    public void testTypeOf() {
        String code = VALUES
                + "print(typeof u); print(typeof n); print(typeof b); print(typeof i); print(typeof nan);\n"
                + "print(typeof s); print(typeof f); print(typeof o); print(typeof a);";
        assertEquals("undefined\nobject\nboolean\nnumber\nnumber\nstring\nfunction\nobject\nobject\n",
                interpreterOutput(code));
        // No differential test yet: the generator cannot translate typeof (see TODO.md)
    }

    @Test
    public void testTruthiness() {
        String code = "function t(x) { if (x) { return \"t\"; } else { return \"f\"; } }\n"
                + VALUES
                + "print(t(u) + t(n) + t(b) + t(false) + t(i) + t(0) + t(nan) + t(s) + t(\"\") + t(f) + t(o) + t(a));";
        assertEquals("fftftfftfttt\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNotAndLogicalOperatorsOnNaN() {
        String code = "var nan = 0 / 0; print(!nan); print(nan || false); print(nan && true);";
        assertEquals("true\nfalse\nfalse\n", interpreterOutput(code));
    }

    @Test
    public void testEquality() {
        String code = VALUES
                + "print(i == 1); print(s == \"s\"); print(n == null); print(u == u); print(o == o); print(f == f);\n"
                + "print(nan == nan); print(nan != nan); print({} == {}); print([] == []);";
        assertEquals("true\ntrue\ntrue\ntrue\ntrue\ntrue\nfalse\ntrue\nfalse\nfalse\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNullEqualsUndefined() {
        String code = "var u; var o = {};\nprint(u == null); print(null == u); print(o.x == null); print(o.x != null);";
        assertEquals("true\ntrue\ntrue\nfalse\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testEqualityWithoutTypeCoercion() {
        // Deliberately different from Javascript's ==, so no differential test
        assertEquals("false\nfalse\nfalse\nfalse\n", interpreterOutput(
                "print(1 == \"1\"); print(true == 1); print(0 == false); print(\"\" == false);"));
    }

    @Test
    public void testNumberFormatting() {
        String code = "print(3); print(2.5); print(-4); print(1 / 0); print(0 / 0); print(0.1 + 0.2);";
        assertEquals("3\n2.5\n-4\nInfinity\nNaN\n0.30000000000000004\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    // =========================================================================
    // Java contract of TSValue: equals and hashCode must be consistent
    // =========================================================================

    @Test
    public void testEqualsAndHashCode() {
        Object one = new TSValue(1);
        Object oneAsDouble = new TSValue(1.0);
        assertEquals(one, oneAsDouble);
        assertEquals(one.hashCode(), oneAsDouble.hashCode());
        assertEquals(new TSValue("a"), new TSValue("a"));
        assertNotEquals(new TSValue(1), new TSValue("1"));
    }

    @Test
    public void testHashCodeOfUndefinedAndNull() {
        assertEquals(TSValue.UNDEFINED.hashCode(), TSValue.UNDEFINED.hashCode());
        assertEquals(TSValue.NULL.hashCode(), TSValue.NULL.hashCode());
    }
}
