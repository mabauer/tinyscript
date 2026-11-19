package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Differential transpiler tests: verifies that the Tinyscript generator
 * produces JavaScript whose observable output matches the interpreter.
 *
 * Level 1 differential tests: each test covers one language feature.
 * {@link #assertSameOutput(String)} runs the snippet through both the
 * interpreter and {@code node}, and compares stdout.  On failure, the generated
 * JavaScript is printed so it can be pasted into {@code node} directly to
 * reproduce without a build.
 *
 * <p>Program-level integration tests (Level 2) live in
 * {@link TranspilerVerifyTest}, which sorts after this class and runs last.
 */
public class TranspilerTest extends TinyscriptTranspilerTestBase {

    @BeforeEach
    @Override
    public void setUpInjector() {
        super.setUpInjector();
        // Node availability is checked lazily inside assertSameOutput /
        // assertTranspilerCorrect* — tests skip cleanly if node is absent.
    }

    // =========================================================================
    // Level 1 — focused feature tests
    // =========================================================================

    @Test
    public void testArithmetic() {
        assertSameOutput("print(2 + 3);");
        assertSameOutput("print(10 / 4);");
        assertSameOutput("print(7 - 3);");
        assertSameOutput("print(3 * 4);");
        assertSameOutput("print(10 % 3);");
    }

    @Test
    public void testStrings() {
        assertSameOutput("print(\"hello\" + \" world\");");
        assertSameOutput("print(1 + \"hello\");");
        assertSameOutput("print(\"hello\" + 1);");
    }

    @Test
    public void testRecursion() {
        String fibonacci =
                "function fibonacci(n) {\n"
                + "  if (n == 0 || n == 1) { return 1; }\n"
                + "  else { return fibonacci(n-2) + fibonacci(n-1); }\n"
                + "}\n"
                + "print(fibonacci(5));";
        assertSameOutput(fibonacci);

        String factorial =
                "function factorial(n) {\n"
                + "  if (n == 1) { return 1; }\n"
                + "  else { return n * factorial(n-1); }\n"
                + "}\n"
                + "print(factorial(5));";
        assertSameOutput(factorial);
    }

    @Test
    public void testClosures() {
        String script =
                "var makeCounter = function() {\n"
                + "  var value = 0;\n"
                + "  return {\n"
                + "    inc: function() { value = value + 1; },\n"
                + "    val: function() { return value; }\n"
                + "  };\n"
                + "};\n"
                + "var c = makeCounter();\n"
                + "c.inc();\n"
                + "c.inc();\n"
                + "c.inc();\n"
                + "print(c.val());";
        assertSameOutput(script);
    }

    @Test
    public void testArrowFunctions() {
        assertSameOutput("var f = (x) => x * 2; print(f(3));");
        assertSameOutput("var add = (a, b) => a + b; print(add(3, 4));");
    }

    @Test
    public void testClosuresOverFunctionDeclarations() {
        assertSameOutput(
                "function makeGetter(v) { function get() { return v; } return get; }\n"
                + "var a = makeGetter(1);\n"
                + "var b = makeGetter(2);\n"
                + "print(a());\n"
                + "print(b());");
    }

    @Test
    public void testReturnFromNestedBlocks() {
        assertSameOutput(
                "function f(x) { if (x > 0) { var y = x; return y; } return 0; }\n"
                + "function g() { var local = 42; var r = f(5); return local + r; }\n"
                + "print(g());");
    }

    @Test
    public void testBreak() {
        assertSameOutput("for (var i = 1, 10) { if (i == 3) { break; } print(i); }");
        assertSameOutput("for (var i = 1, 2) { for (var j = 1, 3) { if (j == 2) { break; } print(i + \"-\" + j); } }");
        assertSameOutput("for (var s : [\"a\", \"b\", \"stop\", \"c\"]) { if (s == \"stop\") { break; } print(s); }");
    }

    @Test
    public void testWhile() {
        assertSameOutput("var i = 0; while (i < 3) { print(i); i = i + 1; }");
        assertSameOutput("var i = 0; while (true) { i = i + 1; if (i > 2) { break; } print(i); }");
        assertSameOutput("for (var i = 1, 2) { var j = 0; while (j < 5) { if (j == 1) { break; } print(i + \"-\" + j); j = j + 1; } }");
    }

    @Test
    public void testStringComparison() {
        assertSameOutput("print(\"a\" < \"b\"); print(\"b\" <= \"a\"); print(\"B\" < \"a\"); print(\"ab\" > \"a\");");
    }

    @Test
    public void testArrayAddition() {
        assertSameOutput("var a = [1]; var b = a + [2, 3]; print(b.join(\",\")); print(b.length); print(a.length);");
        assertSameOutput("var b = [1] + [2]; print(b.map(x => x * 10).join(\",\"));");
    }

}
