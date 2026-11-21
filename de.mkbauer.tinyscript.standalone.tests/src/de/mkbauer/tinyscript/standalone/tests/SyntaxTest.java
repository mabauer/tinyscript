package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * continue, the conditional operator, compound assignments and ++ / -- statements
 * (step 3 of the Advent of Code plan in TODO.md).
 */
public class SyntaxTest extends TinyscriptTranspilerTestBase {

    // =========================================================================
    // continue
    // =========================================================================

    @Test
    public void testContinueInAllLoops() {
        String code = "var s = \"\";\n"
                + "for (var i = 1, 6) { if (i % 2 == 0) { continue; } s = s + i; }\n"
                + "for (var x : [\"a\", \"b\", \"c\"]) { if (x == \"b\") { continue; } s = s + x; }\n"
                + "var n = 0; while (n < 5) { n = n + 1; if (n == 3) { continue; } s = s + n; }\n"
                + "print(s);";
        assertEquals("135ac1245\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testContinueOnlyLeavesTheInnermostIteration() {
        String code = "var s = \"\";\n"
                + "for (var i = 1, 3) { for (var j = 1, 3) { if (j == 2) { continue; } s = s + i + j + \" \"; } s = s + \"| \"; }\n"
                + "print(s);";
        assertEquals("11 13 | 21 23 | 31 33 | \n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testContinueOnlyInsideALoop() {
        assertRejected("continue;");
        assertRejected("for (var i = 1, 3) { var f = function() { continue; }; }");
    }

    // =========================================================================
    // cond ? a : b
    // =========================================================================

    @Test
    public void testConditionalOperator() {
        String code = "function sign(x) { return x > 0 ? 1 : x < 0 ? -1 : 0; }\n"
                + "print(sign(5), sign(-2), sign(0));\n"
                + "var max = (a, b) => a > b ? a : b; print(max(3, 7));\n"
                + "var y = true ? \"yes\" : \"no\"; print(y); print([1, 2, 3].map(x => x % 2 == 0 ? \"e\" : \"o\").join(\"\"));\n"
                + "print(\"n=\" + (1 < 2 ? \"a\" : \"b\"));";
        assertEquals("1 -1 0\n7\nyes\noeo\nn=a\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testConditionalOperatorEvaluatesOnlyOneBranch() {
        String code = "var calls = \"\"; function f(s) { calls = calls + s; return s; }\n"
                + "var r = false ? f(\"a\") : f(\"b\"); print(r + \" \" + calls);";
        assertEquals("b b\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testConditionalOperatorNeedsACondition() {
        assertInterpreterThrows(TinyscriptTypeError.class, "var x = 1 ? 2 : 3;");
    }

    @Test
    public void testConditionalOperatorWithFunctionsAndAssignments() {
        String code = "var f = false ? x => x : x => x * 2; print(f(4));\n"
                + "var a; var b = true ? a = 1 : 2; print(a + b);";
        assertEquals("8\n2\n", interpreterOutput(code));
        assertSameOutput(code);
        // Javascript reads this as c ? a : (b = 1)
        assertRejected("var a; var b; var c = true; c ? a : b = 1;");
    }

    // =========================================================================
    // Compound assignments
    // =========================================================================

    @Test
    public void testCompoundAssignments() {
        String code = "var x = 10; x += 5; print(x); x -= 3; print(x); x *= 2; print(x); x /= 4; print(x); x %= 4; print(x);\n"
                + "var s = \"a\"; s += \"b\"; s += 1; print(s);\n"
                + "var a = [1]; a += [2, 3]; print(a.length);\n"
                + "var o = { n: 1 }; o.n += 41; print(o.n); var b = [1, 2]; b[1] *= 10; print(b[1]);\n"
                + "var y = 1; var z = (y += 2); print(y + z);";
        assertEquals("15\n12\n24\n6\n2\nab1\n3\n42\n20\n6\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testCompoundAssignmentEvaluatesTheTargetOnce() {
        String code = "var a = [0, 0]; var n = 0; function i() { n = n + 1; return 1; }\n"
                + "a[i()] += 5; print(a[1] + \" \" + n);\n"
                + "var calls = 0; function obj() { calls = calls + 1; return o; } var o = { v: 1 }; obj().v += 1; print(o.v + \" \" + calls);";
        assertEquals("5 1\n2 1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testCompoundAssignmentsAreStrict() {
        assertInterpreterThrows(TinyscriptTypeError.class, "var x = 1; x += true;");
        assertInterpreterThrows(TinyscriptTypeError.class, "var x = \"a\"; x -= 1;");
        assertInterpreterThrows(TinyscriptTypeError.class, "var x; x += 1;");
        assertRejected("var x += 1;");
        assertRejected("1 += 2;");
    }

    // =========================================================================
    // ++ and -- as statements
    // =========================================================================

    @Test
    public void testIncrementAndDecrementStatements() {
        String code = "var i = 0; i++; i++; i--; print(i);\n"
                + "var o = { count: 5 }; o.count++; print(o.count); var a = [1, 2]; a[0]--; print(a[0]);\n"
                + "var n = 0; while (n < 3) { n++; } print(n);\n"
                + "var counts = {}; for (var c : [\"x\", \"y\", \"x\"]) { if (counts[c] == null) { counts[c] = 0; } counts[c]++; } print(counts.x + \" \" + counts.y);";
        assertEquals("1\n6\n0\n3\n2 1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testIncrementNeedsANumber() {
        assertInterpreterThrows(TinyscriptTypeError.class, "var s = \"a\"; s++;");
        assertInterpreterThrows(TinyscriptTypeError.class, "var u; u++;");
    }

    @Test
    public void testIncrementOnlyAsAStatement() {
        assertRejected("var i = 0; var j = i++;");
        assertRejected("var i = 0; print(i++);");
        assertRejected("var i = 0; ++i;");
        assertRejected("1++;");
        assertRejected("var f = function() { return 1; }; f()++;");
    }
}
