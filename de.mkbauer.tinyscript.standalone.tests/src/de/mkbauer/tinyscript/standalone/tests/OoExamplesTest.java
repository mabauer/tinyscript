package de.mkbauer.tinyscript.standalone.tests;

import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * The examples in examples/oo, in the interpreter and in node: the same program written
 * with class / interface (planned) and with what tinyscript has today.
 */
public class OoExamplesTest extends TinyscriptTranspilerTestBase {

    private static final Path DIRECTORY = Path.of("../examples/oo").toAbsolutePath().normalize();

    private static final String EXPRESSIONS_OUTPUT = """
            f(x)   = 3 * x^2 + 2 * x - 5
            f'(x)  = 0 * x^2 + 3 * (2 * x^1 * 1) + (0 * x + 2 * 1) - 0
                   = 6 * x + 2
            f(-2) = 3   f'(-2) = -10
            f(-1) = -4   f'(-1) = -4
            f(0) = -5   f'(0) = 2
            f(1) = 0   f'(1) = 8
            f(2) = 11   f'(2) = 14
            h(x, y) = x * y + y^3
            dh/dx   = y
            dh/dy   = x + 3 * y^2
            Newton for x^2 - 2 with g'(x) = 2 * x
              step 1 1.5
              step 2 1.4166666666666667
              step 3 1.4142156862745099
              step 4 1.4142135623746899
              step 5 1.4142135623730951
            sqrt(2) = 1.4142135623730951  difference: 0
            Cannot evaluate x * y + y^3: no value for y
            """;

    @Test
    public void testExpressionsToday() {
        assertProgramOutput(EXPRESSIONS_OUTPUT, DIRECTORY.resolve("expressions_today.tys"));
    }

    @Test
    @Disabled("class and interface are not implemented yet (TODO.md: Objects with methods are verbose)")
    public void testExpressions() {
        assertProgramOutput(EXPRESSIONS_OUTPUT, DIRECTORY.resolve("expressions.tys"));
    }
}
