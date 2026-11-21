package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.ResourceLimits;
import de.mkbauer.tinyscript.interpreter.TinyscriptResourceLimitViolation;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Interpreter reference tests, organized by language feature.
 *
 * <p>Each {@code assertEquals(expected, interpreterOutput(tsCode))} call gives
 * {@code expected:<x\n> but was:<y\n>} on failure — the exact wrong output
 * points directly to the buggy interpreter node in {@code TinyscriptEngine}.
 *
 * <p>These tests never require node.js and are never skipped.
 */
public class InterpreterReferenceTest extends TinyscriptTranspilerTestBase {

    // =========================================================================
    // Expressions and operators
    // =========================================================================

    @Test
    public void testArithmeticExpressions() {
        assertEquals("5\n",   interpreterOutput("print(2 + 3);"));
        assertEquals("2.5\n", interpreterOutput("print(10 / 4);"));
        assertEquals("4\n",   interpreterOutput("print(7 - 3);"));
        assertEquals("12\n",  interpreterOutput("print(3 * 4);"));
        assertEquals("1\n",   interpreterOutput("print(10 % 3);"));
    }

    @Test
    public void testStringCoercion() {
        assertEquals("1hello\n", interpreterOutput("print(1 + \"hello\");"));
        assertEquals("hello1\n", interpreterOutput("print(\"hello\" + 1);"));
        assertEquals("hello world\n", interpreterOutput("print(\"hello\" + \" world\");"));
    }

    @Test
    public void testComparisonOperators() {
        assertEquals("true\n",  interpreterOutput("print(5 > 3);"));
        assertEquals("false\n", interpreterOutput("print(5 < 3);"));
        assertEquals("true\n",  interpreterOutput("print(5 == 5);"));
        assertEquals("false\n", interpreterOutput("print(5 == 6);"));
        assertEquals("true\n",  interpreterOutput("print(5 != 6);"));
        assertEquals("true\n",  interpreterOutput("print(5 >= 5);"));
        assertEquals("true\n",  interpreterOutput("print(5 <= 5);"));
    }

    @Test
    public void testLogicalOperators() {
        assertEquals("true\n",  interpreterOutput("print(true && true);"));
        assertEquals("false\n", interpreterOutput("print(true && false);"));
        assertEquals("true\n",  interpreterOutput("print(false || true);"));
        assertEquals("false\n", interpreterOutput("print(false || false);"));
        assertEquals("false\n", interpreterOutput("print(!true);"));
        assertEquals("true\n",  interpreterOutput("print(!false);"));
    }

    @Test
    public void testUnaryMinus() {
        assertEquals("-5\n", interpreterOutput("print(-5);"));
        assertEquals("-5\n", interpreterOutput("let x = 5; print(-x);"));
    }

    // =========================================================================
    // Control structures — if/else
    // =========================================================================

    @Test
    public void testIfElse() {
        assertEquals("big\n",   interpreterOutput(
                "if (5 > 3) { print(\"big\"); } else { print(\"small\"); }"));
        assertEquals("small\n", interpreterOutput(
                "if (1 > 3) { print(\"big\"); } else { print(\"small\"); }"));
    }

    @Test
    public void testIfWithoutElse() {
        assertEquals("yes\n", interpreterOutput("if (true) { print(\"yes\"); }"));
        assertEquals("",      interpreterOutput("if (false) { print(\"yes\"); }"));
    }

    @Test
    public void testElseIf() {
        assertEquals("negative\nzero\npositive\n", interpreterOutput(
                "function sign(n) {\n"
                + "  if (n < 0) { return \"negative\"; }\n"
                + "  else if (n == 0) { return \"zero\"; }\n"
                + "  else { return \"positive\"; }\n"
                + "}\n"
                + "print(sign(-1)); print(sign(0)); print(sign(1));"));
    }

    @Test
    public void testElseIfWithoutFinalElse() {
        assertEquals("b\n", interpreterOutput(
                "let x = 2;\n"
                + "if (x == 1) { print(\"a\"); } else if (x == 2) { print(\"b\"); } else if (x == 3) { print(\"c\"); }"));
        assertEquals("", interpreterOutput(
                "let x = 4;\n"
                + "if (x == 1) { print(\"a\"); } else if (x == 2) { print(\"b\"); }"));
    }

