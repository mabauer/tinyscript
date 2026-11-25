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
 * <p>Each {@code assertSameOutput(expected, tsCode)} call checks the interpreter
 * first ({@code expected:<x\n> but was:<y\n>} points to the buggy node in
 * {@code TinyscriptEngine}), then node, which must print the same. Without
 * node only the interpreter is checked; the tests are never skipped. Only the
 * sandbox (resource limits) is checked in the interpreter alone: the
 * generated code has none.
 */
public class InterpreterReferenceTest extends TinyscriptTranspilerTestBase {

    // =========================================================================
    // Expressions and operators
    // =========================================================================

    @Test
    public void testArithmeticExpressions() {
        assertSameOutput("5\n",   "print(2 + 3);");
        assertSameOutput("2.5\n", "print(10 / 4);");
        assertSameOutput("4\n",   "print(7 - 3);");
        assertSameOutput("12\n",  "print(3 * 4);");
        assertSameOutput("1\n",   "print(10 % 3);");
    }

    @Test
    public void testStringCoercion() {
        assertSameOutput("1hello\n", "print(1 + \"hello\");");
        assertSameOutput("hello1\n", "print(\"hello\" + 1);");
        assertSameOutput("hello world\n", "print(\"hello\" + \" world\");");
    }

    @Test
    public void testComparisonOperators() {
        assertSameOutput("true\n",  "print(5 > 3);");
        assertSameOutput("false\n", "print(5 < 3);");
        assertSameOutput("true\n",  "print(5 == 5);");
        assertSameOutput("false\n", "print(5 == 6);");
        assertSameOutput("true\n",  "print(5 != 6);");
        assertSameOutput("true\n",  "print(5 >= 5);");
        assertSameOutput("true\n",  "print(5 <= 5);");
    }

    @Test
    public void testLogicalOperators() {
        assertSameOutput("true\n",  "print(true && true);");
        assertSameOutput("false\n", "print(true && false);");
        assertSameOutput("true\n",  "print(false || true);");
        assertSameOutput("false\n", "print(false || false);");
        assertSameOutput("false\n", "print(!true);");
        assertSameOutput("true\n",  "print(!false);");
    }

    @Test
    public void testUnaryMinus() {
        assertSameOutput("-5\n", "print(-5);");
        assertSameOutput("-5\n", "let x = 5; print(-x);");
    }

    // =========================================================================
    // Control structures — if/else
    // =========================================================================

    @Test
    public void testIfElse() {
        assertSameOutput("big\n",   
                "if (5 > 3) { print(\"big\"); } else { print(\"small\"); }");
        assertSameOutput("small\n", 
                "if (1 > 3) { print(\"big\"); } else { print(\"small\"); }");
    }

    @Test
    public void testIfWithoutElse() {
        assertSameOutput("yes\n", "if (true) { print(\"yes\"); }");
        assertSameOutput("",      "if (false) { print(\"yes\"); }");
    }

    @Test
    public void testElseIf() {
        assertSameOutput("negative\nzero\npositive\n", 
                "function sign(n) {\n"
                + "  if (n < 0) { return \"negative\"; }\n"
                + "  else if (n == 0) { return \"zero\"; }\n"
                + "  else { return \"positive\"; }\n"
                + "}\n"
                + "print(sign(-1)); print(sign(0)); print(sign(1));");
    }

    @Test
    public void testElseIfWithoutFinalElse() {
        assertSameOutput("b\n", 
                "let x = 2;\n"
                + "if (x == 1) { print(\"a\"); } else if (x == 2) { print(\"b\"); } else if (x == 3) { print(\"c\"); }");
        assertSameOutput("", 
                "let x = 4;\n"
                + "if (x == 1) { print(\"a\"); } else if (x == 2) { print(\"b\"); }");
    }

    @Test
    public void testElseIfEvaluatesConditionsOnlyUntilOneMatches() {
        assertSameOutput("check 1\ncheck 2\nb\n", 
                "function check(n, result) { print(\"check \" + n); return result; }\n"
                + "if (check(1, false)) { print(\"a\"); }\n"
                + "else if (check(2, true)) { print(\"b\"); }\n"
                + "else if (check(3, true)) { print(\"c\"); }");
    }

