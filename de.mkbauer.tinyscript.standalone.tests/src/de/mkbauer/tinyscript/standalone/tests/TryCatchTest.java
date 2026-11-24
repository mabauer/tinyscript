package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.ResourceLimits;
import de.mkbauer.tinyscript.interpreter.TinyscriptError;
import de.mkbauer.tinyscript.interpreter.TinyscriptResourceLimitViolation;
import de.mkbauer.tinyscript.interpreter.TinyscriptStackOverflowError;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * try { ... } catch (e) { ... }: catches the errors thrown with throw (and failed input / output,
 * see StdFsTest); programming errors, failed asserts and resource limits end the script.
 */
public class TryCatchTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testCatchAThrownError() {
        String code = "function parse(s) {\n  if (s == \"\") { throw new Error(\"empty\"); }\n  return s;\n}\n"
                + "try {\n  print(parse(\"a\"));\n  print(parse(\"\"));\n  print(\"not reached\");\n}\n"
                + "catch (e) {\n  print(\"caught\", e.message, e.name, e instanceof Error, e);\n}\n"
                + "print(\"after\");";
        assertEquals("a\ncaught empty Error true Error: empty\nafter\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNestedCatchWithoutErrorAndRethrow() {
        String code = "try {\n  try { throw new Error(\"inner\"); }\n  catch (e) { print(\"inner\", e.message); throw e; }\n}\n"
                + "catch (e2) { print(\"outer\", e2.message); }\n"
                + "try { throw Error(\"ignored\"); } catch { print(\"no error variable\"); }\n"
                + "try { print(\"no error\"); } catch (e) { print(\"not reached\"); }";
        assertEquals("inner inner\nouter inner\nno error variable\nno error\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testReturnBreakAndContinueLeaveTheTryBlock() {
        String code = "function first(items) {\n"
                + "  for (const x : items) {\n"
                + "    try {\n      if (x > 2) { return x; }\n      if (x == 1) { continue; }\n      throw new Error(\"x=\" + x);\n    }\n"
                + "    catch (e) { print(e.message); }\n"
                + "  }\n  return null;\n}\n"
                + "print(first([1, 2, 3, 4]), first([]));\n"
                + "let count = 0;\nwhile (true) {\n  try { count++; if (count == 3) { break; } } catch (e) { print(\"not reached\"); }\n}\n"
                + "print(count);";
        assertEquals("x=2\n3 null\n3\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testTheErrorVariableIsLocalToTheCatchBlock() {
        String code = "const e = \"outer\";\nconst fs = [];\n"
                + "for (const i : range(2)) {\n  try { throw new Error(\"error \" + i); }\n  catch (e) { fs.push(() => e.message); }\n}\n"
                + "print(e, fs.map(f => f()));\n"
                + "function f() { try { throw new Error(\"x\"); } catch (err) { let y = 1; err = err.message + y; return err; } }\n"
                + "print(f());";
        // Each catch gets a variable of its own; it can be assigned (as in Javascript)
        assertEquals("outer [\"error 0\", \"error 1\"]\nx1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testCaughtErrorsCanBeChangedAndThrownAgain() {
        String code = "function check(n) { if (n < 0) { throw new Error(\"negative: \" + n); } return n; }\n"
                + "const errors = [];\n"
                + "for (const n : [1, -2, 3, -4]) {\n  try { check(n); } catch (e) { errors.push(e); }\n}\n"
                + "print(errors, errors.map(e => e.message));\n"
                + "try {\n  try { check(-1); } catch (e) { e.message = \"in input: \" + e.message; throw e; }\n}\n"
                + "catch (e) { print(e.message); }";
        assertEquals("[Error: negative: -2, Error: negative: -4] [\"negative: -2\", \"negative: -4\"]\n"
                + "in input: negative: -1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testThrowingACaughtErrorAgainKeepsItsPlace() {
        TinyscriptRuntimeException e = assertThrows(TinyscriptError.class, () -> interpreterOutput(
                "function load() {\n  throw new Error(\"boom\");\n}\ntry {\n  load();\n}\ncatch (e) {\n  throw e;\n}"));
        assertEquals("boom", e.getMessage());
        assertEquals(2, e.getAffectedLine());
        assertTrue(e.getTinyscriptStacktraceAsString().contains("load"), e.getTinyscriptStacktraceAsString());
        // A changed error is a new one, thrown where it is thrown again
        e = assertThrows(TinyscriptError.class, () -> interpreterOutput(
                "function load() {\n  throw new Error(\"boom\");\n}\ntry {\n  load();\n}\ncatch (e) {\n  e.message = \"more\";\n  throw e;\n}"));
        assertEquals("more", e.getMessage());
        assertEquals(9, e.getAffectedLine());
    }

    @Test
    public void testOnlyErrorObjectsCanBeThrown() {
        assertSameFailure(TinyscriptTypeError.class, "throw \"oops\";");
        assertSameFailure(TinyscriptTypeError.class, "try { throw 42; } catch (e) { print(e); }");
        assertSameFailure(TinyscriptTypeError.class, "throw { message: \"x\" };");
        assertSameFailure(TinyscriptTypeError.class, "throw null;");
    }

    @Test
    public void testProgrammingErrorsAreNotCaught() {
        assertSameFailure(TinyscriptTypeError.class, "const o = null;\ntry { print(o.x); } catch (e) { print(\"caught\"); }");
        assertSameFailure(TinyscriptTypeError.class, "try { [1].splice(\"a\"); } catch (e) { print(\"caught\"); }");
        assertSameFailure(TinyscriptTypeError.class, "try { print(1 + \"a\" < 2); } catch { print(\"caught\"); }");
        assertSameFailure(TinyscriptAssertationError.class, "try { assert 1 == 2; } catch (e) { print(\"caught\"); }");
    }

    @Test
    public void testProgrammingErrorsAreNotCaughtInNodeEither() {
        assumeNodeAvailable();
        for (String code : new String[] {
                "const o = null;\ntry { print(o.x); } catch (e) { print(\"caught\"); }",
                "try { assert 1 == 2; } catch (e) { print(\"caught\"); }",
                "try { throw \"oops\"; } catch (e) { print(\"caught\"); }" }) {
            String jsCode = transpile(parse(code));
            AssertionError e = assertThrows(AssertionError.class, () -> NodeJsRunner.run(jsCode), code);
            assertTrue(!e.getMessage().contains("caught\n"), e.getMessage());
        }
    }

    @Test
    @Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testStackOverflowIsNotCaught() {
        assertSameFailure(TinyscriptStackOverflowError.class,
                "function f(n) { return f(n + 1); }\ntry { f(0); } catch (e) { print(\"caught\"); }");
    }

    @Test
    public void testResourceLimitsAreNotCaught() {
        ResourceLimits limits = new ResourceLimits();
        limits.setMaxStatements(1000);
        assertThrows(TinyscriptResourceLimitViolation.class,
                () -> interpreterOutput("while (true) { try { let x = 1; } catch (e) { print(\"caught\"); } }", limits));
        assertThrows(TinyscriptResourceLimitViolation.class,
                () -> interpreterOutput("try { while (true) { } } catch (e) { print(\"caught\"); }", limits));
    }
}
