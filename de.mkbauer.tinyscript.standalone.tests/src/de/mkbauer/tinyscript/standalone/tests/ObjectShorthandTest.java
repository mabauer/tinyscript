package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The object literal shorthand { row, col } for { row: row, col: col }, as in Javascript.
 */
public class ObjectShorthandTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testShorthandProperties() {
        String code = "const row = 3; const col = 5; const p = { row, col };\n"
                + "print(p.row * 10 + p.col, Object.keys(p).join());\n"
                + "const q = { row, label: \"start\", col }; print(Object.keys(q).join(), q.label);";
        assertEquals("35 row,col\nrow,label,col start\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testShorthandAndPatternsFitTogether() {
        String code = "function neighbor(position) { const { row, col } = position; return { row: row + 1, col }; }\n"
                + "const n = neighbor({ row: 1, col: 2 }); print(n.row, n.col);";
        assertEquals("2 2\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testShorthandUsesTheVariableInScope() {
        // The generator renames shadowing variables; the key must still be the name in the source
        String code = "const x = 1; { const x = 2; const o = { x }; print(o.x); } print(({ x }).x);\n"
                + "function f(value) { return { value }; } print(f(7).value);";
        assertEquals("2\n1\n7\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testPropertiesKeepTheirOrder() {
        // Like Javascript: Object.keys gives the properties in the order they were added
        String code = "const o = { zebra: 1, apple: 2, mango: 3 }; o.banana = 4; print(Object.keys(o).join());";
        assertEquals("zebra,apple,mango,banana\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testIntegerKeysComeFirstAsInJavascript() {
        // Javascript lists integer keys first, in ascending order, then the others in insertion order
        String code = "const pipes = { \"|\": 1, \"7\": 2, \"10\": 3, a: 4, \"2\": 5 };\n"
                + "print(Object.keys(pipes), Object.values(pipes), pipes);\n"
                + "const o = { b: 1 }; o[30] = \"x\"; o[4] = \"y\"; o[\"07\"] = 1; o[\"-1\"] = 2; o[\"1.5\"] = 3; o[\"4294967295\"] = 4; o[\"4294967294\"] = 5;\n"
                + "print(Object.keys(o).join(), Object.getOwnPropertyNames(o).join());\n"
                + "print(Object.entries(Object.assign({ z: 0 }, { \"9\": 9, \"1\": 1 })));\n"
                + "for (const [k, v] : Object.entries({ x: 1, \"0\": 2 })) { print(k, v); }";
        assertEquals("[\"2\", \"7\", \"10\", \"|\", \"a\"] [5, 2, 3, 1, 4] {2: 5, 7: 2, 10: 3, |: 1, a: 4}\n"
                + "4,30,4294967294,b,07,-1,1.5,4294967295 4,30,4294967294,b,07,-1,1.5,4294967295\n"
                + "[[\"1\", 1], [\"9\", 9], [\"z\", 0]]\n"
                + "0 2\nx 1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testShorthandNeedsADeclaredVariable() {
        assertRejected("const o = { undeclared };");
        assertRejected("function F() { const o = { this }; }");
    }
}
