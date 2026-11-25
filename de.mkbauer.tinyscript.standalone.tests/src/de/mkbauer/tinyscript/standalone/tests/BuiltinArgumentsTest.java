package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Builtins understand the optional arguments of their Javascript counterparts; more
 * arguments than a builtin takes are a TypeError instead of being ignored silently.
 */
public class BuiltinArgumentsTest extends TinyscriptTranspilerTestBase {

    @TempDir
    Path tempDir;

    // =========================================================================
    // Optional arguments as in Javascript
    // =========================================================================

    @Test
    public void testStringSearchWithAPosition() {
        String code = "print(\"abc\".startsWith(\"b\", 1), \"abc\".startsWith(\"a\", 1), \"abc\".startsWith(\"a\", -5), \"abc\".startsWith(\"\", 9));\n"
                + "print(\"abc\".endsWith(\"b\", 2), \"abc\".endsWith(\"c\", 2), \"abc\".endsWith(\"c\", 9), \"abc\".endsWith(\"\", -1));\n"
                + "print(\"abca\".includes(\"a\", 1), \"abc\".includes(\"a\", 1), \"abc\".includes(\"a\", -1));";
        assertSameOutput("true false true true\ntrue false true true\ntrue false true\n", code);
    }

    @Test
    public void testSplitWithALimit() {
        String code = "print(\"a-b-c\".split(\"-\", 2).join(\"|\")); print(\"a-b-c\".split(\"-\", 0).length); print(\"a-b-c\".split(\"-\", 9).length); print(\"abc\".split(\"\", 2).join(\"|\"));";
        assertSameOutput("a|b\n0\n3\na|b\n", code);
    }

    @Test
    public void testArrayIncludesFromAnIndex() {
        String code = "let a = [1, 2, 3, 1]; print(a.includes(1, 1), a.includes(2, 2), a.includes(3, -2), a.includes(2, -1), a.includes(1, 9));";
        assertSameOutput("true false true false false\n", code);
    }

    @Test
    public void testOptionalArgumentsThatWereAlreadySupported() {
        String code = "print(\"abcabc\".indexOf(\"b\", 2), \"abcabc\".lastIndexOf(\"b\", 3), \"hello\".slice(), \"7\".padStart(3, \"0\"), \"7\".padEnd(3, \"-\"));\n"
                + "print([1, 2, 1].indexOf(1, 1), [1, 2, 3].slice(1).join(), [1, 2].join(\"+\"), [1, 2].reduce((s, x) => s + x, 10), [0, 0, 0].fill(1, 1, 2).join());\n"
                + "print(Math.max(), Math.min(), Math.max(1, 5, 3), parseInt(\"ff\", 16), String.fromCharCode());";
        assertSameOutput("4 1 hello 007 7--\n2 2,3 1+2 13 0,1,0\n-Infinity Infinity 5 255 \n", code);
    }

    // =========================================================================
    // Too many arguments
    // =========================================================================

    // A known gap of the generated code (TODO.md, phase 2): its builtins do not check the
    // maximum, since callbacks get more arguments
    private void assertTooManyArguments(String expectedMessage, String code) {
        assertEquals(expectedMessage, assertKnownGap(TinyscriptTypeError.class, code).getMessage());
    }

    private String onlyError(String code) {
        java.util.List<String> errors = reportedErrors(code);
        assertEquals(1, errors.size(), errors.toString());
        return errors.get(0).replaceFirst("^\\d+:\\d+: ", "");
    }

    @Test
    public void testTooManyArgumentsAreATypeError() {
        // Where the receiver is only known at run time
        assertTooManyArguments("startsWith() takes at most 2 arguments, not 3", "const s = \"abc\";\ns.startsWith(\"a\", 0, 1);");
        assertTooManyArguments("toUpperCase() takes no arguments, not 1", "const s = \"abc\";\ns.toUpperCase(\"x\");");
        assertTooManyArguments("map() takes at most 1 argument, not 2", "const a = [1];\na.map(x => x, {});");
        assertTooManyArguments("get() takes at most 1 argument, not 2", "new Map().get(1, 2);");
        // Where it is known, already before the run
        assertEquals("startsWith() takes at most 2 arguments, not 3", onlyError("\"abc\".startsWith(\"a\", 0, 1);"));
        assertEquals("toUpperCase() takes no arguments, not 1", onlyError("\"abc\".toUpperCase(\"x\");"));
        assertEquals("sqrt() takes at most 1 argument, not 2", onlyError("Math.sqrt(4, 2);"));
        assertEquals("map() takes at most 1 argument, not 2", onlyError("[1].map(x => x, {});"));
        assertEquals("Number() takes at most 1 argument, not 2", onlyError("Number(\"1\", 2);"));
        assertEquals("replace() takes at most 2 arguments, not 3", onlyError("\"a\".replace(\"a\", \"b\", \"c\");"));
    }

    @Test
    public void testVariadicBuiltins() {
        String code = "let a = []; a.push(1, 2, 3); a.unshift(-1, 0); print(a.join()); print(Math.max(1, 2, 3, 4), String.fromCharCode(97, 98, 99));\n"
                + "function f(x, y) { return x + y; } print(f.call(null, 1, 2), f.apply(null, [3, 4]));";
        assertSameOutput("-1,0,1,2,3\n4 abc\n3 7\n", code);
    }

    @Test
    public void testBuiltinsAsCallbacksGetItemIndexAndArray() {
        // Like Javascript, a callback gets (item, index, array); a builtin used as callback ignores the rest
        String code = "print([\"1\", \"2\", \"3\"].map(Number).reduce((s, x) => s + x, 0)); print([4, 9].map(Math.sqrt).join());";
        assertSameOutput("6\n2,3\n", code);
    }

    @Test
    public void testUserFunctionsIgnoreExtraArguments() {
        // As in Javascript, a function value (a callback, a method) ignores extra arguments; a
        // call of a declared function with too many is rejected before the run (ErrorMessagesTest)
        String code = "function f(x) { return x; } const g = f; print(g(1, 2, 3), [1].map(x => x * 2));";
        assertSameOutput("1 [2]\n", code);
    }

    // =========================================================================
    // std:string
    // =========================================================================

    private Path write(String name, String code) throws IOException {
        return Files.writeString(tempDir.resolve(name), code);
    }

    @Test
    public void testIntegers() throws IOException {
        Path main = write("main.tys", "import integers from \"std:string\";\n"
                + "print(integers(\"p=0,4 v=3,-3\").join()); print(integers(\"Button A: X+94, Y+34\").join()); print(integers(\"a-b - 7-\").join()); print(integers(\"none\").length);");
        assertEquals("0,4,3,-3\n94,34\n7\n0\n", runModule(main));
        assertSameOutputModule(main);
    }

    @Test
    public void testWords() throws IOException {
        Path main = write("main.tys", "import words, isWhitespace, isDigit from \"std:string\";\n"
                + "print(words(\"  3   4\\t5\\n\").join(\"|\")); print(words(\"\").length); print(words(\"   \").length); print(words(\"one\").join(\"|\"));\n"
                + "print(isWhitespace(\" \"), isWhitespace(\"x\"), isDigit(\"7\"), isDigit(\"x\"), isDigit(\"a\"[5]));");
        assertEquals("3|4|5\n0\n0\none\ntrue false true false false\n", runModule(main));
        assertSameOutputModule(main);
    }
}
