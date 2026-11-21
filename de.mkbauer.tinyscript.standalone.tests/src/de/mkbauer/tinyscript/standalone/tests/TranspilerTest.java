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
                "let makeCounter = function() {\n"
                + "  let value = 0;\n"
                + "  return {\n"
                + "    inc: function() { value = value + 1; },\n"
                + "    val: function() { return value; }\n"
                + "  };\n"
                + "};\n"
                + "let c = makeCounter();\n"
                + "c.inc();\n"
                + "c.inc();\n"
                + "c.inc();\n"
                + "print(c.val());";
        assertSameOutput(script);
    }

    @Test
    public void testArrowFunctions() {
        assertSameOutput("let f = (x) => x * 2; print(f(3));");
        assertSameOutput("let add = (a, b) => a + b; print(add(3, 4));");
    }

    @Test
    public void testClosuresOverFunctionDeclarations() {
        assertSameOutput(
                "function makeGetter(v) { function get() { return v; } return get; }\n"
                + "let a = makeGetter(1);\n"
                + "let b = makeGetter(2);\n"
                + "print(a());\n"
                + "print(b());");
    }

    @Test
    public void testReturnFromNestedBlocks() {
        assertSameOutput(
                "function f(x) { if (x > 0) { let y = x; return y; } return 0; }\n"
                + "function g() { let local = 42; let r = f(5); return local + r; }\n"
                + "print(g());");
    }

    @Test
    public void testBreak() {
        assertSameOutput("for (const i : range(1, 11)) { if (i == 3) { break; } print(i); }");
        assertSameOutput("for (const i : range(1, 3)) { for (const j : range(1, 4)) { if (j == 2) { break; } print(i + \"-\" + j); } }");
        assertSameOutput("for (let s : [\"a\", \"b\", \"stop\", \"c\"]) { if (s == \"stop\") { break; } print(s); }");
    }

    @Test
    public void testElseIf() {
        assertSameOutput("function sign(n) { if (n < 0) { return \"negative\"; } else if (n == 0) { return \"zero\"; } else { return \"positive\"; } }"
                + " print(sign(-1)); print(sign(0)); print(sign(1));");
        assertSameOutput("for (const x : range(1, 5)) { if (x == 1) { print(\"a\"); } else if (x == 2) { print(\"b\"); } else if (x == 3) { print(\"c\"); } }");
        assertSameOutput("for (const i : range(1, 6)) { if (i == 2) { print(\"two\"); } else if (i == 3) { break; } else { print(i); } }");
    }

    @Test
    public void testIterableForWithoutVar() {
        String code = "let x; for (x : [1, 2]) { print(x); } print(x);";
        assertEquals("1\n2\n2\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNewWithoutParentheses() {
        String code = "function Foo() { this.x = 1; }\n"
                + "let o = new Foo;\n"
                + "print(o.x); print(o instanceof Foo);\n"
                + "let a = new Array; print(a.length);";
        assertEquals("1\ntrue\n0\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testIterableIsEvaluatedOnce() {
        String code = "function f() { print(\"f\"); return [1, 2]; } for (let x : f()) { print(x); }";
        assertEquals("f\n1\n2\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNestedIterableLoops() {
        String code = "for (let a : [1, 2]) { for (let b : [3, 4]) { print(a + \"-\" + b); } }";
        assertEquals("1-3\n1-4\n2-3\n2-4\n", interpreterOutput(code));
        assertSameOutput(code);
    }


    @Test
    public void testStringKeysInObjectLiterals() {
        String code = "let o = { \"a b\": 1, c: 2 }; print(o[\"a b\"]); print(o.c);";
        assertEquals("1\n2\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNewWithParenthesesAndMethodCall() {
        String code = "function Foo(x) { this.x = x; }\n"
                + "Foo.prototype.get = function() { return this.x; };\n"
                + "print(new Foo(2).x); print(new Foo(3).get());\n"
                + "let ns = { Bar: function(y) { this.y = y; } }; print(new ns.Bar(4).y);";
        assertEquals("2\n3\n4\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testArrayGapsAreFilledWithUndefined() {
        String code = "let a = [1]; a[3] = 4; print(a.length); print(a[2] == null); print(a[3]);";
        assertEquals("4\ntrue\n4\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testSettingArrayLengthTruncates() {
        String code = "let a = [1, 2, 3]; a.length = 1; print(a.length); print(a[1] == null); print(a.join(\",\"));";
        assertEquals("1\ntrue\n1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testSettingArrayLengthGrows() {
        String code = "let a = [1]; a.length = 3; print(a.length); print(a[2] == null); a.push(4); print(a[3]);";
        assertEquals("3\ntrue\n4\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testReadingNonIndexKeysOfArraysIsUndefined() {
        String code = "let a = [1]; print(a[-1] == null); print(a[1.5] == null); print(a.x == null); print(a.length);";
        assertEquals("true\ntrue\ntrue\n1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testArbitraryExpressionsInArrayLiterals() {
        String code = "let x = 1; let y = 2;\n"
                + "let a = [x > y, !(x > y), x == 1, f => f * 2];\n"
                + "print(a[0]); print(a[1]); print(a[2]); print(a[3](4));";
        assertEquals("false\ntrue\ntrue\n8\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testArbitraryExpressionsAsIndex() {
        String code = "let o = {}; let a = [10, 20]; let i;\n"
                + "o[1 < 2] = \"lt\"; print(o[\"true\"]);\n"
                + "print(a[i = 1]); print(i);";
        assertEquals("lt\n20\n1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNestedUnaryOperators() {
        String code = "let x = 3; let b = true; let o = {};\n"
                + "print(!!b); print(!!o); print(!!null); print(- -x); print(-(-x)); print(typeof -x); print(typeof !b);";
        assertEquals("true\ntrue\nfalse\n3\n3\nnumber\nboolean\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testEscapesInStringLiterals() {
        String code = "let s = \"a\\nb\\t\\\"q\\\" \\\\ \\u00e4\";\nprint(s); print(s.length); print('it\\'s');";
        assertEquals("a\nb\t\"q\" \\ ä\n11\nit's\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testPrintWithSeveralArguments() {
        String code = "print(\"a\", 1, true); print(1, 2); print(\"x\" + 1, \"y\"); print(); print(2.5, -1);";
        assertEquals("a 1 true\n1 2\nx1 y\n\n2.5 -1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testWhile() {
        assertSameOutput("let i = 0; while (i < 3) { print(i); i = i + 1; }");
        assertSameOutput("let i = 0; while (true) { i = i + 1; if (i > 2) { break; } print(i); }");
        assertSameOutput("for (const i : range(1, 3)) { let j = 0; while (j < 5) { if (j == 1) { break; } print(i + \"-\" + j); j = j + 1; } }");
    }

    @Test
    public void testStringComparison() {
        assertSameOutput("print(\"a\" < \"b\"); print(\"b\" <= \"a\"); print(\"B\" < \"a\"); print(\"ab\" > \"a\");");
    }

    @Test
    public void testArrayAddition() {
        assertSameOutput("let a = [1]; let b = a + [2, 3]; print(b.join(\",\")); print(b.length); print(a.length);");
        assertSameOutput("let b = [1] + [2]; print(b.map(x => x * 10).join(\",\"));");
    }

}