    @Test
    public void testElseIfEvaluatesConditionsOnlyUntilOneMatches() {
        assertEquals("check 1\ncheck 2\nb\n", interpreterOutput(
                "function check(n, result) { print(\"check \" + n); return result; }\n"
                + "if (check(1, false)) { print(\"a\"); }\n"
                + "else if (check(2, true)) { print(\"b\"); }\n"
                + "else if (check(3, true)) { print(\"c\"); }"));
    }

    @Test
    public void testElseIfBranchesHaveTheirOwnScope() {
        assertEquals("2\n1\n", interpreterOutput(
                "let x = 1;\n"
                + "if (false) { let y = 1; } else if (true) { let y = 2; let x = 2; print(x); }\n"
                + "print(x);"));
    }

    @Test
    public void testBreakInsideElseIf() {
        assertEquals("1\ntwo\n", interpreterOutput(
                "for (const i : range(1, 6)) {\n"
                + "  if (i == 2) { print(\"two\"); } else if (i == 3) { break; } else { print(i); }\n"
                + "}"));
    }

    // =========================================================================
    // Control structures — numeric for loop
    // =========================================================================

    @Test
    public void testNumericForLoop() {
        assertEquals("1\n2\n3\n", interpreterOutput(
                "for (const i : range(1, 4)) { print(i); }"));
    }

    @Test
    public void testNumericForLoopSum() {
        assertEquals("6\n", interpreterOutput(
                "let sum = 0;\n"
                + "for (const i : range(1, 4)) { sum = sum + i; }\n"
                + "print(sum);"));
    }

    // =========================================================================
    // Control structures — for-in (iterable)
    // =========================================================================

    @Test
    public void testForInArray() {
        assertEquals("a\nb\nc\n", interpreterOutput(
                "let arr = [\"a\", \"b\", \"c\"];\n"
                + "for (let x : arr) { print(x); }"));
    }

    @Test
    public void testForInArrayNumbers() {
        assertEquals("1\n2\n3\n", interpreterOutput(
                "let arr = [1, 2, 3];\n"
                + "for (let x : arr) { print(x); }"));
    }

    // =========================================================================
    // Functions
    // =========================================================================

    @Test
    public void testFunctionDeclarationAndCall() {
        assertEquals("6\n", interpreterOutput(
                "function double(x) { return x * 2; }\n"
                + "print(double(3));"));
    }

    @Test
    public void testRecursiveFactorial() {
        assertEquals("120\n", interpreterOutput(
                "function f(n) { if (n <= 1) { return 1; } return n * f(n-1); }\n"
                + "print(f(5));"));
    }

    @Test
    public void testRecursiveFibonacci() {
        assertEquals("8\n", interpreterOutput(
                "function fibonacci(n) {\n"
                + "  if (n == 0 || n == 1) { return 1; }\n"
                + "  else { return fibonacci(n-2) + fibonacci(n-1); }\n"
                + "}\n"
                + "print(fibonacci(5));"));
    }

    @Test
    public void testFunctionExpression() {
        assertEquals("9\n", interpreterOutput(
                "let square = function(x) { return x * x; };\n"
                + "print(square(3));"));
    }

    // =========================================================================
    // Arrow functions
    // =========================================================================

    @Test
    public void testArrowFunctionSingleParam() {
        assertEquals("6\n", interpreterOutput("let f = x => x * 2; print(f(3));"));
    }

    @Test
    public void testArrowFunctionParenthesisedParam() {
        assertEquals("6\n", interpreterOutput("let f = (x) => x * 2; print(f(3));"));
    }

    @Test
    public void testArrowFunctionMultipleParams() {
        assertEquals("7\n", interpreterOutput(
                "let add = (a, b) => a + b; print(add(3, 4));"));
    }

    // =========================================================================
    // Closures and lexical scope
    // =========================================================================

    @Test
    public void testClosure() {
        assertEquals("5\n", interpreterOutput(
                "function makeAdder(x) { return function(y) { return x + y; }; }\n"
                + "print(makeAdder(3)(2));"));
    }

    @Test
    public void testClosureCounter() {
        assertEquals("3\n", interpreterOutput(
                "let makeCounter = function() {\n"
                + "  let value = 0;\n"
                + "  return {\n"
                + "    inc: function() { value = value + 1; },\n"
                + "    val: function() { return value; }\n"
                + "  };\n"
                + "};\n"
                + "let c = makeCounter();\n"
                + "c.inc(); c.inc(); c.inc();\n"
                + "print(c.val());"));
    }

