package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptInterruption;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * TinyscriptEngine.interrupt() stops a running script from another thread, as Ctrl-C
 * does in the REPL.
 */
public class InterruptTest extends TinyscriptTranspilerTestBase {

    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @AfterEach
    public void stopTimer() {
        timer.shutdownNow();
    }

    private TinyscriptEngine engine() {
        TinyscriptEngine engine = new TinyscriptEngine(resourceSet());
        engine.setSandboxed(false);
        engine.defineStdOut(new PrintStream(out, true));
        return engine;
    }

    // Runs the script and interrupts it after a moment; it must end within a few seconds
    private void assertInterrupted(String tsCode) {
        TinyscriptEngine engine = engine();
        var script = parse(tsCode);
        timer.schedule(engine::interrupt, 200, TimeUnit.MILLISECONDS);
        TinyscriptInterruption e = assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> assertThrows(TinyscriptInterruption.class, () -> engine.execute(script)));
        assertEquals("Interrupted", e.getMessage());
    }

    @Test
    public void testAnEndlessLoopIsInterrupted() {
        assertInterrupted("let n = 0;\nwhile (true) { n++; }");
    }

    @Test
    public void testAnEmptyEndlessLoopIsInterrupted() {
        assertInterrupted("while (true) { }");
    }

    @Test
    public void testCallsWithoutStatementsAreInterrupted() {
        // Arrow functions with an expression body run no statements, only calls
        assertInterrupted("const spin = n => n == 0 ? 0 : spin(n - 1) + spin(n - 1);\nspin(60);");
    }

    @Test
    public void testTryCatchDoesNotCatchAnInterruption() {
        assertInterrupted("while (true) {\n    try {\n        while (true) { }\n    }\n    catch (e) {\n        print(\"caught\");\n    }\n}");
        assertEquals("", out.toString());
    }

    @Test
    public void testAnInterruptWithoutARunningScriptDoesNotStopTheNextOne() {
        TinyscriptEngine engine = engine();
        engine.interrupt();
        engine.execute(parse("let n = 0;\nwhile (n < 1000) { n++; }\nprint(n);"));
        assertEquals("1000\n", out.toString());
    }
}
