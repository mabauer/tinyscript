package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
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
        String code = "print(\"abc\".startsWith(\"b\", 1), \" \", \"abc\".startsWith(\"a\", 1), \" \", \"abc\".startsWith(\"a\", -5), \" \", \"abc\".startsWith(\"\", 9));\n"
                + "print(\"abc\".endsWith(\"b\", 2), \" \", \"abc\".endsWith(\"c\", 2), \" \", \"abc\".endsWith(\"c\", 9), \" \", \"abc\".endsWith(\"\", -1));\n"
                + "print(\"abca\".includes(\"a\", 1), \" \", \"abc\".includes(\"a\", 1), \" \", \"abc\".includes(\"a\", -1));";
        assertEquals("true false true true\ntrue false true true\ntrue false true\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testSplitWithALimit() {
        String code = "print(\"a-b-c\".split(\"-\", 2).join(\"|\")); print(\"a-b-c\".split(\"-\", 0).length); print(\"a-b-c\".split(\"-\", 9).length); print(\"abc\".split(\"\", 2).join(\"|\"));";
        assertEquals("a|b\n0\n3\na|b\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testArrayIncludesFromAnIndex() {
        String code = "var a = [1, 2, 3, 1]; print(a.includes(1, 1), \" \", a.includes(2, 2), \" \", a.includes(3, -2), \" \", a.includes(2, -1), \" \", a.includes(1, 9));";
        assertEquals("true false true false false\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testOptionalArgumentsThatWereAlreadySupported() {
        String code = "print(\"abcabc\".indexOf(\"b\", 2), \" \", \"abcabc\".lastIndexOf(\"b\", 3), \" \", \"hello\".slice(), \" \", \"7\".padStart(3, \"0\"), \" \", \"7\".padEnd(3, \"-\"));\n"
                + "print([1, 2, 1].indexOf(1, 1), \" \", [1, 2, 3].slice(1).join(), \" \", [1, 2].join(\"+\"), \" \", [1, 2].reduce((s, x) => s + x, 10), \" \", [0, 0, 0].fill(1, 1, 2).join());\n"
                + "print(Math.max(), \" \", Math.min(), \" \", Math.max(1, 5, 3), \" \", parseInt(\"ff\", 16), \" \", String.fromCharCode());";
        assertEquals("4 1 hello 007 7--\n2 2,3 1+2 13 0,1,0\n-Infinity Infinity 5 255 \n", interpreterOutput(code));
        assertSameOutput(code);
    }

    // =========================================================================
    // Too many arguments
    // =========================================================================

    private void assertTooManyArguments(String expectedMessage, String code) {
        TinyscriptRuntimeException e = assertThrows(TinyscriptTypeError.class, () -> interpreterOutput(code));
        assertEquals(expectedMessage, e.getMessage());
    }

    @Test
    public void testTooManyArgumentsAreATypeError() {
        assertTooManyArguments("startsWith() takes at most 2 arguments, not 3", "\"abc\".startsWith(\"a\", 0, 1);");
        assertTooManyArguments("toUpperCase() takes no arguments, not 1", "\"abc\".toUpperCase(\"x\");");
        assertTooManyArguments("sqrt() takes at most 1 argument, not 2", "Math.sqrt(4, 2);");
        assertTooManyArguments("map() takes at most 1 argument, not 2", "[1].map(x => x, {});");
        assertTooManyArguments("get() takes at most 1 argument, not 2", "new Map().get(1, 2);");
        assertTooManyArguments("Number() takes at most 1 argument, not 2", "Number(\"1\", 2);");
        assertTooManyArguments("replace() takes at most 2 arguments, not 3", "\"a\".replace(\"a\", \"b\", \"c\");");
    }

    @Test
    public void testVariadicBuiltins() {
        String code = "var a = []; a.push(1, 2, 3); a.unshift(-1, 0); print(a.join()); print(Math.max(1, 2, 3, 4), String.fromCharCode(97, 98, 99));\n"
                + "function f(x, y) { return x + y; } print(f.call(null, 1, 2), \" \", f.apply(null, [3, 4]));";
        assertEquals("-1,0,1,2,3\n4abc\n3 7\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testBuiltinsAsCallbacksGetItemIndexAndArray() {
        // Like Javascript, a callback gets (item, index, array); a builtin used as callback ignores the rest
        String code = "print([\"1\", \"2\", \"3\"].map(Number).reduce((s, x) => s + x, 0)); print([4, 9].map(Math.sqrt).join());";
        assertEquals("6\n2,3\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testUserFunctionsIgnoreExtraArguments() {
        // Unchanged: as in Javascript, extra arguments of user-defined functions are ignored
        String code = "function f(x) { return x; } print(f(1, 2, 3));";
        assertEquals("1\n", interpreterOutput(code));
    }

    // =========================================================================
    // std:string
    // =========================================================================

    private Path write(String name, String code) throws IOException {
        return Files.writeString(tempDir.resolve(name), code);
    }

    @Test
    public void testWords() throws IOException {
        Path main = write("main.tys", "import words, isWhitespace, isDigit from \"std:string\";\n"
                + "print(words(\"  3   4\\t5\\n\").join(\"|\")); print(words(\"\").length); print(words(\"   \").length); print(words(\"one\").join(\"|\"));\n"
                + "print(isWhitespace(\" \"), \" \", isWhitespace(\"x\"), \" \", isDigit(\"7\"), \" \", isDigit(\"x\"), \" \", isDigit(\"a\"[5]));");
        assertEquals("3|4|5\n0\n0\none\ntrue false true false false\n", runModule(main));
        assertSameOutputModule(main);
    }
}
