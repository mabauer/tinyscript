package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Flat destructuring patterns in let and for: let [a, b] = pair; let { row, col } = p;
 * for (let [key, value] : map).
 */
public class DestructuringTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testArrayPattern() {
        String code = "let [a, b] = [1, 2]; print(a + b);\n"
                + "let [first, second] = \"x,y,z\".split(\",\"); print(first + second);\n"
                + "let [x, y] = [3, 4], z = 5; print(x * y * z);\n"
                + "function divide(n, d) { return [Math.floor(n / d), n % d]; } let [q, r] = divide(17, 5); print(q, r);";
        assertSameOutput("3\nxy\n60\n3 2\n", code);
    }

    @Test
    public void testObjectPattern() {
        String code = "let guard = { row: 6, col: 4, dir: \"up\" }; let { row, col } = guard; print(row * 10 + col);\n"
                + "let { missing } = guard; print(missing);\n"
                + "let { length } = \"abc\"; print(length);";
        assertSameOutput("64\nundefined\n3\n", code);
    }

    @Test
    public void testPatternsInForLoops() {
        String code = "let m = new Map([[\"a\", 1], [\"b\", 2]]);\n"
                + "for (let [key, value] : m) { print(key + value); }\n"
                + "for (let [n, square] : [[2, 4], [3, 9]]) { print(n + \"^2=\" + square); }\n"
                + "for (let { name, age } : [{ name: \"Ann\", age: 30 }]) { print(name + \" \" + age); }";
        assertSameOutput("a1\nb2\n2^2=4\n3^2=9\nAnn 30\n", code);
    }

    @Test
    public void testPatternVariablesAreBlockScoped() {
        String code = "let a = \"outer\"; { let [a, b] = [1, 2]; print(a); } print(a);\n"
                + "let fs = []; for (let [k, v] : [[1, 2], [3, 4]]) { fs.push(() => k + v); } print(fs[0]() + fs[1]());";
        assertSameOutput("1\nouter\n10\n", code);
    }

    @Test
    public void testAnArrayPatternNeedsAnArrayThatIsLongEnough() {
        TinyscriptRuntimeException e = assertSameFailure(TinyscriptTypeError.class, "let xs = [1, 2];\nlet [a, b, c] = xs;");
        assertEquals("[a, b, c] needs an array with at least 3 elements, not 2", e.getMessage());
        assertSameFailure(TinyscriptTypeError.class, "let [a, b] = \"ab\";");
        assertSameFailure(TinyscriptTypeError.class, "let [a] = null;");
        assertSameFailure(TinyscriptTypeError.class, "for (let [a, b] : [[1, 2], [3]]) { }");
        // More elements are fine
        assertSameOutput("1\n", "let [a] = [1, 2, 3]; print(a);");
    }

    @Test
    public void testTheGeneratedCodeChecksTheArrayLengthAsWell() {
        assumeNodeAvailable();
        String jsCode = transpile(parse("let xs = [1, 2];\nlet [a, b, c] = xs;"));
        AssertionError e = assertThrows(AssertionError.class, () -> NodeJsRunner.run(jsCode));
        assertTrue(e.getMessage().contains("at least 3 elements"), e.getMessage());
    }

    @Test
    public void testAnObjectPatternNeedsAnObject() {
        assertSameFailure(TinyscriptTypeError.class, "let { a } = null;");
        assertSameFailure(TinyscriptTypeError.class, "let u; let { a } = u;");
        assertKnownGap(TinyscriptTypeError.class, "let { a } = 42;");
    }

    @Test
    public void testInvalidPatterns() {
        assertRejected("let [a, a] = [1, 2];");
        assertRejected("let [a, b];");
        assertRejected("for (let [a, b] = 1, 3) { }");
        assertRejected("let [a, [b, c]] = [1, [2, 3]];");
        assertRejected("let { a: x } = { a: 1 };");
        assertRejected("let [a, b] = [1, 2]; print(c);");
    }

    @Test
    public void testPatternVariablesCanBeExported() {
        String code = "export let [a, b] = [1, 2]; print(a + b);";
        assertSameOutput("3\n", code);
    }
}