    // =========================================================================
    // Block scoping
    // =========================================================================

    @Test
    public void testBlockScoping() {
        assertEquals("10\n5\n", interpreterOutput(
                "let x = 5;\n"
                + "{ let x = 10; print(x); }\n"
                + "print(x);"));
    }

    // =========================================================================
    // Objects and prototypal inheritance
    // =========================================================================

    @Test
    public void testObjectLiteral() {
        assertEquals("42\n", interpreterOutput(
                "let obj = { x: 42 };\n"
                + "print(obj.x);"));
    }

    @Test
    public void testPrototypalInheritance() {
        assertEquals("42\n", interpreterOutput(
                "let proto = { getX: function() { return this.x; } };\n"
                + "let obj = Object.create(proto);\n"
                + "obj.x = 42;\n"
                + "print(obj.getX());"));
    }

    @Test
    public void testObjectPropertyAssignment() {
        assertEquals("hello\n", interpreterOutput(
                "let obj = {};\n"
                + "obj.name = \"hello\";\n"
                + "print(obj.name);"));
    }

    // =========================================================================
    // Arrays
    // =========================================================================

    @Test
    public void testArrayLiteral() {
        assertEquals("1\n", interpreterOutput(
                "let arr = [1, 2, 3];\n"
                + "print(arr[0]);"));
    }

    @Test
    public void testArrayLength() {
        assertEquals("3\n", interpreterOutput(
                "let arr = [1, 2, 3];\n"
                + "print(arr.length);"));
    }

    @Test
    public void testArrayPushPop() {
        assertEquals("4\n3\n", interpreterOutput(
                "let arr = [1, 2, 3];\n"
                + "arr.push(4);\n"
                + "print(arr.pop());\n"
                + "print(arr.length);"));
    }

    @Test
    public void testArrayAdditionConcatenates() {
        assertEquals("1,2,3\n3\n1\n", interpreterOutput(
                "let a = [1];\n"
                + "let b = a + [2, 3];\n"
                + "print(b.join(\",\"));\n"
                + "print(b.length);\n"
                + "print(a.length);"));
    }

    @Test
    public void testArrayAdditionResultIsAnArray() {
        assertEquals("true\ntrue\n", interpreterOutput(
                "let a = [1] + [2];\n"
                + "print(Array.isArray(a));\n"
                + "print(Object.getPrototypeOf(a) == Object.getPrototypeOf([]));"));
    }

    @Test
    public void testAddingStringAndArrayConcatenatesStrings() {
        assertEquals("a: [1, 2]\n[1]x\n", interpreterOutput(
                "print(\"a: \" + [1, 2]);\n"
                + "print([1] + \"x\");"));
    }

    @Test
    public void testAddingArrayAndOtherNonArrayIsATypeError() {
        assertInterpreterThrows(TinyscriptTypeError.class, "print([1] + 2);");
        assertInterpreterThrows(TinyscriptTypeError.class, "print(0 + [1]);");
        assertInterpreterThrows(TinyscriptTypeError.class, "print([1] + {});");
        assertInterpreterThrows(TinyscriptTypeError.class, "print(null + [1]);");
    }

    // =========================================================================
    // Error / exception cases
    // =========================================================================

    @Test
    public void testAssertFalseThrows() {
        assertInterpreterThrows(TinyscriptAssertationError.class, "assert(false);");
    }

    @Test
    public void testAssertTrueDoesNotThrow() {
        // Should complete without error
        interpreterOutput("assert(true);");
    }

    // =========================================================================
    // Scoping: return from nested blocks, closures over function declarations
    // =========================================================================

    @Test
    public void testReturnFromNestedBlockRestoresCallerContext() {
        assertEquals("47\n", interpreterOutput(
                "function f(x) { if (x > 0) { let y = x; return y; } return 0; }\n"
                + "function g() { let local = 42; let r = f(5); return local + r; }\n"
                + "print(g());"));
    }

    @Test
    public void testReturnFromLoopRestoresCallerContext() {
        assertEquals("13\n", interpreterOutput(
                "function find(arr) { for (let x : arr) { if (x > 2) { return x; } } return -1; }\n"
                + "function g() { let local = 10; let r = find([1, 2, 3]); return local + r; }\n"
                + "print(g());"));
    }

