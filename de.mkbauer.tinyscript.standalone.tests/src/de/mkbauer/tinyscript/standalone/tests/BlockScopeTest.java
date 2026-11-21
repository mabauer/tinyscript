package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * tinyscript's variables are block-scoped; the generated Javascript must behave the same
 * (it used to emit function-scoped var).
 */
public class BlockScopeTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testShadowingInANestedBlock() {
        String code = "var x = \"outer\";\n"
                + "{ var x = \"block\"; print(x); }\n"
                + "if (true) { var x = \"if\"; print(x); }\n"
                + "while (true) { var x = \"while\"; print(x); break; }\n"
                + "print(x);";
        assertEquals("block\nif\nwhile\nouter\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testClosuresOverLoopVariables() {
        String code = "var fs = [];\n"
                + "for (var i = 1, 3) { fs.push(() => i); }\n"
                + "for (var s : [\"a\", \"b\"]) { fs.push(() => s); }\n"
                + "for (var [k, v] : [[1, 2]]) { fs.push(() => k + v); }\n"
                + "print(fs.map(f => f()).join());";
        assertEquals("1,2,3,a,b,3\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testClosuresOverVariablesInLoopBodies() {
        String code = "var fs = [];\n"
                + "for (var i = 1, 3) { var square = i * i; fs.push(() => square); }\n"
                + "var n = 0; while (n < 2) { var copy = n; fs.push(() => copy); n++; }\n"
                + "print(fs.map(f => f()).join());";
        assertEquals("1,4,9,0,1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testChangingTheLoopVariableDoesNotChangeTheIterations() {
        // Like Lua: the numeric for loop counts on its own, the body gets a copy
        String code = "for (var i = 1, 3) { i = i * 10; print(i); }";
        assertEquals("10\n20\n30\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNestedLoopsWithPatternsAndExistingVariables() {
        // The generator checks the loop variables of the enclosing loops for shadowing
        String code = "let sum = 0; let j = 0;\n"
                + "for (const [a, b] : [[1, 2], [3, 4]]) { for (let i = a, b) { for (j = 1, 2) { const x = i * j; sum += x; } } }\n"
                + "print(sum, j);";
        assertEquals("30 2\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testAnExistingVariableAsLoopVariable() {
        // for (i = ...) without var uses the variable i of the surrounding block
        String code = "var i = 0; for (i = 1, 3) { } print(i);\n"
                + "var x = \"\"; for (x : [\"a\", \"b\"]) { } print(x);";
        assertEquals("3\nb\n", interpreterOutput(code));
        assertSameOutput(code);
    }
}
