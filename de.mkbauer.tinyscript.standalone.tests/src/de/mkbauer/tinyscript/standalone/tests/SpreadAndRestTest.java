package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Spread in calls and array literals (f(...items), [...a, x]) takes what the for loop goes
 * through; a rest element at the end of an array pattern ([first, ...rest]) gets the other
 * elements.
 */
public class SpreadAndRestTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testSpreadInCalls() {
        String code = "const values = [3, 1, 4];\n"
                + "print(Math.max(...values), Math.min(0, ...values, 9));\n"
                + "const list = [1]; list.push(...values, 5); print(list);\n"
                + "function f(a, b, ...rest) { return a + b + rest.length; } print(f(...[1, 2, 3, 4]));\n"
                + "class P { constructor(x, y) { this.x = x; this.y = y; } } print(new P(...[1, 2]));\n"
                + "print(...[]);";
        assertEquals("4 0\n[1, 3, 1, 4, 5]\n5\nP {x: 1, y: 2}\n\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testSpreadInArrayLiterals() {
        String code = "const a = [1, 2]; const b = [3];\n"
                + "print([...a, ...b], [0, ...a, 9], [...a,], [...[]]);\n"
                + "const copy = [...a]; copy.push(3); print(a, copy);";
        assertEquals("[1, 2, 3] [0, 1, 2, 9] [1, 2] []\n[1, 2] [1, 2, 3]\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testSpreadTakesWhatTheForLoopGoesThrough() {
        String code = "print([...\"abc\"], [...range(3)], [...range(3).reverse()]);\n"
                + "print([...new Set([1, 1, 2])], [...new Map([[\"a\", 1]])]);\n"
                + "print(String.fromCharCode(...[104, 105]));";
        assertEquals("[\"a\", \"b\", \"c\"] [0, 1, 2] [2, 1, 0]\n[1, 2] [[\"a\", 1]]\nhi\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testSpreadOfAnythingElseIsATypeError() {
        assertSameFailure(TinyscriptTypeError.class, "const o = { a: 1 }; print(...o);");
        assertSameFailure(TinyscriptTypeError.class, "print([...5]);");
        assertSameFailure(TinyscriptTypeError.class, "let u; print(...u);");
    }

    @Test
    public void testRestElements() {
        String code = "const [first, ...rest] = [1, 2, 3]; print(first, rest);\n"
                + "const [a, b, ...none] = [1, 2]; print(a, b, none);\n"
                + "const [...all] = \"x\".split(\"\"); print(all);\n"
                + "for (const [key, ...values] : [[\"k\", 1, 2], [\"l\"]]) { print(key, values); }\n"
                + "function head([h, ...t]) { return h + \"/\" + t.length; } print(head([7, 8, 9]));\n"
                + "print([[1, 2, 3]].map(([x, ...more]) => more));";
        assertEquals("1 [2, 3]\n1 2 []\n[\"x\"]\nk [1, 2]\nl []\n7/2\n[[2, 3]]\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testRestElementsAreStrictLikeOtherPatterns() {
        // At least as many elements as there are names before the rest
        assertSameFailure(TinyscriptTypeError.class, "const one = [1];\nconst [a, b, ...rest] = one;");
        assertSameFailure(TinyscriptTypeError.class, "const [...rest] = \"abc\";");
    }

    @Test
    public void testRestNamesAreDeclarations() {
        String code = "const [x, ...xs] = [1, 2]; let [y, ...ys] = [3]; ys = ys + [4]; print(xs, ys);";
        assertEquals("[2] [4]\n", interpreterOutput(code));
        assertSameOutput(code);
    }
}