    @Test
    public void testElseIfBranchesHaveTheirOwnScope() {
        assertSameOutput("2\n1\n", 
                "let x = 1;\n"
                + "if (false) { let y = 1; } else if (true) { let y = 2; let x = 2; print(x); }\n"
                + "print(x);");
    }

    @Test
    public void testBreakInsideElseIf() {
        assertSameOutput("1\ntwo\n", 
                "for (const i : range(1, 6)) {\n"
                + "  if (i == 2) { print(\"two\"); } else if (i == 3) { break; } else { print(i); }\n"
                + "}");
    }

    // =========================================================================
    // Control structures — numeric for loop
    // =========================================================================

    @Test
    public void testNumericForLoop() {
        assertSameOutput("1\n2\n3\n", 
                "for (const i : range(1, 4)) { print(i); }");
    }

    @Test
    public void testNumericForLoopSum() {
        assertSameOutput("6\n", 
                "let sum = 0;\n"
                + "for (const i : range(1, 4)) { sum = sum + i; }\n"
                + "print(sum);");
    }

    // =========================================================================
    // Control structures — for-in (iterable)
    // =========================================================================

    @Test
    public void testForInArray() {
        assertSameOutput("a\nb\nc\n", 
                "let arr = [\"a\", \"b\", \"c\"];\n"
                + "for (let x : arr) { print(x); }");
    }

    @Test
    public void testForInArrayNumbers() {
        assertSameOutput("1\n2\n3\n", 
                "let arr = [1, 2, 3];\n"
                + "for (let x : arr) { print(x); }");
    }

    // =========================================================================
    // Functions
    // =========================================================================

    @Test
    public void testFunctionDeclarationAndCall() {
        assertSameOutput("6\n", 
                "function double(x) { return x * 2; }\n"
                + "print(double(3));");
    }

    @Test
    public void testRecursiveFactorial() {
        assertSameOutput("120\n", 
                "function f(n) { if (n <= 1) { return 1; } return n * f(n-1); }\n"
                + "print(f(5));");
    }

    @Test
    public void testRecursiveFibonacci() {
        assertSameOutput("8\n", 
                "function fibonacci(n) {\n"
                + "  if (n == 0 || n == 1) { return 1; }\n"
                + "  else { return fibonacci(n-2) + fibonacci(n-1); }\n"
                + "}\n"
                + "print(fibonacci(5));");
    }

    @Test
    public void testFunctionExpression() {
        assertSameOutput("9\n", 
                "let square = function(x) { return x * x; };\n"
                + "print(square(3));");
    }

    // =========================================================================
    // Arrow functions
    // =========================================================================

    @Test
    public void testArrowFunctionSingleParam() {
        assertSameOutput("6\n", "let f = x => x * 2; print(f(3));");
    }

    @Test
    public void testArrowFunctionParenthesisedParam() {
        assertSameOutput("6\n", "let f = (x) => x * 2; print(f(3));");
    }

    @Test
    public void testArrowFunctionMultipleParams() {
        assertSameOutput("7\n", 
                "let add = (a, b) => a + b; print(add(3, 4));");
    }

    // =========================================================================
    // Closures and lexical scope
    // =========================================================================

    @Test
    public void testClosure() {
        assertSameOutput("5\n", 
                "function makeAdder(x) { return function(y) { return x + y; }; }\n"
                + "print(makeAdder(3)(2));");
    }

    @Test
    public void testClosureCounter() {
        assertSameOutput("3\n", 
                "let makeCounter = function() {\n"
                + "  let value = 0;\n"
                + "  return {\n"
                + "    inc: function() { value = value + 1; },\n"
                + "    val: function() { return value; }\n"
                + "  };\n"
                + "};\n"
                + "let c = makeCounter();\n"
                + "c.inc(); c.inc(); c.inc();\n"
                + "print(c.val());");
    }

