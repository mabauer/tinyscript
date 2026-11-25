package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * The methods of numbers, as in Javascript: x.toFixed(digits) and x.toString(radix)
 * (decided 2026-11-21).
 */
public class NumberMethodsTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testToFixed() {
        String code = "print((1234.5).toFixed(2), (2 / 3).toFixed(3), (123.456).toFixed(), (0).toFixed(2), (0.000001).toFixed(7));\n"
                + "print((1.005).toFixed(2), (2.5).toFixed(0), (-2.5).toFixed(0), (0.125).toFixed(2), (1.45).toFixed(1));\n"
                + "print((-0.001).toFixed(2), (-0).toFixed(1), (-1.5).toFixed(3), (1e21).toFixed(2), (1 / 0).toFixed(1), (0 / 0).toFixed(2));\n"
                + "print((0.1).toFixed(20), (123.456).toFixed(100).length);";
        assertSameOutput("1234.50 0.667 123 0.00 0.0000010\n"
                + "1.00 3 -3 0.13 1.4\n"
                + "-0.00 0.0 -1.500 1e+21 Infinity NaN\n"
                + "0.10000000000000000555 104\n", code);
    }

    @Test
    public void testToString() {
        String code = "print((255).toString(16), (5).toString(2), (-255).toString(16), (0).toString(2), (35).toString(36), (8).toString(8));\n"
                + "print((9007199254740991).toString(2), (1e21).toString(16), (2.5).toString(), (12345678.5).toString(10), (1e21).toString());";
        assertSameOutput("ff 101 -ff 0 z 10\n"
                + "11111111111111111111111111111111111111111111111111111 3635c9adc5dea00000 2.5 12345678.5 1e+21\n", code);
    }

    @Test
    public void testMethodsOnVariablesAndInExpressions() {
        String code = "const price = 3.14159; const n = 7; let u;\n"
                + "print(price.toFixed(2).padStart(8, \".\"), n.toString(2).padStart(8, \"0\"));\n"
                + "print([1, 2.5, 1 / 3].map(x => x.toFixed(1)).join(\" \"), (n * 2).toFixed(1), \"#\" + (255).toString(16));\n"
                + "print(typeof n.toFixed, n?.toFixed(1), u?.toFixed(1));";
        assertSameOutput("....3.14 00000111\n1.0 2.5 0.3 14.0 #ff\nfunction 7.0 undefined\n", code);
    }

    @Test
    public void testWrongArguments() {
        assertSameFailure(TinyscriptTypeError.class, "(1).toFixed(1.5);");
        assertSameFailure(TinyscriptTypeError.class, "(1).toFixed(\"2\");");
        assertSameFailure(TinyscriptTypeError.class, "(1).toFixed(101);");
        assertSameFailure(TinyscriptTypeError.class, "(1).toFixed(-1);");
        assertSameFailure(TinyscriptTypeError.class, "(1.5).toString(2);");
        assertSameFailure(TinyscriptTypeError.class, "(1 / 0).toString(2);");
        assertSameFailure(TinyscriptTypeError.class, "(1).toString(37);");
        assertSameFailure(TinyscriptTypeError.class, "(1).toString(1);");
        assertEquals(java.util.List.of("1:1: toFixed() takes at most 1 argument, not 2"), reportedErrors("(1).toFixed(1, 2);"));
        assertSameFailure(TinyscriptTypeError.class, "Number.prototype.toFixed.call({}, 2);");
    }

    @Test
    public void testNumbersOnlyHaveTheirMethods() {
        assertKnownGap(TinyscriptTypeError.class, "const n = 5; print(n.length);");
        assertSameFailure(TinyscriptTypeError.class, "const n = 5; n.x = 1;");
        assertSameFailure(TinyscriptTypeError.class, "const n = 5; n.toFixed = 1;");
        assertKnownGap(TinyscriptTypeError.class, "const n = 5; print(n.constructor);");
    }
}
