package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptError;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * throw ends the script with an error message; there is no try / catch.
 */
public class ThrowTest extends TinyscriptTranspilerTestBase {

    private TinyscriptRuntimeException thrown(String code) {
        return assertThrows(TinyscriptError.class, () -> interpreterOutput(code));
    }

    @Test
    public void testThrowAnError() {
        TinyscriptRuntimeException e = thrown("function parse(line) {\n  throw new Error(\"unexpected input: \" + line);\n}\nparse(\"x\");");
        assertEquals("unexpected input: x", e.getMessage());
        assertEquals(2, e.getAffectedLine());
        assertTrue(e.getTinyscriptStacktraceAsString().contains("parse"), e.getTinyscriptStacktraceAsString());
    }

    @Test
    public void testThrowOtherValues() {
        assertEquals("oops", thrown("throw \"oops\";").getMessage());
        assertEquals("42", thrown("throw 42;").getMessage());
        assertEquals("", thrown("throw new Error();").getMessage());
    }

    @Test
    public void testOutputBeforeTheThrowIsKept() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThrows(TinyscriptError.class, () -> runInterpreter("print(\"before\"); throw new Error(\"stop\"); print(\"after\");", out));
        assertEquals("before\n", out.toString());
    }

    @Test
    public void testErrorObjects() {
        String code = "let e = new Error(\"bad\"); print(e.message, e.name, e.toString(), e instanceof Error);\n"
                + "print(new Error().message == \"\", Error(\"x\").message, \"\" + new Error(\"y\"));";
        assertEquals("bad Error Error: bad true\ntrue x Error: y\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testThrowEndsTheProgramInNodeAsWell() {
        assumeNodeAvailable();
        String jsCode = transpile(parse("print(\"before\"); throw new Error(\"stop\");"));
        AssertionError e = assertThrows(AssertionError.class, () -> NodeJsRunner.run(jsCode));
        assertTrue(e.getMessage().contains("Error: stop"), e.getMessage());
    }

    @Test
    public void testErrorMessageMustBeAString() {
        assertInterpreterThrows(TinyscriptTypeError.class, "new Error(42);");
    }
}