    // =========================================================================
    // Block scoping
    // =========================================================================

    @Test
    public void testBlockScoping() {
        assertSameOutput("10\n5\n", 
                "let x = 5;\n"
                + "{ let x = 10; print(x); }\n"
                + "print(x);");
    }

    // =========================================================================
    // Objects and prototypal inheritance
    // =========================================================================

    @Test
    public void testObjectLiteral() {
        assertSameOutput("42\n", 
                "let obj = { x: 42 };\n"
                + "print(obj.x);");
    }

    @Test
    public void testPrototypalInheritance() {
        assertSameOutput("42\n", 
                "let proto = { getX: function() { return this.x; } };\n"
                + "let obj = Object.create(proto);\n"
                + "obj.x = 42;\n"
                + "print(obj.getX());");
    }

    @Test
    public void testObjectPropertyAssignment() {
        assertSameOutput("hello\n", 
                "let obj = {};\n"
                + "obj.name = \"hello\";\n"
                + "print(obj.name);");
    }

    // =========================================================================
    // Arrays
    // =========================================================================

    @Test
    public void testArrayLiteral() {
        assertSameOutput("1\n", 
                "let arr = [1, 2, 3];\n"
                + "print(arr[0]);");
    }

    @Test
    public void testArrayLength() {
        assertSameOutput("3\n", 
                "let arr = [1, 2, 3];\n"
                + "print(arr.length);");
    }

    @Test
    public void testArrayPushPop() {
        assertSameOutput("4\n3\n", 
                "let arr = [1, 2, 3];\n"
                + "arr.push(4);\n"
                + "print(arr.pop());\n"
                + "print(arr.length);");
    }

    @Test
    public void testArrayAdditionConcatenates() {
        assertSameOutput("1,2,3\n3\n1\n", 
                "let a = [1];\n"
                + "let b = a + [2, 3];\n"
                + "print(b.join(\",\"));\n"
                + "print(b.length);\n"
                + "print(a.length);");
    }

    @Test
    public void testArrayAdditionResultIsAnArray() {
        assertSameOutput("true\ntrue\n", 
                "let a = [1] + [2];\n"
                + "print(Array.isArray(a));\n"
                + "print(Object.getPrototypeOf(a) == Object.getPrototypeOf([]));");
    }

    @Test
    public void testAddingStringAndArrayConcatenatesStrings() {
        assertSameOutput("a: [1, 2]\n[1]x\n", 
                "print(\"a: \" + [1, 2]);\n"
                + "print([1] + \"x\");");
    }

    @Test
    public void testAddingArrayAndOtherNonArrayIsATypeError() {
        // With literals on both sides, an error before the run (LiteralChecksTest)
        assertSameFailure(TinyscriptTypeError.class, "const a = [1];\nprint(a + 2);");
        assertSameFailure(TinyscriptTypeError.class, "const a = [1];\nprint(0 + a);");
        assertSameFailure(TinyscriptTypeError.class, "const a = [1];\nprint(a + {});");
        assertSameFailure(TinyscriptTypeError.class, "const a = [1];\nprint(null + a);");
    }

    // =========================================================================
    // Error / exception cases
    // =========================================================================

    @Test
    public void testAssertFalseThrows() {
        assertSameFailure(TinyscriptAssertationError.class, "assert(false);");
    }

    @Test
    public void testAssertTrueDoesNotThrow() {
        // Should complete without error
        assertSameOutput("", "assert(true);");
    }

    // =========================================================================
    // Scoping: return from nested blocks, closures over function declarations
    // =========================================================================

    @Test
    public void testReturnFromNestedBlockRestoresCallerContext() {
        assertSameOutput("47\n", 
                "function f(x) { if (x > 0) { let y = x; return y; } return 0; }\n"
                + "function g() { let local = 42; let r = f(5); return local + r; }\n"
                + "print(g());");
    }

