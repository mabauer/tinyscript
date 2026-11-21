package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * tinyscript's variables are block-scoped; the generated Javascript must behave the same
 * (it used to emit function-scoped let).
 */
public class BlockScopeTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testShadowingInANestedBlock() {
        String code = "let x = \"outer\";\n"
                + "{ let x = \"block\"; print(x); }\n"
                + "if (true) { let x = \"if\"; print(x); }\n"
                + "while (true) { let x = \"while\"; print(x); break; }\n"
                + "print(x);";
        assertEquals("block\nif\nwhile\nouter\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testClosuresOverLoopVariables() {
        String code = "let fs = [];\n"
                + "for (const i : range(1, 4)) { fs.push(() => i); }\n"
                + "for (let s : [\"a\", \"b\"]) { fs.push(() => s); }\n"
                + "for (let [k, v] : [[1, 2]]) { fs.push(() => k + v); }\n"
                + "print(fs.map(f => f()).join());";
        assertEquals("1,2,3,a,b,3\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testClosuresOverVariablesInLoopBodies() {
        String code = "let fs = [];\n"
                + "for (const i : range(1, 4)) { let square = i * i; fs.push(() => square); }\n"
                + "let n = 0; while (n < 2) { let copy = n; fs.push(() => copy); n++; }\n"
                + "print(fs.map(f => f()).join());";
        assertEquals("1,4,9,0,1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testChangingTheLoopVariableDoesNotChangeTheIterations() {
        // Each iteration gets its own variable: changing it does not change the next one
        String code = "for (let i : range(1, 4)) { i = i * 10; print(i); }";
        assertEquals("10\n20\n30\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNestedLoopsWithPatternsAndExistingVariables() {
        // The generator checks the loop variables of the enclosing loops for shadowing
        String code = "let sum = 0; let j = 0;\n"
                + "for (const [a, b] : [[1, 2], [3, 4]]) { for (const i : range(a, b + 1)) { for (j : range(1, 3)) { const x = i * j; sum += x; } } }\n"
                + "print(sum, j);";
        assertEquals("30 2\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testAnExistingVariableAsLoopVariable() {
        // for (i = ...) without let uses the variable i of the surrounding block
        String code = "let i = 0; for (i : range(1, 4)) { } print(i);\n"
                + "let x = \"\"; for (x : [\"a\", \"b\"]) { } print(x);";
        assertEquals("3\nb\n", interpreterOutput(code));
        assertSameOutput(code);
    }
}
