package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * continue, the conditional operator, compound assignments and ++ / -- statements.
 */
public class SyntaxTest extends TinyscriptTranspilerTestBase {

    // =========================================================================
    // continue
    // =========================================================================

    @Test
    public void testContinueInAllLoops() {
        String code = "let s = \"\";\n"
                + "for (const i : range(1, 7)) { if (i % 2 == 0) { continue; } s = s + i; }\n"
                + "for (let x : [\"a\", \"b\", \"c\"]) { if (x == \"b\") { continue; } s = s + x; }\n"
                + "let n = 0; while (n < 5) { n = n + 1; if (n == 3) { continue; } s = s + n; }\n"
                + "print(s);";
        assertEquals("135ac1245\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testContinueOnlyLeavesTheInnermostIteration() {
        String code = "let s = \"\";\n"
                + "for (const i : range(1, 4)) { for (const j : range(1, 4)) { if (j == 2) { continue; } s = s + i + j + \" \"; } s = s + \"| \"; }\n"
                + "print(s);";
        assertEquals("11 13 | 21 23 | 31 33 | \n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testContinueOnlyInsideALoop() {
        assertRejected("continue;");
        assertRejected("for (const i : range(1, 4)) { let f = function() { continue; }; }");
    }

    // =========================================================================
    // cond ? a : b
    // =========================================================================

    @Test
    public void testConditionalOperator() {
        String code = "function sign(x) { return x > 0 ? 1 : x < 0 ? -1 : 0; }\n"
                + "print(sign(5), sign(-2), sign(0));\n"
                + "let max = (a, b) => a > b ? a : b; print(max(3, 7));\n"
                + "let y = true ? \"yes\" : \"no\"; print(y); print([1, 2, 3].map(x => x % 2 == 0 ? \"e\" : \"o\").join(\"\"));\n"
                + "print(\"n=\" + (1 < 2 ? \"a\" : \"b\"));";
        assertEquals("1 -1 0\n7\nyes\noeo\nn=a\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testConditionalOperatorEvaluatesOnlyOneBranch() {
        String code = "let calls = \"\"; function f(s) { calls = calls + s; return s; }\n"
                + "let r = false ? f(\"a\") : f(\"b\"); print(r + \" \" + calls);";
        assertEquals("b b\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testConditionalOperatorNeedsACondition() {
        assertSameFailure(TinyscriptTypeError.class, "const one = 1;\nlet x = one ? 2 : 3;");
    }

    @Test
    public void testConditionalOperatorWithFunctionsAndAssignments() {
        String code = "let f = false ? x => x : x => x * 2; print(f(4));\n"
                + "let a; let b = true ? a = 1 : 2; print(a + b);";
        assertEquals("8\n2\n", interpreterOutput(code));
        assertSameOutput(code);
        // Javascript reads this as c ? a : (b = 1)
        assertRejected("let a; let b; let c = true; c ? a : b = 1;");
    }

    // =========================================================================
    // Compound assignments
    // =========================================================================

    @Test
    public void testCompoundAssignments() {
        String code = "let x = 10; x += 5; print(x); x -= 3; print(x); x *= 2; print(x); x /= 4; print(x); x %= 4; print(x);\n"
                + "let s = \"a\"; s += \"b\"; s += 1; print(s);\n"
                + "let a = [1]; a += [2, 3]; print(a.length);\n"
                + "let o = { n: 1 }; o.n += 41; print(o.n); let b = [1, 2]; b[1] *= 10; print(b[1]);\n"
                + "let y = 1; let z = (y += 2); print(y + z);";
        assertEquals("15\n12\n24\n6\n2\nab1\n3\n42\n20\n6\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testCompoundAssignmentEvaluatesTheTargetOnce() {
        String code = "let a = [0, 0]; let n = 0; function i() { n = n + 1; return 1; }\n"
                + "a[i()] += 5; print(a[1] + \" \" + n);\n"
                + "let calls = 0; function obj() { calls = calls + 1; return o; } let o = { v: 1 }; obj().v += 1; print(o.v + \" \" + calls);";
        assertEquals("5 1\n2 1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testCompoundAssignmentsAreStrict() {
        assertSameFailure(TinyscriptTypeError.class, "let x = 1; x += true;");
        assertSameFailure(TinyscriptTypeError.class, "let x = \"a\"; x -= 1;");
        assertSameFailure(TinyscriptTypeError.class, "let x; x += 1;");
        assertRejected("let x += 1;");
        assertRejected("1 += 2;");
    }

    // =========================================================================
    // ++ and -- as statements
    // =========================================================================

    @Test
    public void testIncrementAndDecrementStatements() {
        String code = "let i = 0; i++; i++; i--; print(i);\n"
                + "let o = { count: 5 }; o.count++; print(o.count); let a = [1, 2]; a[0]--; print(a[0]);\n"
                + "let n = 0; while (n < 3) { n++; } print(n);\n"
                + "let counts = {}; for (let c : [\"x\", \"y\", \"x\"]) { if (counts[c] == null) { counts[c] = 0; } counts[c]++; } print(counts.x + \" \" + counts.y);";
        assertEquals("1\n6\n0\n3\n2 1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testIncrementNeedsANumber() {
        assertSameFailure(TinyscriptTypeError.class, "let s = \"a\"; s++;");
        assertSameFailure(TinyscriptTypeError.class, "let u; u++;");
    }

    @Test
    public void testIncrementOnlyAsAStatement() {
        assertRejected("let i = 0; let j = i++;");
        assertRejected("let i = 0; print(i++);");
        assertRejected("let i = 0; ++i;");
        assertRejected("1++;");
        assertRejected("let f = function() { return 1; }; f()++;");
    }
}
