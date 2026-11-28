package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Checks before the run, from the builtins of the runtime: unknown members of builtins and
 * methods of literals, and the number of arguments of a builtin (TODO.md, second backend,
 * static checks level 1). They fail before the run in both backends.
 */
public class BuiltinChecksTest extends TinyscriptTranspilerTestBase {

    private String onlyError(String code) {
        List<String> errors = reportedErrors(code);
        assertEquals(1, errors.size(), errors.toString());
        return errors.get(0).replaceFirst("^\\d+:\\d+: ", "");
    }

    @Test
    public void testUnknownMembersOfBuiltins() {
        assertEquals("Math has no method 'sqr' (did you mean 'sqrt'?)", onlyError("print(Math.sqr(4));"));
        assertEquals("Math has no member 'PIE' (did you mean 'PI'?)", onlyError("print(Math.PIE);"));
        assertEquals("JSON has no method 'stringify'", onlyError("print(JSON.stringify(1));"));
        assertEquals("Number has no method 'isInt'", onlyError("print(Number.isInt(1));"));
        assertEquals("'PI' of Math is not a function", onlyError("print(Math.PI());"));
    }

    @Test
    public void testMethodsOfLiterals() {
        assertEquals("a string has no method 'splitt' (did you mean 'split'?)", onlyError("print(\"a,b\".splitt(\",\"));"));
        assertEquals("an array has no method 'contains'", onlyError("print([1].contains(1));"));
        assertEquals("a number has no method 'toFixd' (did you mean 'toFixed'?)", onlyError("print((1).toFixd(2));"));
        assertEquals("'length' of a string is not a function", onlyError("print(\"abc\".length());"));
        assertEquals("repeat() needs 1 argument, not 0", onlyError("print(\"a\".repeat());"));
    }

    @Test
    public void testNumberOfArgumentsOfBuiltinFunctions() {
        assertEquals("parseInt() needs at least 1 argument, not 0", onlyError("print(parseInt());"));
        assertEquals("parseInt() takes at most 2 arguments, not 3", onlyError("print(parseInt(\"1\", 10, 2));"));
        assertEquals("isNaN() takes at most 1 argument, not 2", onlyError("print(isNaN(1, 2));"));
        assertEquals("range() needs at least 1 argument, not 0", onlyError("for (const i : range()) { }"));
    }

    @Test
    public void testWhatStaysAllowed() {
        // Known members, any number of arguments where allowed, spread, and names that hide a
        // builtin (builtins themselves cannot be changed: FrozenBuiltinsTest)
        String code = "print(Math.max(), Math.max(1, 2, 3), Math.PI > 3, \"abc\".length, [1, 2].length, Number.isInteger(2));\n"
                + "const xs = [3, 1];\nprint(Math.max(...xs), print == print);\n"
                + "const JSON = { stringify(x) { return \"s\"; } };\nprint(JSON.stringify(1));\n"
                + "print(typeof Math.XYZ, Math.__proto__ == Object.prototype);";
        assertEquals(List.of(), reportedErrors(code));
        assertSameOutput("-Infinity 3 true 3 2 true\n3 true\ns\nundefined true\n", code);
    }
}
