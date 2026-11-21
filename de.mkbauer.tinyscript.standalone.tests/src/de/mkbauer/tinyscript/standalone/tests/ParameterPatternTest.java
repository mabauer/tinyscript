package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Flat destructuring patterns as parameters of functions and arrow functions,
 * e.g. robots.map(({ x, y }) => x + y).
 */
public class ParameterPatternTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testPatternsAsParameters() {
        String code = "function distance([x1, y1], [x2, y2]) { return Math.abs(x1 - x2) + Math.abs(y1 - y2); }\n"
                + "print(distance([1, 2], [4, 6]));\n"
                + "const robots = [{ x: 1, y: 2, name: \"a\" }, { x: 3, y: 4, name: \"b\" }];\n"
                + "print(robots.map(({ x, y }) => x * y).join());\n"
                + "print(robots.map(({ name }, i) => name + i).join());\n"
                + "const m = new Map([[\"k\", 1], [\"l\", 2]]); print(m.entries().map(([key, value]) => key + value).join());\n"
                + "const f = function({ a }, b, ...rest) { return a + b + rest.length; }; print(f({ a: 1 }, 2, 3, 4));";
        assertEquals("7\n2,12\na0,b1\nk1,l2\n5\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testPatternParametersWithABlockBody() {
        String code = "const sum = ([a, b]) => { const s = a + b; return s; }; print(sum([3, 4]));\n"
                + "print([[1, 2], [3, 4]].map(([a, b]) => { return a * b; }).join());";
        assertEquals("7\n2,12\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testPatternParametersAreStrictLikeOtherPatterns() {
        assertInterpreterThrows(TinyscriptTypeError.class, "function f([a, b]) { return a; } f([1]);");
        assertInterpreterThrows(TinyscriptTypeError.class, "function f({ a }) { return a; } f(null);");
        assertInterpreterThrows(TinyscriptTypeError.class, "function f([a]) { return a; } f();");
    }

    @Test
    public void testTheGeneratedCodeChecksArrayParameters() {
        assumeNodeAvailable();
        String jsCode = transpile(parse("const f = ([a, b, c]) => a; f([1, 2]);"));
        AssertionError e = assertThrows(AssertionError.class, () -> NodeJsRunner.run(jsCode));
        assertTrue(e.getMessage().contains("at least 3 elements"), e.getMessage());
    }

    @Test
    public void testFunctionLengthCountsPatterns() {
        String code = "function f([a, b], { c }, d) { } print(f.length);";
        assertEquals("3\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testInvalidParameterPatterns() {
        assertRejected("function f([a, b], a) { }");
        assertRejected("function f({ a, a }) { }");
        assertRejected("const g = ([x, y]) => z;");
    }

    // =========================================================================
    // Infinity and std:string integers
    // =========================================================================

    @Test
    public void testInfinity() {
        String code = "print(Infinity, -Infinity, Infinity > 1e300, 1 / 0 == Infinity, Math.min(Infinity, 3));";
        assertEquals("Infinity -Infinity true true 3\n", interpreterOutput(code));
        assertSameOutput(code);
    }
}
