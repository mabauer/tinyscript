package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Number literals as in Javascript: hexadecimal, binary and octal, and separators between
 * digits.
 */
public class NumberLiteralsTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testHexadecimalBinaryAndOctal() {
        String code = "print(0xff, 0XFF, 0x0, 0b1010, 0B11, 0o17, 0O777, 0b0011110 | 0b1000000);\n"
                + "print(0xFFFFFFFFFFFFFFFF, 0x1fffffffffffff, 0b1 + 0o1 + 0x1, -0x10);";
        assertSameOutput("255 255 0 10 3 15 511 94\n"
                + "18446744073709552000 9007199254740991 3 -16\n", code);
    }

    @Test
    public void testSeparators() {
        String code = "print(1_000_000, 1_000_000_000_000, 3.141_592, 1e1_0, 0xff_ff, 0b1111_0000, 0o7_7, .0_5);";
        assertSameOutput("1000000 1000000000000 3.141592 10000000000 65535 240 63 0.05\n", code);
    }

    @Test
    public void testDecimalNumbersAsBefore() {
        String code = "print(42, 3.14, .5, 5., 1e3, 1E-3, 2.5e+2, 0, 0.0, 10, 0e1, 0.5e1, 0x1e+5);";
        assertSameOutput("42 3.14 0.5 5 1000 0.001 250 0 0 10 0 5 35\n", code);
    }

    private String firstError(String code) {
        return reportedErrors(code).get(0).replaceFirst("^\\d+:\\d+: ", "");
    }

    @Test
    public void testMistakes() {
        String separator = "A _ in a number must stand between two digits, as in 1_000_000";
        for (String number : new String[] { "1_", "1__0", "1_.5", "0x_1", "1e_3", "1e1_", "0b1_" })
            assertEquals(separator, firstError("print(" + number + ");"), number);
        assertEquals("0x needs digits: 0xff", firstError("print(0x);"));
        assertEquals("0b needs digits: 0b1010", firstError("print(0b);"));
        assertEquals("0b102: a binary number has only the digits 0 and 1", firstError("print(0b102);"));
        assertEquals("0o8: an octal number has only the digits 0 to 7", firstError("print(0o8);"));
    }

    @Test
    public void testNoLeadingZero() {
        // In Javascript (outside of strict mode), 017 is the octal number 15
        assertEquals("017: a number cannot start with 0 (in Javascript, it may be an octal number): write 17, or 0o17 for an octal number",
                firstError("print(017);"));
        assertEquals("08: a number cannot start with 0 (in Javascript, it may be an octal number): write 8", firstError("print(08);"));
        assertEquals("0_1: a number cannot start with 0 (in Javascript, it may be an octal number): write 1, or 0o1 for an octal number",
                firstError("print(0_1);"));
    }
}
