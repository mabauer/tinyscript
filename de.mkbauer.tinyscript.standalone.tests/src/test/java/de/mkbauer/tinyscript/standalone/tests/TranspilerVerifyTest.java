package de.mkbauer.tinyscript.standalone.tests;

import org.junit.Test;

/**
 * Level 2 integration tests: runs complete {@code .ts} programs through both
 * the interpreter (must not throw) and node.js (must exit 0).
 *
 * <p>Less targeted than {@link TranspilerTest} (Level 1), but catches
 * regressions in complex programs that combine multiple language features.
 * The generated JavaScript is shown on failure.
 *
 * <p>This class is named so that it sorts after {@link TranspilerTest}
 * alphabetically, ensuring the execution order:
 * {@link InterpreterReferenceTest} → {@link TranspilerTest} →
 * {@link TranspilerVerifyTest}.
 */
public class TranspilerVerifyTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testFibonacciProgram() {
        assertTranspilerCorrectFromFile("fibonacci.ts");
    }

    @Test
    public void testFactorialProgram() {
        assertTranspilerCorrectFromFile("factorial.ts");
    }
}
