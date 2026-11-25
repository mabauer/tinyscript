package de.mkbauer.tinyscript.standalone.tests;


import org.junit.jupiter.api.Test;

/**
 * const declares a variable that cannot be assigned again; let one that can.
 */
public class ConstTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testConstants() {
        String code = "const answer = 42; const [a, b] = [1, 2]; const { length } = \"abc\";\n"
                + "print(answer + a + b + length);\n"
                + "const list = []; list.push(1); list[0] = 2; print(list.join());\n"
                + "for (const x : [1, 2]) { print(x); }\n"
                + "for (const i : range(1, 3)) { print(i * 10); }\n"
                + "for (const [k, v] : new Map([[\"k\", \"v\"]])) { print(k + v); }";
        assertSameOutput("48\n2\n1\n2\n10\n20\nkv\n", code);
    }

    @Test
    public void testLetAndVarAreTheSame() {
        String code = "let x = 1; let y = 2; { let x = 10; x += y; print(x); } print(x);";
        assertSameOutput("12\n1\n", code);
    }

    @Test
    public void testConstantsCannotBeAssigned() {
        assertRejected("const x = 1; x = 2;");
        assertRejected("const x = 1; x += 2;");
        assertRejected("const x = 1; x++;");
        assertRejected("const [a, b] = [1, 2]; a = 3;");
        assertRejected("const { a } = { a: 1 }; a--;");
        assertRejected("for (const i : range(1, 4)) { i = 5; }");
        assertRejected("for (const x : [1]) { x = 2; }");
        assertRejected("const x = 1; for (x : [1, 2]) { }");
        assertRejected("const x = 1; for (x : range(1, 3)) { }");
        assertRejected("const x = 1; function f() { x = 2; }");
        assertRejected("const x = 1; let g = () => { x = 2; };");
    }

    @Test
    public void testAConstantNeedsAValue() {
        assertRejected("const x;");
        assertRejected("const x = 1, y;");
    }

    @Test
    public void testShadowingAConstantIsFine() {
        String code = "const x = 1; { let x = 2; x = 3; print(x); } function f(x) { x = 5; return x; } print(f(0), x);";
        assertSameOutput("3\n5 1\n", code);
    }

    @Test
    public void testExportedConstants() {
        assertSameOutput("1\n", "export const one = 1; print(one);");
    }
}
