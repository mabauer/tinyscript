package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * const declares a variable that cannot be assigned again; let is a synonym of var.
 */
public class ConstTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testConstants() {
        String code = "const answer = 42; const [a, b] = [1, 2]; const { length } = \"abc\";\n"
                + "print(answer + a + b + length);\n"
                + "const list = []; list.push(1); list[0] = 2; print(list.join());\n"
                + "for (const x : [1, 2]) { print(x); }\n"
                + "for (const i = 1, 2) { print(i * 10); }\n"
                + "for (const [k, v] : new Map([[\"k\", \"v\"]])) { print(k + v); }";
        assertEquals("48\n2\n1\n2\n10\n20\nkv\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testLetAndVarAreTheSame() {
        String code = "let x = 1; var y = 2; { let x = 10; x += y; print(x); } print(x);";
        assertEquals("12\n1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testConstantsCannotBeAssigned() {
        assertRejected("const x = 1; x = 2;");
        assertRejected("const x = 1; x += 2;");
        assertRejected("const x = 1; x++;");
        assertRejected("const [a, b] = [1, 2]; a = 3;");
        assertRejected("const { a } = { a: 1 }; a--;");
        assertRejected("for (const i = 1, 3) { i = 5; }");
        assertRejected("for (const x : [1]) { x = 2; }");
        assertRejected("const x = 1; for (x : [1, 2]) { }");
        assertRejected("const x = 1; for (x = 1, 2) { }");
        assertRejected("const x = 1; function f() { x = 2; }");
        assertRejected("const x = 1; var g = () => { x = 2; };");
    }

    @Test
    public void testAConstantNeedsAValue() {
        assertRejected("const x;");
        assertRejected("const x = 1, y;");
    }

    @Test
    public void testShadowingAConstantIsFine() {
        String code = "const x = 1; { let x = 2; x = 3; print(x); } function f(x) { x = 5; return x; } print(f(0), x);";
        assertEquals("3\n5 1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testExportedConstants() {
        assertEquals("1\n", interpreterOutput("export const one = 1; print(one);"));
    }
}
