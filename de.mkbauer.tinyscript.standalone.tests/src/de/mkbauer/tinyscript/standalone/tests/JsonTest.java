package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.interpreter.TinyscriptJsonError;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * JSON.parse as in Javascript; a text that is no JSON is an Error named SyntaxError that catch
 * gets.
 */
public class JsonTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testValues() {
        String code = "print(JSON.parse(\"[1, [2, [3, []]], 4]\"), JSON.parse(\"[]\"), JSON.parse(\"{}\"));\n"
                + "print(JSON.parse(\" { \\\"a\\\": 1, \\\"b\\\": [true, false, null], \\\"c\\\": { \\\"d\\\": \\\"e\\\" } } \"));\n"
                + "print(JSON.parse(\"-1.5e3\"), JSON.parse(\"0\"), JSON.parse(\"-0\") == 0, JSON.parse(\"12345678901234567890\"), JSON.parse(\"1E-2\"));\n"
                + "print(JSON.parse(\"\\\"a\\\\\\\"b\\\\\\\\c\\\\/d\\\\u00e9\\\\n\\\"\").length, JSON.parse(\"true\"), JSON.parse(\"null\"));\n"
                + "const o = JSON.parse(\"{\\\"b\\\": 1, \\\"1\\\": 2, \\\"b\\\": 3}\"); print(o, Object.keys(o));";
        assertEquals("[1, [2, [3, []]], 4] [] {}\n"
                + "{a: 1, b: [true, false, null], c: {d: \"e\"}}\n"
                + "-1500 0 true 12345678901234567000 0.01\n"
                + "9 true null\n"
                + "{1: 2, b: 3} [\"1\", \"b\"]\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testPacketsAsInAdventOfCode() {
        // AoC 2022, day 13: nested lists, one per line
        String code = "const lines = [\"[1,[2,[3,[4,[5,6,7]]]],8,9]\", \"[[[]]]\", \"[[1],4]\"];\n"
                + "for (const packet : lines.map(JSON.parse)) { print(packet, Array.isArray(packet), packet.length); }";
        assertEquals("[1, [2, [3, [4, [5, 6, 7]]]], 8, 9] true 4\n[[[]]] true 1\n[[1], 4] true 2\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testInvalidJsonCanBeCaught() {
        // As in Javascript, an Error named SyntaxError; the messages of node differ
        String code = "for (const text : [\"{a: 1}\", \"[1, 2,]\", \"'x'\", \"\", \"[1] 2\", \"01\", \"{\\\"a\\\" 1}\", \"\\\"\\n\\\"\"]) {\n"
                + "    try { JSON.parse(text); print(\"parsed\", text); } catch (e) { print(e.name, e instanceof Error); }\n"
                + "}";
        assertEquals("SyntaxError true\n".repeat(8), interpreterOutput(code));
        assertSameOutput(code);
    }

    private void assertMessage(String expected, String text) {
        TinyscriptJsonError e = assertThrows(TinyscriptJsonError.class, () -> interpreterOutput("JSON.parse(" + text + ");"));
        assertEquals(expected, e.getMessage());
    }

    @Test
    public void testMessages() {
        assertMessage("JSON.parse: expected a key in double quotes, not 'a' at position 1", "\"{a: 1}\"");
        assertMessage("JSON.parse: unexpected ']' at position 6 where a value is expected", "\"[1, 2,]\"");
        assertMessage("JSON.parse: the text ends where a value is expected", "\"\"");
        assertMessage("JSON.parse: unexpected '2' at position 4 after the value", "\"[1] 2\"");
        assertMessage("JSON.parse: unexpected '1' at position 1 after the value", "\"01\"");
        assertMessage("JSON.parse: expected : at position 5, not '1' at position 5", "\"{\\\"a\\\" 1}\"");
        assertMessage("JSON.parse: expected ] at position 2, not the end", "\"[1\"");
        assertMessage("JSON.parse: the string at position 0 has no closing \"", "\"\\\"abc\"");
        assertMessage("JSON.parse: unknown escape \\x at position 1", "\"\\\"\\\\x\\\"\"");
        assertMessage("JSON.parse: nested more than 1000 levels deep", "\"[\".repeat(1001)");
    }

    @Test
    public void testOnlyStrings() {
        assertInterpreterThrows(TinyscriptTypeError.class, "JSON.parse(5);");
        assertInterpreterThrows(TinyscriptTypeError.class, "JSON.parse(null);");
        // No reviver
        assertInterpreterThrows(TinyscriptTypeError.class, "JSON.parse(\"1\", (k, v) => v);");
    }
}
