package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import de.mkbauer.tinyscript.interpreter.ResourceLimits;
import de.mkbauer.tinyscript.interpreter.TinyscriptResourceLimitViolation;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * String methods and number conversion (step 1 of the Advent of Code plan in TODO.md).
 */
public class StringsAndNumbersTest extends TinyscriptTranspilerTestBase {

    // =========================================================================
    // Strings
    // =========================================================================

    @Test
    public void testSplitAndTrim() {
        String code = "var s = \"  a,b,,c \\n\";\n"
                + "print(\"[\" + s.trim() + \"]\"); print(s.trim().split(\",\").length); print(s.trim().split(\",\").join(\"|\"));\n"
                + "print(\"abc\".split(\"\").join(\"-\")); print(\"a b  c\".split(\" \").length); print(\"\".split(\",\").length);";
        assertEquals("[a,b,,c]\n4\na|b||c\na-b-c\n4\n1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testSearching() {
        String code = "print(\"abc\".startsWith(\"ab\"), \"abc\".endsWith(\"bc\"), \"abc\".includes(\"x\"), \"abc\".includes(\"\"));\n"
                + "print(\"abcabc\".lastIndexOf(\"b\")); print(\"abc\".lastIndexOf(\"x\"));";
        assertEquals("true true false true\n4\n-1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testReplace() {
        String code = "print(\"a-b-c\".replace(\"-\", \"+\")); print(\"a-b-c\".replaceAll(\"-\", \"+\")); print(\"aaa\".replaceAll(\"a\", \"aa\"));\n"
                + "print(\"a.b\".replace(\".\", \"$\")); print(\"abc\".replace(\"x\", \"y\"));";
        assertEquals("a+b-c\na+b+c\naaaaaa\na$b\nabc\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testReplacementPatternsAndEmptySearch() {
        String code = "print(\"a-b\".replace(\"-\", \"[$&]\")); print(\"a-b\".replace(\"-\", \"$$\")); print(\"a-b\".replace(\"-\", \"<$`|$'>\"));\n"
                + "print(\"ab\".replaceAll(\"\", \"-\")); print(\"ab\".replace(\"\", \"-\")); print(\"abcb\".lastIndexOf(\"b\", 2));";
        assertEquals("a[-]b\na$b\na<a|b>b\n-a-b-\n-ab\n1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testSlice() {
        String code = "print(\"hello\".slice(1, 3)); print(\"hello\".slice(-3)); print(\"hello\".slice(2)); print(\"[\" + \"hello\".slice(3, 1) + \"]\");\n"
                + "print(\"hello\".slice(-10, 2)); print(\"hello\".slice(1, -1));";
        assertEquals("el\nllo\nllo\n[]\nhe\nell\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testRepeatPadAndCase() {
        String code = "print(\"ab\".repeat(3)); print(\"[\" + \"ab\".repeat(0) + \"]\"); print(\"7\".padStart(3, \"0\")); print(\"[\" + \"x\".padStart(3) + \"]\");\n"
                + "print(\"abc\".padStart(2, \"0\")); print(\"5\".padStart(4, \"ab\")); print(\"Ab\".toUpperCase() + \"Ab\".toLowerCase());\n"
                + "print(\"[\" + \"x\".padEnd(3) + \"]\"); print(\"ab\".padEnd(5, \"xy\"));";
        assertEquals("ababab\n[]\n007\n[  x]\nabc\naba5\nABab\n[x  ]\nabxyx\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testCharCodes() {
        String code = "print(\"a\".charCodeAt(0)); print(\"abc\".charCodeAt(2)); print(isNaN(\"a\".charCodeAt(5)));\n"
                + "print(String.fromCharCode(72, 105)); print(String.fromCharCode(\"a\".charCodeAt(0) + 1));";
        assertEquals("97\n99\ntrue\nHi\nb\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testStringMethodsCheckTheirArguments() {
        assertInterpreterThrows(TinyscriptTypeError.class, "\"a,b\".split(1);");
        assertInterpreterThrows(TinyscriptTypeError.class, "\"a\".repeat(-1);");
        assertInterpreterThrows(TinyscriptTypeError.class, "\"a\".repeat(1.5);");
        assertInterpreterThrows(TinyscriptTypeError.class, "\"a\".repeat(\"2\");");
        assertInterpreterThrows(TinyscriptTypeError.class, "\"a\".startsWith(1);");
        assertInterpreterThrows(TinyscriptTypeError.class, "\"a\".replace(\"a\", 1);");
        assertInterpreterThrows(TinyscriptTypeError.class, "\"a\".padStart(3, 5);");
        assertInterpreterThrows(TinyscriptTypeError.class, "\"a\".slice(\"1\");");
        assertInterpreterThrows(TinyscriptTypeError.class, "String.fromCharCode(\"a\");");
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testLongStringsAreCheckedBeforeTheyAreBuilt() {
        ResourceLimits limits = new ResourceLimits();
        limits.setMaxStringLength(1000);
        assertThrows(TinyscriptResourceLimitViolation.class, () -> interpreterOutput("\"ab\".repeat(1000000000);", limits));
        assertThrows(TinyscriptResourceLimitViolation.class, () -> interpreterOutput("\"ab\".padStart(1000000000);", limits));
    }

    // =========================================================================
    // Numbers: explicit conversion
    // =========================================================================

    @Test
    public void testNumber() {
        String code = "print(Number(\"42\") + 1); print(Number(\" 3.5 \")); print(Number(\"-1e3\")); print(Number(\"0x1f\"));\n"
                + "print(isNaN(Number(\"abc\"))); print(isNaN(Number(\"12px\"))); print(Number(7)); print(Number(true) + Number(false));";
        assertEquals("43\n3.5\n-1000\n31\ntrue\ntrue\n7\n1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNumberOfAnEmptyStringIsNaN() {
        // Javascript gives 0, a trap
        assertEquals("true\ntrue\n", interpreterOutput("print(isNaN(Number(\"\"))); print(isNaN(Number(\"   \")));"));
    }

    @Test
    public void testParseInt() {
        String code = "print(parseInt(\"42abc\")); print(parseInt(\"  -17\")); print(parseInt(\"+5\")); print(parseInt(\"3.9\"));\n"
                + "print(parseInt(\"ff\", 16)); print(parseInt(\"0x1f\")); print(parseInt(\"101\", 2)); print(parseInt(\"z\", 36));\n"
                + "print(isNaN(parseInt(\"x\"))); print(isNaN(parseInt(\"\"))); print(parseInt(\"12345678901234\"));";
        assertEquals("42\n-17\n5\n3\n255\n31\n5\n35\ntrue\ntrue\n12345678901234\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testIsNaNAndIsInteger() {
        String code = "print(isNaN(0 / 0), isNaN(1)); print(Number.isInteger(5), Number.isInteger(5.5), Number.isInteger(\"5\"), Number.isInteger(1 / 0));";
        assertEquals("true false\ntrue false false false\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testConversionsCheckTheirArguments() {
        assertInterpreterThrows(TinyscriptTypeError.class, "Number(null);");
        assertInterpreterThrows(TinyscriptTypeError.class, "var u; Number(u);");
        assertInterpreterThrows(TinyscriptTypeError.class, "Number({});");
        assertInterpreterThrows(TinyscriptTypeError.class, "parseInt(42);");
        assertInterpreterThrows(TinyscriptTypeError.class, "parseInt(\"1\", 1);");
        assertInterpreterThrows(TinyscriptTypeError.class, "parseInt(\"1\", 37);");
        assertInterpreterThrows(TinyscriptTypeError.class, "isNaN(\"x\");");
    }
}
