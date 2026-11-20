package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Bitwise operators &, |, ^, ~, <<, >> on integers up to 53 bits (decided 2026-11-20). Unlike
 * Javascript, which works on 32 bits and cuts off larger numbers silently, tinyscript computes
 * them exactly or throws a TypeError; the generated Javascript computes the same with BigInt.
 */
public class BitwiseTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testBitwiseOperators() {
        String code = "print(12 & 10, \" \", 12 | 10, \" \", 12 ^ 10, \" \", ~5, \" \", 1 << 4, \" \", 100 >> 2);";
        assertEquals("8 14 6 -6 16 25\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNegativeNumbersInTwosComplement() {
        String code = "print(-1 & 255, \" \", -8 >> 1, \" \", -1 ^ 5, \" \", ~-1, \" \", -16 | 3);";
        assertEquals("255 -4 -6 0 -13\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testMoreThan32Bits() {
        // Javascript's operators would cut these numbers off at 32 bits
        String code = "const big = 1099511627776; // 2^40\n"
                + "print(big ^ 1, \" \", (big + 5) & 7, \" \", big | 3, \" \", big >> 20, \" \", 1 << 52, \" \", 8796093022207 ^ 4398046511104);";
        assertEquals("1099511627777 5 1099511627779 1048576 4503599627370496 4398046511103\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testPrecedenceAsInJavascript() {
        String code = "print(1 + 2 << 1, \" \", 6 & 3 | 8, \" \", 1 | 2 ^ 3 & 4, \" \", 16 >> 1 + 1, \" \", (5 & 1) == 1, \" \", 1 << 2 < 5);";
        assertEquals("6 10 3 4 true true\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testCompoundAssignments() {
        String code = "let x = 12; x &= 10; print(x); x |= 1; print(x); x ^= 15; print(x); x <<= 3; print(x); x >>= 1; print(x);\n"
                + "const o = { flags: 0 }; o.flags |= 4; o.flags |= 1; print(o.flags); const a = [1]; a[0] <<= 40; print(a[0]);";
        assertEquals("8\n9\n6\n48\n24\n5\n1099511627776\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    private void assertTypeError(String expectedMessage, String code) {
        TinyscriptRuntimeException e = assertThrows(TinyscriptTypeError.class, () -> interpreterOutput(code));
        assertEquals(expectedMessage, e.getMessage());
    }

    @Test
    public void testOperandsMustBeIntegersUpTo53Bits() {
        assertTypeError("'&' needs integers up to 2^53, but x is 1.5", "const x = 1.5;\nprint(x & 1);");
        assertTypeError("'^' needs integers up to 2^53, but s is a string", "const s = \"3\";\nprint(s ^ 1);");
        assertTypeError("'|' needs integers up to 2^53, but huge is 18014398509481984", "const huge = 18014398509481984;\nprint(huge | 1);");
        assertTypeError("'~' needs integers up to 2^53, but u is undefined", "let u;\nprint(~u);");
    }

    @Test
    public void testShifts() {
        assertTypeError("'<<' needs a shift count from 0 to 53, not -1", "print(1 << -1);");
        assertTypeError("'>>' needs a shift count from 0 to 53, not 54", "print(1 >> 54);");
        assertTypeError("'<<' gives more than 53 bits: 1099511627776 << 20", "print(1099511627776 << 20);");
    }
}
