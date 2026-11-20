package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Flat destructuring patterns in var and for: var [a, b] = pair; var { row, col } = p;
 * for (var [key, value] : map).
 */
public class DestructuringTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testArrayPattern() {
        String code = "var [a, b] = [1, 2]; print(a + b);\n"
                + "var [first, second] = \"x,y,z\".split(\",\"); print(first + second);\n"
                + "var [x, y] = [3, 4], z = 5; print(x * y * z);\n"
                + "function divide(n, d) { return [Math.floor(n / d), n % d]; } var [q, r] = divide(17, 5); print(q, \" \", r);";
        assertEquals("3\nxy\n60\n3 2\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testObjectPattern() {
        String code = "var guard = { row: 6, col: 4, dir: \"up\" }; var { row, col } = guard; print(row * 10 + col);\n"
                + "var { missing } = guard; print(missing);\n"
                + "var { length } = \"abc\"; print(length);";
        assertEquals("64\nundefined\n3\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testPatternsInForLoops() {
        String code = "var m = new Map([[\"a\", 1], [\"b\", 2]]);\n"
                + "for (var [key, value] : m) { print(key + value); }\n"
                + "for (var [n, square] : [[2, 4], [3, 9]]) { print(n + \"^2=\" + square); }\n"
                + "for (var { name, age } : [{ name: \"Ann\", age: 30 }]) { print(name + \" \" + age); }";
        assertEquals("a1\nb2\n2^2=4\n3^2=9\nAnn 30\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testPatternVariablesAreBlockScoped() {
        String code = "var a = \"outer\"; { var [a, b] = [1, 2]; print(a); } print(a);\n"
                + "var fs = []; for (var [k, v] : [[1, 2], [3, 4]]) { fs.push(() => k + v); } print(fs[0]() + fs[1]());";
        assertEquals("1\nouter\n10\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testAnArrayPatternNeedsAnArrayThatIsLongEnough() {
        TinyscriptRuntimeException e = assertThrows(TinyscriptTypeError.class, () -> interpreterOutput("var [a, b, c] = [1, 2];"));
        assertEquals("[a, b, c] needs an array with at least 3 elements, not 2", e.getMessage());
        assertInterpreterThrows(TinyscriptTypeError.class, "var [a, b] = \"ab\";");
        assertInterpreterThrows(TinyscriptTypeError.class, "var [a] = null;");
        assertInterpreterThrows(TinyscriptTypeError.class, "for (var [a, b] : [[1, 2], [3]]) { }");
        // More elements are fine
        assertEquals("1\n", interpreterOutput("var [a] = [1, 2, 3]; print(a);"));
    }

    @Test
    public void testTheGeneratedCodeChecksTheArrayLengthAsWell() {
        assumeNodeAvailable();
        String jsCode = transpile(parse("var [a, b, c] = [1, 2];"));
        AssertionError e = assertThrows(AssertionError.class, () -> NodeJsRunner.run(jsCode));
        assertTrue(e.getMessage().contains("at least 3 elements"), e.getMessage());
    }

    @Test
    public void testAnObjectPatternNeedsAnObject() {
        assertInterpreterThrows(TinyscriptTypeError.class, "var { a } = null;");
        assertInterpreterThrows(TinyscriptTypeError.class, "var u; var { a } = u;");
        assertInterpreterThrows(TinyscriptTypeError.class, "var { a } = 42;");
    }

    @Test
    public void testInvalidPatterns() {
        assertRejected("var [a, a] = [1, 2];");
        assertRejected("var [a, b];");
        assertRejected("for (var [a, b] = 1, 3) { }");
        assertRejected("var [a, [b, c]] = [1, [2, 3]];");
        assertRejected("var { a: x } = { a: 1 };");
        assertRejected("var [a, b] = [1, 2]; print(c);");
    }

    @Test
    public void testPatternVariablesCanBeExported() {
        String code = "export var [a, b] = [1, 2]; print(a + b);";
        assertEquals("3\n", interpreterOutput(code));
    }
}
