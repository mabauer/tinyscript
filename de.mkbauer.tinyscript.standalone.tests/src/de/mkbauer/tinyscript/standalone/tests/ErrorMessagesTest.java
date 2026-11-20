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
        assertEquals("2:15: mismatched input ';' expecting ','", errors.get(0));
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
