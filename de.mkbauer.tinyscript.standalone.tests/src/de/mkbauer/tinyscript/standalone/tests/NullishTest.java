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
        String code = "let u; let m = new Map(); m.set(\"a\", 0);\n"
                + "print(u ?? 1, null ?? 2, 0 ?? 3, \"\" ?? 4, false ?? 5);\n"
                + "print(m.get(\"a\") ?? 9, m.get(\"b\") ?? 9, u ?? null ?? \"last\");";
        assertEquals("1 2 0  false\n0 9 last\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNullishCoalescingEvaluatesTheRightSideOnlyIfNeeded() {
        String code = "let calls = 0; function f() { calls++; return 1; }\n"
                + "let x = 5 ?? f(); let u; let y = u ?? f(); print(x + y + calls);";
        assertEquals("7\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNullishCoalescingPrecedence() {
        String code = "let u; print(u ?? 1 + 2); print((u ?? 1) + 2); print(u ?? true ? \"y\" : \"n\"); let v = u ?? 4; print(v);\n"
                + "print((u ?? false) || true); print(u ?? (false || true));";
        assertEquals("3\n3\ny\n4\ntrue\ntrue\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNullishCoalescingCannotBeMixedWithAndOrWithoutParentheses() {
        // A syntax error in Javascript as well
        assertRejected("let a; let b = a ?? true || false;");
        assertRejected("let a; let b = true && a ?? false;");
    }

    // =========================================================================
    // ?.
    // =========================================================================

    @Test
    public void testOptionalChaining() {
        String code = "let u; let o = { a: { b: 1 }, f: function() { return 2; }, arr: [5] };\n"
                + "print(o?.a?.b, o.x?.b, u?.b, o?.[\"a\"].b, u?.[0], o.arr?.[0]);\n"
                + "print(o.f?.call(o), o?.f(), u?.f(), \"abc\"?.length);";
        assertEquals("1 undefined undefined 1 undefined 5\n2 2 undefined 3\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testOptionalChainingShortCircuitsTheWholeChain() {
        // Like Javascript: u?.a.b.c is undefined, not a TypeError for .b; f() is not called
        String code = "let u; let calls = 0; function f() { calls++; return 1; }\n"
                + "print(u?.a.b.c, u?.a[f()], u?.a.m(f()), calls);\n"
                + "let list = { value: 1, next: { value: 2, next: null } }; print(list.next?.next?.value ?? \"end\");";
        assertEquals("undefined undefined undefined 0\nend\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testOnlyTheOptionalPartIsOptional() {
        assertSameFailure(TinyscriptTypeError.class, "let o = {}; print(o?.a.b);");
        assertSameFailure(TinyscriptTypeError.class, "let u; print((u?.a).b);");
    }

    @Test
    public void testOptionalChainIsNoAssignmentTarget() {
        // A syntax error in Javascript as well
        assertRejected("let o = {}; o?.a = 1;");
        assertRejected("let o = { a: {} }; o?.a.b = 1;");
        assertRejected("let o = { n: 1 }; o?.n++;");
    }
}
