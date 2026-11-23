package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Default parameters, function f(a, step = 1): the default replaces a missing argument or
 * undefined (not null), is evaluated at each call and sees the parameters before it.
 */
public class DefaultParameterTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testDefaultsInAllKindsOfFunctions() {
        String code = "function f(a, b = 2) { return a + b; }\n"
                + "print(f(1), f(1, 5), f(1, undefined));\n"
                + "const g = function(s, sep = \", \") { return s.join(sep); }; print(g([1, 2]), g([1, 2], \"-\"));\n"
                + "const h = (x, y = 10) => x * y; print(h(2), h(2, 3));\n"
                + "const k = (x = 7) => { return x; }; print(k());\n"
                + "const o = { scale(v, factor = 2) { return v * factor; } }; print(o.scale(3), o.scale(3, 3));\n"
                + "class Point { constructor(x = 0, y = 0) { this.x = x; this.y = y; } } print(new Point(), new Point(1));";
        assertEquals("3 6 3\n1, 2 1-2\n20 6\n7\n6 9\nPoint {x: 0, y: 0} Point {x: 1, y: 0}\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNullDoesNotTakeTheDefault() {
        String code = "function f(a = 1) { return a; } print(f(null), f(undefined), f());";
        assertEquals("null 1 1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testADefaultIsEvaluatedAtEachCall() {
        String code = "function add(x, list = []) { list.push(x); return list; }\n"
                + "print(add(1), add(2));\n"
                + "let calls = 0; function next() { calls += 1; return calls; }\n"
                + "function f(a = next()) { return a; } print(f(), f(10), f(), calls);";
        assertEquals("[1] [2]\n1 10 2 2\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testADefaultSeesTheParametersBeforeIt() {
        String code = "const size = 100;\n"
                + "function slice(items, start = 0, end = items.length) { return items.slice(start, end); }\n"
                + "print(slice([1, 2, 3]), slice([1, 2, 3], 1), slice([1, 2, 3], 0, 1));\n"
                + "function area(w, h = w, unit = size) { return w * h + unit; } print(area(3), area(3, 2));\n"
                + "function later(a, get = () => b, b = 5) { return get(); } print(later(1));";
        assertEquals("[1, 2, 3] [2, 3] [1]\n109 106\n5\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testDefaultsWithPatternsAndRest() {
        String code = "function f([a, b], c = a + b, ...rest) { return c + rest.length; }\n"
                + "print(f([1, 2]), f([1, 2], 10), f([1, 2], 10, 0, 0));\n"
                + "const g = ({ x }, scale = x) => x * scale; print(g({ x: 3 }), g({ x: 3 }, 2));";
        assertEquals("3 10 12\n9 6\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testLengthCountsTheParametersBeforeTheFirstDefault() {
        String code = "function f(a, b, c = 1) { } function g(a = 1, b = 2) { }\n"
                + "print(f.length, g.length, ((x, y = 1) => x).length);";
        assertEquals("2 0 1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testParenthesizedAssignmentIsNoArrowFunction() {
        String code = "let a = 0; const b = (a = 3); print(a, b); const c = (a = 4) => a; print(c(), c(5), a);";
        assertEquals("3 3\n4 5 3\n", interpreterOutput(code));
        assertSameOutput(code);
    }
}