    @Test
    public void testClosureOverFunctionDeclaration() {
        assertEquals("1\n2\n", interpreterOutput(
                "function makeGetter(v) { function get() { return v; } return get; }\n"
                + "let a = makeGetter(1);\n"
                + "let b = makeGetter(2);\n"
                + "print(a());\n"
                + "print(b());"));
    }

    // =========================================================================
    // break
    // =========================================================================

    @Test
    public void testBreakInNumericFor() {
        assertEquals("1\n2\n", interpreterOutput(
                "for (const i : range(1, 11)) { if (i == 3) { break; } print(i); }"));
    }

    @Test
    public void testBreakInIterableFor() {
        assertEquals("a\nb\n", interpreterOutput(
                "for (let s : [\"a\", \"b\", \"stop\", \"c\"]) { if (s == \"stop\") { break; } print(s); }"));
    }

    @Test
    public void testBreakLeavesOnlyInnermostLoop() {
        assertEquals("1-1\n2-1\n", interpreterOutput(
                "for (const i : range(1, 3)) { for (const j : range(1, 4)) { if (j == 2) { break; } print(i + \"-\" + j); } }"));
    }

    @Test
    public void testBreakInsideFunctionInLoop() {
        assertEquals("1\n2\n", interpreterOutput(
                "function firstTwo() { let n = 0; for (const i : range(1, 11)) { n = n + 1; if (n > 2) { break; } print(i); } }\n"
                + "firstTwo();"));
    }

    @Test
    public void testBreakOutsideLoopIsRejected() {
        assertRejected("break;");
        assertRejected("if (true) { break; }");
    }

    @Test
    public void testBreakCannotLeaveFunctionBody() {
        assertRejected("for (const i : range(1, 4)) { let f = function() { break; }; f(); }");
    }

    // =========================================================================
    // while
    // =========================================================================

    @Test
    public void testWhileLoop() {
        assertEquals("0\n1\n2\n", interpreterOutput(
                "let i = 0;\n"
                + "while (i < 3) { print(i); i = i + 1; }"));
    }

    @Test
    public void testWhileWithFalseConditionDoesNotRunBody() {
        assertEquals("done\n", interpreterOutput(
                "while (false) { print(\"never\"); }\n"
                + "print(\"done\");"));
    }

    @Test
    public void testWhileBodyHasItsOwnBlockScope() {
        assertEquals("0\n10\n", interpreterOutput(
                "let i = 0;\n"
                + "while (i < 2) { let x = i * 10; print(x); i = i + 1; }"));
    }

    @Test
    public void testBreakInWhile() {
        assertEquals("1\n2\n", interpreterOutput(
                "let i = 0;\n"
                + "while (true) { i = i + 1; if (i > 2) { break; } print(i); }"));
    }

    @Test
    public void testBreakInWhileLeavesOnlyInnermostLoop() {
        assertEquals("1-0\n2-0\n", interpreterOutput(
                "for (const i : range(1, 3)) { let j = 0; while (true) { if (j == 1) { break; } print(i + \"-\" + j); j = j + 1; } }"));
    }

    @Test
    public void testReturnFromWhileRestoresCallerContext() {
        assertEquals("15\n", interpreterOutput(
                "function firstAbove(limit) { let n = 0; while (true) { let next = n + 1; if (next > limit) { return next; } n = next; } }\n"
                + "function g() { let local = 10; let r = firstAbove(4); return local + r; }\n"
                + "print(g());"));
    }

    @Test
    public void testDuplicateGlobalDeclarationsAreRejected() {
        assertRejected("let x = 1; let x = 2;");
        assertRejected("function f() { } function f() { }");
        assertRejected("let f = 1; function f() { }");
    }

