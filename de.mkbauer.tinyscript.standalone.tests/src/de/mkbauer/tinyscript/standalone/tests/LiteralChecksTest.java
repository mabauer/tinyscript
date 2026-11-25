package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Checks before the run of operators and conditions whose operands can never fit, by the syntax
 * alone (literals and the results of operators): they fail whenever they run, in both backends
 * (TODO.md, second backend, static checks level 1).
 */
public class LiteralChecksTest extends TinyscriptTranspilerTestBase {

    private String onlyError(String code) {
        List<String> errors = reportedErrors(code);
        assertEquals(1, errors.size(), errors.toString());
        return errors.get(0).replaceFirst("^\\d+:\\d+: ", "");
    }

    private String onlyWarning(String code) {
        List<String> warnings = reportedWarnings(code);
        assertEquals(1, warnings.size(), warnings.toString());
        return warnings.get(0).replaceFirst("^\\d+:\\d+: ", "").replaceFirst(" \\(line \\d+\\)$", "");
    }

    @Test
    public void testArithmetic() {
        assertEquals("'-' needs two numbers, but \"a\" is a string", onlyError("let x = 1;\nprint(\"a\" - x);"));
        assertEquals("'*' needs two numbers, but true is a boolean", onlyError("let x = 1;\nprint(x * true);"));
        assertEquals("'%' needs two numbers, but null is null", onlyError("let x = 1;\nprint(x % null);"));
        assertEquals("'-' needs two numbers, but (x > 1) is a boolean", onlyError("let x = 1;\nprint((x > 1) - 1);"));
        assertEquals("'&' needs integers, but \"1\" is a string", onlyError("let x = 1;\nprint(x & \"1\");"));
        assertEquals("'-' needs two numbers, but \"a\" is a string", onlyError("let x = 1;\nx -= \"a\";"));
        assertEquals("'-' needs a number, but \"a\" is a string", onlyError("print(-\"a\");"));
    }

    @Test
    public void testPlusAndComparisons() {
        assertEquals("'+' can only add an array to another array or to a string, but 1 is a number", onlyError("print([1] + 1);"));
        assertEquals("'+' adds two numbers, two arrays, or a string and a value, but 1 is a number and true is a boolean",
                onlyError("print(1 + true);"));
        assertEquals("'+' adds two numbers, two arrays, or a string and a value, but {} is an object and 1 is a number",
                onlyError("print({} + 1);"));
        assertEquals("'<' compares two numbers or two strings, but 1 is a number and \"2\" is a string", onlyError("print(1 < \"2\");"));
        assertEquals("'>=' compares two numbers or two strings, but null is null", onlyError("let x = 1;\nprint(x >= null);"));
        assertEquals("'<' compares two numbers or two strings, but 1 + \"a\" is a string and 2 is a number", onlyError("print(1 + \"a\" < 2);"));
    }

    @Test
    public void testConditions() {
        assertEquals("The condition 1 is a number, not a boolean: compare it, e.g. 1 > 0", onlyError("if (1) { print(1); }"));
        assertEquals("The condition \"\" is a string, not a boolean: compare it, e.g. \"\" != \"\"", onlyError("while (\"\") { }"));
        assertEquals("The condition n - 1 is a number, not a boolean: compare it, e.g. n - 1 > 0", onlyError("let n = 2;\nwhile (n - 1) { n--; }"));
        assertEquals("The condition 0 is a number, not a boolean: compare it, e.g. 0 > 0", onlyError("print(!0);"));
        assertEquals("The condition 1 is a number, not a boolean: compare it, e.g. 1 > 0", onlyError("assert(1);"));
        assertEquals("The condition 1 is a number, not a boolean: compare it, e.g. 1 > 0", onlyError("print(1 ? 2 : 3);"));
        assertEquals("The condition typeof x is a string, not a boolean: compare it, e.g. typeof x != \"\"",
                onlyError("let x;\nprint(x != null && typeof x);"));
        assertEquals("'=' assigns a value: to compare, write == (or, to assign here, put the assignment in parentheses)",
                onlyError("let x = 1;\nif (x = 2) { print(x); }"));
    }

    @Test
    public void testMathNeedsNumbers() {
        assertEquals("Math functions need numbers, but \"4\" is a string", onlyError("print(Math.sqrt(\"4\"));"));
        assertEquals("Math functions need numbers, but [1] is an array", onlyError("print(Math.max(1, [1]));"));
    }

    @Test
    public void testWarnings() {
        assertEquals("x == NaN is always false (NaN is not equal to itself): use isNaN(x)", onlyWarning("let x = 1;\nprint(x == NaN);"));
        assertEquals("NaN != x is always true (NaN is not equal to itself): use !isNaN(x)", onlyWarning("let x = 1;\nprint(NaN != x);"));
        assertEquals("typeof never gives \"numbr\", so this is always false (did you mean \"number\"?)",
                onlyWarning("let x = 1;\nprint(typeof x == \"numbr\");"));
        assertEquals("typeof never gives \"array\", so this is always true", onlyWarning("let x = 1;\nprint(typeof x != \"array\");"));
    }

    @Test
    public void testWhatStaysAllowed() {
        // Unknown operands (variables, properties, calls), assignments in parentheses, and
        // everything that fits
        String code = "let x = 3;\nconst s = \"a\";\nconst a = [1];\nlet n = null;\n"
                + "print(x - 1, s + 1, 1 + s, a + [2], \"\" + a, x < 4, \"a\" < \"b\", -x, -(-2), typeof x == \"number\");\n"
                + "let y;\nif ((y = x * 2) > 5) { print(y); }\n"
                + "while (n) { }\nif (a) { print(\"array\"); }\nprint(x > 0 && s != \"\", !n, n ?? 1);";
        assertEquals(List.of(), reportedErrors(code));
        assertEquals(List.of(), reportedWarnings(code));
        assertSameOutput("2 a1 1a [1, 2] [1] true true -3 2 true\n6\narray\ntrue true 1\n", code);
    }
}