    @Test
    public void testReturnFromLoopRestoresCallerContext() {
        assertSameOutput("13\n", 
                "function find(arr) { for (let x : arr) { if (x > 2) { return x; } } return -1; }\n"
                + "function g() { let local = 10; let r = find([1, 2, 3]); return local + r; }\n"
                + "print(g());");
    }

    @Test
    public void testClosureOverFunctionDeclaration() {
        assertSameOutput("1\n2\n", 
                "function makeGetter(v) { function get() { return v; } return get; }\n"
                + "let a = makeGetter(1);\n"
                + "let b = makeGetter(2);\n"
                + "print(a());\n"
                + "print(b());");
    }

    // =========================================================================
    // break
    // =========================================================================

    @Test
    public void testBreakInNumericFor() {
        assertSameOutput("1\n2\n", 
                "for (const i : range(1, 11)) { if (i == 3) { break; } print(i); }");
    }

    @Test
    public void testBreakInIterableFor() {
        assertSameOutput("a\nb\n", 
                "for (let s : [\"a\", \"b\", \"stop\", \"c\"]) { if (s == \"stop\") { break; } print(s); }");
    }

    @Test
    public void testBreakLeavesOnlyInnermostLoop() {
        assertSameOutput("1-1\n2-1\n", 
                "for (const i : range(1, 3)) { for (const j : range(1, 4)) { if (j == 2) { break; } print(i + \"-\" + j); } }");
    }

    @Test
    public void testBreakInsideFunctionInLoop() {
        assertSameOutput("1\n2\n", 
                "function firstTwo() { let n = 0; for (const i : range(1, 11)) { n = n + 1; if (n > 2) { break; } print(i); } }\n"
                + "firstTwo();");
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
        assertSameOutput("0\n1\n2\n", 
                "let i = 0;\n"
                + "while (i < 3) { print(i); i = i + 1; }");
    }

    @Test
    public void testWhileWithFalseConditionDoesNotRunBody() {
        assertSameOutput("done\n", 
                "while (false) { print(\"never\"); }\n"
                + "print(\"done\");");
    }

    @Test
    public void testWhileBodyHasItsOwnBlockScope() {
        assertSameOutput("0\n10\n", 
                "let i = 0;\n"
                + "while (i < 2) { let x = i * 10; print(x); i = i + 1; }");
    }

    @Test
    public void testBreakInWhile() {
        assertSameOutput("1\n2\n", 
                "let i = 0;\n"
                + "while (true) { i = i + 1; if (i > 2) { break; } print(i); }");
    }

    @Test
    public void testBreakInWhileLeavesOnlyInnermostLoop() {
        assertSameOutput("1-0\n2-0\n", 
                "for (const i : range(1, 3)) { let j = 0; while (true) { if (j == 1) { break; } print(i + \"-\" + j); j = j + 1; } }");
    }

    @Test
    public void testReturnFromWhileRestoresCallerContext() {
        assertSameOutput("15\n", 
                "function firstAbove(limit) { let n = 0; while (true) { let next = n + 1; if (next > limit) { return next; } n = next; } }\n"
                + "function g() { let local = 10; let r = firstAbove(4); return local + r; }\n"
                + "print(g());");
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
        assertSameOutput("true\nfalse\ntrue\ntrue\n", 
                "print(\"a\" < \"b\");\n"
                + "print(\"b\" <= \"a\");\n"
                + "print(\"b\" > \"a\");\n"
                + "print(\"a\" >= \"a\");");
    }

    @Test
    public void testComparingNumberWithStringIsATypeError() {
        assertSameFailure(TinyscriptTypeError.class, "const a = \"a\";\nprint(1 < a);");
    }

    // =========================================================================
    // Numeric for: step validation
    // =========================================================================



    // =========================================================================
    // Sandbox: every loop iteration counts against the statement limit
    // (interpreter only, as all resource limits)
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
        assertSameFailure("let a = [1];\n\na.push();");
        TinyscriptRuntimeException e = assertThrows(TinyscriptRuntimeException.class,
                () -> interpreterOutput("let a = [1];\n\na.push();"));
        assertEquals(3, e.getAffectedLine());
    }
}
