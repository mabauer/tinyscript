package de.mkbauer.tinyscript.standalone.tests;


import org.junit.jupiter.api.Test;

/**
 * Arrow functions with an expression body (x => x * 2) return the expression; with a block
 * body (x => { ... }) they behave like a function: without return, the result is undefined.
 */
public class ArrowFunctionsTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testBlockBodyRunsAllStatements() {
        String code = "let log = \"\"; let f = x => { log = log + \"a\"; log = log + x; }; f(\"b\"); print(log);\n"
                + "let seen = []; [3, 1].forEach(x => { let y = x * 2; seen.push(y); }); print(seen.join(\",\"));";
        assertSameOutput("ab\n6,2\n", code);
    }

    @Test
    public void testBlockBodyWithoutReturnGivesUndefined() {
        String code = "let f = x => { x * 2; }; print(f(1)); let g = () => {}; print(g());\n"
                + "let h = (a, b) => { let s = a + b; return s * 2; }; print(h(1, 2));";
        assertSameOutput("undefined\nundefined\n6\n", code);
    }

    @Test
    public void testExpressionBody() {
        String code = "let f = x => x * 2; print(f(4)); let add = x => y => x + y; print(add(1)(2));\n"
                + "let o = x => ({ v: x }); print(o(5).v); let sign = x => x < 0 ? \"-\" : \"+\"; print(sign(-1) + sign(1));";
        assertSameOutput("8\n3\n5\n-+\n", code);
    }

    @Test
    public void testArrowFunctionsSeeThisOfTheirSurroundingFunction() {
        String code = "let o = { n: 1, f: function() { let g = () => this.n; return g(); },\n"
                + "  h: function() { let r = []; [1, 2].forEach(x => { r.push(x + this.n); }); return r; } };\n"
                + "print(o.f()); print(o.h().join(\",\"));";
        assertSameOutput("1\n2,3\n", code);
    }

    @Test
    public void testRestParameters() {
        String code = "function f(a, ...rest) { return a + rest.length; } print(f(1, 2, 3)); print(f(1));\n"
                + "let g = (...xs) => xs.join(\"-\"); print(g(1, 2, 3)); let h = (x, ...xs) => { return x + xs.length; }; print(h(10, 1, 1));\n"
                + "let k = function(...all) { return all.length; }; print(k());";
        assertSameOutput("3\n1\n1-2-3\n12\n0\n", code);
    }
}
