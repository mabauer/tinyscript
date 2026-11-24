package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * The helpers for failures: assertSameFailure (both backends fail at the same point and in the
 * same category for catch) and assertKnownGap (node goes on where the interpreter fails).
 */
public class SameFailureTest extends TinyscriptTranspilerTestBase {

    private Failure nodeFailureOf(String tsCode) {
        return nodeFailure(NodeJsRunner.execute(withFailureHandler(transpile(parse(tsCode))), ""));
    }

    @Test
    public void testTheSameFailure() {
        assertSameFailure("print(1);\nconst f = x => x;\nconst o = new f();\nprint(2);");
        // Caught in both: throw, and not caught in both: a failed assert
        assertSameFailure("print(\"a\");\nthrow new Error(\"stop\");");
        assertSameFailure("assert 1 == 2;");
    }

    @Test
    public void testWhatTheInterpreterAndNodeReport() {
        assumeNodeAvailable();
        String code = "print(\"before\");\nthrow new Error(\"two\\nlines\\tand a \\\\\");";
        Failure interpreter = interpreterFailure(code);
        Failure node = nodeFailureOf(code);
        assertEquals(new Failure("before\n", "Error", "two\nlines\tand a \\", true), interpreter);
        assertEquals(interpreter, node);
        Failure overflow = nodeFailureOf("function f() { return f(); }\nf();");
        assertEquals("RangeError", overflow.name());
        assertEquals(false, overflow.catchable());
    }

    @Test
    public void testNodeGoesOn() {
        assumeNodeAvailable();
        AssertionFailedError e = assertThrows(AssertionFailedError.class, () -> assertSameFailure("print(\"a\" - 1);"));
        assertEquals(true, e.getMessage().startsWith("The interpreter fails, but node goes on"), e.getMessage());
        assertKnownGap("print(\"a\" - 1);");
    }

    @Test
    public void testAClosedGap() {
        assumeNodeAvailable();
        AssertionFailedError e = assertThrows(AssertionFailedError.class, () -> assertKnownGap("const o = new Math.sqrt(4);"));
        assertEquals(true, e.getMessage().startsWith("node fails now as well"), e.getMessage());
    }

    @Test
    public void testTheInterpreterMustFail() {
        AssertionFailedError e = assertThrows(AssertionFailedError.class, () -> assertSameFailure("print(1);"));
        assertEquals(true, e.getMessage().startsWith("The interpreter does not fail"), e.getMessage());
    }
}
