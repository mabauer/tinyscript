package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * a ?? b and optional chaining a?.b, as in Javascript.
 */
public class NullishTest extends TinyscriptTranspilerTestBase {

    // =========================================================================
    // ??
    // =========================================================================

    @Test
    public void testNullishCoalescing() {
        String code = "var u; var m = new Map(); m.set(\"a\", 0);\n"
                + "print(u ?? 1, \" \", null ?? 2, \" \", 0 ?? 3, \" \", \"\" ?? 4, \" \", false ?? 5);\n"
                + "print(m.get(\"a\") ?? 9, \" \", m.get(\"b\") ?? 9, \" \", u ?? null ?? \"last\");";
        assertEquals("1 2 0  false\n0 9 last\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNullishCoalescingEvaluatesTheRightSideOnlyIfNeeded() {
        String code = "var calls = 0; function f() { calls++; return 1; }\n"
                + "var x = 5 ?? f(); var u; var y = u ?? f(); print(x + y + calls);";
        assertEquals("7\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNullishCoalescingPrecedence() {
        String code = "var u; print(u ?? 1 + 2); print((u ?? 1) + 2); print(u ?? true ? \"y\" : \"n\"); var v = u ?? 4; print(v);\n"
                + "print((u ?? false) || true); print(u ?? (false || true));";
        assertEquals("3\n3\ny\n4\ntrue\ntrue\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNullishCoalescingCannotBeMixedWithAndOrWithoutParentheses() {
        // A syntax error in Javascript as well
        assertRejected("var a; var b = a ?? true || false;");
        assertRejected("var a; var b = true && a ?? false;");
    }

    // =========================================================================
    // ?.
    // =========================================================================

    @Test
    public void testOptionalChaining() {
        String code = "var u; var o = { a: { b: 1 }, f: function() { return 2; }, arr: [5] };\n"
                + "print(o?.a?.b, \" \", o.x?.b, \" \", u?.b, \" \", o?.[\"a\"].b, \" \", u?.[0], \" \", o.arr?.[0]);\n"
                + "print(o.f?.call(o), \" \", o?.f(), \" \", u?.f(), \" \", \"abc\"?.length);";
        assertEquals("1 undefined undefined 1 undefined 5\n2 2 undefined 3\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testOptionalChainingShortCircuitsTheWholeChain() {
        // Like Javascript: u?.a.b.c is undefined, not a TypeError for .b; f() is not called
        String code = "var u; var calls = 0; function f() { calls++; return 1; }\n"
                + "print(u?.a.b.c, \" \", u?.a[f()], \" \", u?.a.m(f()), \" \", calls);\n"
                + "var list = { value: 1, next: { value: 2, next: null } }; print(list.next?.next?.value ?? \"end\");";
        assertEquals("undefined undefined undefined 0\nend\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testOnlyTheOptionalPartIsOptional() {
        assertInterpreterThrows(TinyscriptTypeError.class, "var o = {}; print(o?.a.b);");
        assertInterpreterThrows(TinyscriptTypeError.class, "var u; print((u?.a).b);");
    }

    @Test
    public void testOptionalChainIsNoAssignmentTarget() {
        // A syntax error in Javascript as well
        assertRejected("var o = {}; o?.a = 1;");
        assertRejected("var o = { a: {} }; o?.a.b = 1;");
        assertRejected("var o = { n: 1 }; o?.n++;");
    }
}
