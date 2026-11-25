package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Checks before the run of calls and structure (TODO.md, second backend, static checks level 1):
 * errors for what fails whenever it runs, warnings for what runs but is almost always a mistake.
 */
public class StructureChecksTest extends TinyscriptTranspilerTestBase {

    private String onlyError(String code) {
        List<String> errors = reportedErrors(code);
        assertEquals(1, errors.size(), errors.toString());
        return errors.get(0).replaceFirst("^\\d+:\\d+: ", "");
    }

    private String onlyWarning(String code) {
        List<String> warnings = reportedWarnings(code);
        assertEquals(1, warnings.size(), warnings.toString());
        return warnings.get(0).replaceFirst("^\\d+:\\d+: ", "").replaceFirst(" \\(line \\d+\\)$", "");
    }

    @Test
    public void testConstructorCalls() {
        assertEquals("Point is a class: use new Point(…)", onlyError("class Point { }\nconst p = Point();"));
        assertEquals("'new' needs a class or a constructor function, not an arrow function", onlyError("const f = x => x;\nconst o = new f(1);"));
        assertEquals("'new' needs a class or a constructor function, not the builtin function sqrt", onlyError("const o = new Math.sqrt(4);"));
        assertEquals("'new' needs a class or a constructor function, not the builtin function print", onlyError("const o = new print(4);"));
        assertEquals("'new' needs a class or a constructor function, not the builtin function Number", onlyError("const o = new Number(5);"));
    }

    @Test
    public void testThrowTakesAnError() {
        assertEquals("throw needs an Error object, e.g. throw new Error(\"...\"), not a string", onlyError("throw \"oops\";"));
        assertEquals("throw needs an Error object, e.g. throw new Error(\"...\"), not a number", onlyError("throw 42;"));
        assertEquals("throw needs an Error object, e.g. throw new Error(\"...\"), not an object", onlyError("throw { message: \"x\" };"));
        assertEquals("throw needs an Error object, e.g. throw new Error(\"...\"), not null", onlyError("throw null;"));
    }

    @Test
    public void testArrayPatterns() {
        assertEquals("[a, b, c] needs an array with at least 3 elements, not 2", onlyError("let [a, b, c] = [1, 2];"));
        assertEquals("[a, b, ...rest] needs an array with at least 2 elements, not 1", onlyError("const [a, b, ...rest] = [1];"));
    }

    @Test
    public void testWarnings() {
        assertEquals("Duplicate key 'a': only the last one counts", onlyWarning("const o = { a: 1, b: 2, a: 3 };"));
        assertEquals("Duplicate key 'x': only the last one counts", onlyWarning("const x = 1;\nconst o = { x, \"x\": 2 };"));
        assertEquals("This code never runs: it comes after return", onlyWarning("function f() {\n  return 1;\n  print(2);\n}\nprint(f());"));
        assertEquals("This code never runs: it comes after break", onlyWarning("while (true) {\n  break;\n  print(1);\n}"));
        assertEquals("This code never runs: it comes after throw", onlyWarning("function f() {\n  throw new Error(\"x\");\n  print(1);\n}"));
    }

    @Test
    public void testWhatStaysAllowed() {
        // new of classes, functions, builtin constructors; throw of Errors and of unknown values;
        // patterns of long enough or unknown arrays; a function declared after return (hoisted)
        String code = "class Point { constructor(x) { this.x = x; } }\nfunction Old(y) { this.y = y; }\n"
                + "print(new Point(1), new Old(2), new Map(), new Error(\"e\").message);\n"
                + "const [a, b] = [1, 2, 3];\nconst xs = [4];\nconst [c, ...rest] = xs;\nprint(a, b, c, rest);\n"
                + "function f() {\n  return g();\n  function g() { return 5; }\n}\nprint(f());\n"
                + "try { throw new Error(\"x\"); } catch (e) { print(e.message); }";
        assertEquals(List.of(), reportedErrors(code));
        assertEquals(List.of(), reportedWarnings(code));
        assertSameOutput("Point {x: 1} {y: 2} Map {} e\n1 2 4 []\n5\nx\n", code);
    }
}
