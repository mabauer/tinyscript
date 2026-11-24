package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Characterization tests for the semantics of values: {@code typeof}, truthiness,
 * equality and how numbers are printed.
 *
 * <p>They pin down the behavior before {@link TSValue} is restructured. Where
 * Javascript behaves the same, the snippets also run as differential tests.
 */
public class ValuesTest extends TinyscriptTranspilerTestBase {

    private static final String VALUES =
            "let u; let n = null; let b = true; let i = 1; let nan = 0 / 0; let s = \"s\";\n"
            + "let f = function() { return 1; }; let o = {}; let a = [];\n";

    @Test
    public void testTypeOf() {
        String code = VALUES
                + "print(typeof u); print(typeof n); print(typeof b); print(typeof i); print(typeof nan);\n"
                + "print(typeof s); print(typeof f); print(typeof o); print(typeof a);";
        assertEquals("undefined\nobject\nboolean\nnumber\nnumber\nstring\nfunction\nobject\nobject\n",
                interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testTypeOfArbitraryExpressions() {
        String code = "function f() { return 1; } let a = [\"s\"]; let o = { x: true, g: f };\n"
                + "print(typeof f()); print(typeof a[0]); print(typeof (1 + 2)); print(typeof o.x);\n"
                + "print(typeof o.g); print(typeof o.missing); print(typeof [1]); print(typeof {});";
        assertEquals("number\nstring\nnumber\nboolean\nfunction\nundefined\nobject\nobject\n",
                interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testTypeOfBindsTighterThanBinaryOperators() {
        String code = "print(typeof 1 + \"x\"); print(typeof 1 == \"number\");";
        assertEquals("numberx\ntrue\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testTypeOfUndeclaredNameIsRejected() {
        assertRejected("print(typeof nothing);");
    }

    @Test
    public void testTypeOfPropertyOfUndefinedIsATypeError() {
        assertSameFailure(TinyscriptTypeError.class, "let o = {}; print(typeof o.x.y);");
    }

    @Test
    public void testConditions() {
        // Booleans, null / undefined (false) and objects (true); numbers and strings are a
        // TypeError (see StrictnessTest)
        String code = "function t(x) { if (x) { return \"t\"; } else { return \"f\"; } }\n"
                + VALUES
                + "print(t(u) + t(n) + t(b) + t(false) + t(f) + t(o) + t(a));";
        assertEquals("fftfttt\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNaNIsNoCondition() {
        assertSameFailure(TinyscriptTypeError.class, "let nan = 0 / 0; print(!nan);");
        assertSameFailure(TinyscriptTypeError.class, "let nan = 0 / 0; print(nan || false);");
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
        String code = "let u; let o = {};\nprint(u == null); print(null == u); print(o.x == null); print(o.x != null);";
        assertEquals("true\ntrue\ntrue\nfalse\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testEqualityWithoutTypeCoercion() {
        // Unlike Javascript's ==; the generator emits __ts_eq to match
        String code = "print(1 == \"1\"); print(true == 1); print(0 == false); print(\"\" == false);\n"
                + "print(1 != \"1\"); print(null == 0); print(null != false);";
        assertEquals("false\nfalse\nfalse\nfalse\ntrue\nfalse\ntrue\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testStringObjectsEqualTheirStrings() {
        String code = "let s = new String(\"a\");\n"
                + "print(s == \"a\"); print(\"a\" == s); print(s == new String(\"a\")); print(s != \"b\");";
        assertEquals("true\ntrue\ntrue\ntrue\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNumberFormatting() {
        String code = "print(3); print(2.5); print(-4); print(1 / 0); print(0 / 0); print(0.1 + 0.2);";
        assertEquals("3\n2.5\n-4\nInfinity\nNaN\n0.30000000000000004\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNumbersOnlyHaveMethodsAndBooleansNoProperties() {
        // Numbers have toFixed and toString (see NumberMethodsTest), nothing else
        assertEquals(java.util.List.of("1:7: a number has no method 'hasOwnProperty'"), reportedErrors("print((5).hasOwnProperty(\"x\"));"));
        assertKnownGap(TinyscriptTypeError.class, "print(true.toString());");
        assertKnownGap(TinyscriptTypeError.class, "let n = 5; print(n.x);");
        assertSameFailure(TinyscriptTypeError.class, "let n = 5; n.x = 1;");
        assertKnownGap(TinyscriptTypeError.class, "let nan = 0 / 0; print(nan.x);");
        assertKnownGap(TinyscriptTypeError.class, "let b = false; print(b.hasOwnProperty(\"value\"));");
    }

    @Test
    public void testStringsKeepTheirMethods() {
        String code = "let n = 5; print(String(n) + String(true)); print(\"abc\".length); print(\"abc\".charAt(1));";
        assertEquals("5true\n3\nb\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    // =========================================================================
    // Java contract of TSValue: equals and hashCode must be consistent
    // =========================================================================

    @Test
    public void testEqualsAndHashCode() {
        Object one = TSValue.of(1);
        Object oneAsDouble = TSValue.of(1.0);
        assertEquals(one, oneAsDouble);
        assertEquals(one.hashCode(), oneAsDouble.hashCode());
        assertEquals(TSValue.of("a"), TSValue.of("a"));
        assertNotEquals(TSValue.of(1), TSValue.of("1"));
    }

    @Test
    public void testHashCodeOfUndefinedAndNull() {
        assertEquals(TSValue.UNDEFINED.hashCode(), TSValue.UNDEFINED.hashCode());
        assertEquals(TSValue.NULL.hashCode(), TSValue.NULL.hashCode());
    }

    @Test
    public void testUndefinedLiteral() {
        String code = "let x; const o = { undefined: 1 };\n"
                + "print(x == undefined, undefined == null, undefined != 0, typeof undefined, o.missing == undefined);\n"
                + "function f(a) { if (a == undefined) { return undefined; } return a; }\n"
                + "print(f(), f(undefined), f(2), [undefined, null], undefined ?? \"default\", (2.5).toFixed(undefined), o.undefined);\n"
                + "print(\"x\" + undefined, String(undefined));";
        assertEquals("true true true undefined true\n"
                + "undefined undefined 2 [undefined, null] default 3 1\n"
                + "xundefined undefined\n", interpreterOutput(code));
        assertSameOutput(code);
    }
}
