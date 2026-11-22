package de.mkbauer.tinyscript.standalone.tests;

import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * The examples in examples/oo, in the interpreter and in node: classes before class
 * (car, prototypes_as_classes), and programs written with class / interface (planned) and
 * with what tinyscript has today.
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

    private static final String ORDERS_OUTPUT = """
            Line 4: unknown product gizmo
            Line 8: cannot read "refund 1001"

            Audit log:
               1  order 1001 placed by alice: 2 widget, 1 gadget for 34.99
               2  order 1002 placed by bob: 5 gadget for 49.95
               3  order 1002 cancelled: not enough gadget
               4  payment of 34.99 for order 1001
               5  order 1001 paid
               6  order 1001 shipped to alice
               7  order 1004 placed by carol: 1 widget for 12.50
               8  payment of 10.00 for order 1004
               9  payment of 49.95 for order 1002

            Stock:
              widget     0
              gadget     3

            Open:
              order 1004      2.50
            To refund:
              order 1002     49.95
            """;

    @Test
    public void testCar() {
        assertProgramOutput("Engine is: V8 true\nEngine is: V8 true\n{engine: \"V8\"} Car {engine: \"V8\"}\n",
                DIRECTORY.resolve("car.tys"));
    }

    @Test
    public void testPrototypesAsClasses() {
        assertProgramOutput("""
                Person called Joe
                Person called Jane (CTO)
                true true ["name", "title"]
                Person called Joe
                Person called Jane (CTO)
                EmployeeClass {person: PersonClass {name: "Jane"}, title: "CTO"}
                """, DIRECTORY.resolve("prototypes_as_classes.tys"));
    }

    @Test
    public void testExpressionsToday() {
        assertProgramOutput(EXPRESSIONS_OUTPUT, DIRECTORY.resolve("expressions_today.tys"));
    }

    @Test
    @Disabled("class and interface are not implemented yet (TODO.md: Objects with methods are verbose)")
    public void testExpressions() {
        assertProgramOutput(EXPRESSIONS_OUTPUT, DIRECTORY.resolve("expressions.tys"));
    }

    @Test
    public void testOrdersToday() {
        assertProgramOutput(ORDERS_OUTPUT, DIRECTORY.resolve("orders_today.tys"));
    }

    @Test
    @Disabled("class and interface are not implemented yet (TODO.md: Objects with methods are verbose)")
    public void testOrders() {
        assertProgramOutput(ORDERS_OUTPUT, DIRECTORY.resolve("orders.tys"));
    }
}
