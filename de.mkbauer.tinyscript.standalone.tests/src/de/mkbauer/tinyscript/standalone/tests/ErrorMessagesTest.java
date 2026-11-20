package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.TinyscriptDiagnostics;

/**
 * The errors reported for typical mistakes (many of them Javascript habits): only the one
 * that matters, with its position. The catalog grows with each improvement of the messages.
 */
public class ErrorMessagesTest extends TinyscriptTranspilerTestBase {

    // =========================================================================
    // Only the first syntax error, no follow-up errors
    // =========================================================================

    @Test
    public void testOnlyTheFirstSyntaxErrorIsReported() {
        // A C-style for loop used to give 2 syntax errors and 3 unresolved references
        List<String> errors = reportedErrors("let sum = 0;\nfor (let i = 0; i < 10; i++) {\n    sum += i;\n}");
        assertEquals(1, errors.size(), errors.toString());
        assertEquals("2:15: tinyscript's numeric for loop has no ';': for (let i = 0, n - 1) { … } counts from 0 to n - 1, including the end", errors.get(0));
    }

    @Test
    public void testNoFollowUpErrorsOfAnUnsupportedConstruct() {
        assertEquals(1, reportedErrors("function f(a, b = 2) { return a + b; }\nprint(f(1));").size());
        assertEquals(1, reportedErrors("try {\n    print(1);\n} catch (e) {\n    print(e);\n}").size());
        assertEquals(1, reportedErrors("const r = /a+/;").size());
    }

    @Test
    public void testAllErrorsWithoutSyntaxErrors() {
        // Without syntax errors, each error stands on its own: all of them, in the order of the source
        List<String> errors = reportedErrors("print(b);\nprint(a);");
        assertEquals(List.of("1:7: Couldn't resolve reference to Identifier 'b'.", "2:7: Couldn't resolve reference to Identifier 'a'."), errors);
    }

    // =========================================================================
    // Javascript habits get a hint what to write in tinyscript
    // =========================================================================

    private String firstError(String code) {
        List<String> errors = reportedErrors(code);
        assertEquals(1, errors.size(), errors.toString());
        return errors.get(0).replaceFirst("^\\d+:\\d+: ", "");
    }

    @Test
    public void testCStyleForLoop() {
        assertEquals("tinyscript's numeric for loop has no ';': for (let i = 0, n - 1) { … } counts from 0 to n - 1, including the end",
                firstError("let sum = 0;\nfor (let i = 0; i < 10; i++) {\n    sum += i;\n}"));
    }

    @Test
    public void testTripleEquals() {
        String message = "tinyscript has no === and !==: use == and !=, which never convert types";
        assertEquals(message, firstError("let x = 1;\nif (x === 1) { print(x); }"));
        assertEquals(message, firstError("let x = 1;\nif (x !== 1) { print(x); }"));
    }

    @Test
    public void testStatementWithoutBlock() {
        String message = "if, else, for and while need a block: if (…) { … }";
        assertEquals(message, firstError("let x = 1;\nif (x > 0) print(x);"));
        assertEquals(message, firstError("let x = 1;\nwhile (x > 0) x--;"));
    }

    @Test
    public void testForOfAndForIn() {
        String message = "A loop over the elements of an array, string, Map or Set is written for (const x : items) in tinyscript";
        assertEquals(message, firstError("const items = [1, 2];\nfor (const x of items) { print(x); }"));
        assertEquals(message, firstError("const o = { a: 1 };\nfor (const k in o) { print(k); }"));
    }

    @Test
    public void testTemplateString() {
        assertEquals("Template strings (`…`) are not supported: build strings with +, e.g. \"Hello \" + name",
                firstError("const name = \"x\";\nprint(`Hello ${name}`);"));
    }

    @Test
    public void testDefaultParameter() {
        assertEquals("Parameters cannot have default values: write b ?? 2 in the function",
                firstError("function f(a, b = 2) { return a + b; }"));
    }

    @Test
    public void testIncrementInsideAnExpression() {
        assertEquals("++ and -- are statements, not expressions: write i++; on its own",
                firstError("let i = 0;\nconst j = i++;"));
    }

    @Test
    public void testSpreadInACall() {
        assertEquals("... is only allowed for rest parameters: to call a function with an array, use f.apply(null, array)",
                firstError("const a = [1, 2];\nprint(Math.max(...a));"));
    }

    @Test
    public void testRegularExpression() {
        assertEquals("Regular expressions are not supported (std:string has words, integers and isDigit)",
                firstError("const r = /a+/;"));
    }

    @Test
    public void testUnsupportedKeywords() {
        assertEquals("try / catch is not supported in tinyscript: errors cannot be caught (throw ends the script)",
                firstError("try {\n    print(1);\n} catch (e) {\n    print(e);\n}"));
        assertEquals("switch is not supported in tinyscript: use if / else if",
                firstError("const x = 1;\nswitch (x) { case 1: print(1); }"));
        assertEquals("class is not supported in tinyscript: use a constructor function and its prototype",
                firstError("class Point {\n    constructor(x) { this.x = x; }\n}"));
    }

    @Test
    public void testReservedWordsAreFineAsPropertyNames() {
        String code = "const o = { class: \"a\", case: 1 }; print(o.class + o.case);";
        assertEquals("a1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    // =========================================================================
    // The other syntax errors in plain words
    // =========================================================================

    @Test
    public void testGeneralSyntaxErrors() {
        assertEquals("Missing ';' at the end of line 1", firstError("let x = 1\nprint(x);"));
        assertEquals("Missing ';' before 'print'", firstError("let x = 1 print(x);"));
        assertEquals("Unexpected end of input, expected '}'", firstError("function f(a) {\n    return a;\n"));
        assertEquals("Unexpected ')', expected an expression", firstError("print(1 + );"));
    }

    // =========================================================================
    // Excerpt of the source
    // =========================================================================

    @Test
    public void testExcerptMarksTheColumn() {
        String source = "let x = 1;\nif (x === 1) { print(x); }";
        assertEquals("    if (x === 1) { print(x); }\n          ^\n", TinyscriptDiagnostics.excerpt(source, 2, 7));
        assertEquals("", TinyscriptDiagnostics.excerpt(source, 9, 1));
    }

    @Test
    public void testExcerptKeepsTabs() {
        // Tabs before the column stay tabs, so that the marker lines up in a terminal
        assertEquals("    \tx = ;\n    \t    ^\n", TinyscriptDiagnostics.excerpt("\tx = ;", 1, 6));
    }
}