    @Test
    public void testBreakCannotLeaveFunctionBodyInWhile() {
        assertRejected("while (true) { let f = function() { break; }; f(); }");
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testEmptyEndlessWhileHitsStatementLimit() {
        ResourceLimits limits = new ResourceLimits();
        limits.setMaxStatements(10000);
        assertThrows(TinyscriptResourceLimitViolation.class,
                () -> interpreterOutput("while (true) { }", limits));
    }

    // =========================================================================
    // Operators: no === / !==, comparisons
    // =========================================================================

    @Test
    public void testComparisonsCannotBeChained() {
        // In Javascript 1 < 3 < 2 is true; here chaining is a syntax error
        assertRejected("print(1 < 3 < 2);");
        assertRejected("print(1 == 1 == true);");
    }

    @Test
    public void testStrictEqualityOperatorsAreNotPartOfTheLanguage() {
        assertRejected("print(1 === 1);");
        assertRejected("print(1 !== 2);");
    }

    @Test
    public void testStringComparison() {
        assertEquals("true\nfalse\ntrue\ntrue\n", interpreterOutput(
                "print(\"a\" < \"b\");\n"
                + "print(\"b\" <= \"a\");\n"
                + "print(\"b\" > \"a\");\n"
                + "print(\"a\" >= \"a\");"));
    }

    @Test
    public void testComparingNumberWithStringIsATypeError() {
        assertInterpreterThrows(TinyscriptTypeError.class, "print(1 < \"a\");");
    }

    // =========================================================================
    // Numeric for: step validation
    // =========================================================================



    // =========================================================================
    // Sandbox: every loop iteration counts against the statement limit
    // =========================================================================

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testEmptyNumericLoopHitsStatementLimit() {
        ResourceLimits limits = new ResourceLimits();
        limits.setMaxStatements(10000);
        assertThrows(TinyscriptResourceLimitViolation.class,
                () -> interpreterOutput("for (const i : range(1000000000001)) { }", limits));
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testEmptyIterableLoopCountsIterations() {
        ResourceLimits limits = new ResourceLimits();
        limits.setMaxStatements(5);
        assertThrows(TinyscriptResourceLimitViolation.class,
                () -> interpreterOutput("let a = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];\nfor (let x : a) { }", limits));
    }

    @Test
    public void testOutputWithinLimitIsWritten() {
        ResourceLimits limits = new ResourceLimits();
        limits.setMaxOutputSize(100);
        assertEquals("a\nb\n", interpreterOutput("print(\"a\"); print(\"b\");", limits));
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testOutputLimitViolationReportsTheLine() {
        ResourceLimits limits = new ResourceLimits();
        limits.setMaxOutputSize(100);
        TinyscriptResourceLimitViolation e = assertThrows(TinyscriptResourceLimitViolation.class,
                () -> interpreterOutput("let i = 0;\nwhile (true) {\n  print(\"0123456789\");\n}", limits));
        assertEquals("Output limit reached", e.getMessage());
        assertEquals(3, e.getAffectedLine());
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testArrayGrowthIsCheckedBeforeAllocating() {
        ResourceLimits limits = new ResourceLimits();
        limits.setMaxObjectSize(1000);
        TinyscriptResourceLimitViolation e = assertThrows(TinyscriptResourceLimitViolation.class,
                () -> interpreterOutput("let a = [];\na[2000000000] = 1;", limits));
        assertEquals("Object size limit reached", e.getMessage());
        assertEquals(2, e.getAffectedLine());
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testGrowingArrayLengthIsCheckedBeforeAllocating() {
        ResourceLimits limits = new ResourceLimits();
        limits.setMaxObjectSize(1000);
        TinyscriptResourceLimitViolation e = assertThrows(TinyscriptResourceLimitViolation.class,
                () -> interpreterOutput("let a = [];\na.length = 2000000000;", limits));
        assertEquals("Object size limit reached", e.getMessage());
        assertEquals(2, e.getAffectedLine());
    }

    // =========================================================================
    // Errors report the line where they happened
    // =========================================================================

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testStatementLimitViolationReportsTheLine() {
        ResourceLimits limits = new ResourceLimits();
        limits.setMaxStatements(100);
        TinyscriptResourceLimitViolation e = assertThrows(TinyscriptResourceLimitViolation.class,
                () -> interpreterOutput("let i = 0;\n\nwhile (true) { }", limits));
        assertEquals(3, e.getAffectedLine());
    }

    @Test
    public void testCallDepthLimitViolationReportsTheLine() {
        ResourceLimits limits = new ResourceLimits();
        limits.setMaxRecursionDepth(10);
        TinyscriptResourceLimitViolation e = assertThrows(TinyscriptResourceLimitViolation.class,
                () -> interpreterOutput("function f(n) {\n  return f(n + 1);\n}\nf(0);", limits));
        assertEquals(2, e.getAffectedLine());
    }

    @Test
    public void testErrorInBuiltinReportsTheLineOfTheCall() {
        TinyscriptRuntimeException e = assertThrows(TinyscriptRuntimeException.class,
                () -> interpreterOutput("let a = [1];\n\na.push();"));
        assertEquals(3, e.getAffectedLine());
    }
}
