package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TinyscriptUnsupportedOperationError;

/**
 * The native module std:io: lines of the standard input, in the interpreter and in node.
 */
public class StdIoTest extends TinyscriptTranspilerTestBase {

    @TempDir
    Path dir;

    private Path script(String code) throws IOException {
        return Files.writeString(dir.resolve("main.tys"), "import readLine, lines from \"std:io\";\n" + code);
    }

    private void assertOutput(String expected, String code, String input) throws IOException {
        Path main = script(code);
        assertEquals(expected, runModuleWithInput(main, input));
        assertSameOutputModuleWithInput(main, input);
    }

    @Test
    public void testReadLine() throws IOException {
        assertOutput("Hallo Ada\n", "const name = readLine();\nprint(\"Hallo \" + name);", "Ada\nBabbage\n");
    }

    @Test
    public void testReadLineShowsThePrompt() throws IOException {
        assertOutput("Name? Hallo Ada\n", "print(\"Hallo \" + readLine(\"Name? \"));", "Ada\n");
    }

    @Test
    public void testEndOfInputIsNull() throws IOException {
        assertOutput("a\nb\nnull\nnull\n", "for (const i : range(4)) { print(readLine()); }", "a\nb\n");
        assertOutput("null\n", "print(readLine());", "");
    }

    @Test
    public void testLineBreaks() throws IOException {
        // \r\n as \n; a last line without a line break counts; empty lines stay
        assertOutput("[\"a\", \"\", \"b\", \"c\"]\n",
                "const all = [];\nfor (const line : lines()) { all.push(line); }\nprint(all);", "a\r\n\nb\nc");
    }

    @Test
    public void testUtf8() throws IOException {
        assertOutput("héllo € 7\n", "const line = readLine();\nprint(line, line.length);", "héllo €\n");
    }

    @Test
    public void testLines() throws IOException {
        assertOutput("6\n", "let sum = 0;\nfor (const line : lines()) { sum += Number(line); }\nprint(sum);", "1\n2\n3\n");
    }

    @Test
    public void testLinesAfterReadLineAndBreak() throws IOException {
        // lines() goes on where readLine stopped; a loop that breaks leaves the rest
        assertOutput("header\nx\ny\nz\nnull\n",
                "print(readLine());\n"
                        + "for (const line : lines()) { print(line); if (line == \"x\") { break; } }\n"
                        + "for (const line : lines()) { print(line); }\n"
                        + "print(readLine());",
                "header\nx\ny\nz\n");
    }

    @Test
    public void testLinesPrints() throws IOException {
        assertOutput("[Lines]\n", "print(lines());", "");
    }

    @Test
    public void testLinesIsLazy() throws IOException {
        // The loop sees a line before the next one is read: endless input works
        Path main = script("let n = 0;\nfor (const line : lines()) { n++; if (n == 3) { break; } }\nprint(n);");
        String out = runModuleWithEngine(main, engine -> engine.defineStdIn(new InputStream() {
            @Override
            public int read() {
                return '\n';
            }
        }));
        assertEquals("3\n", out);
    }

    @Test
    public void testPromptMustBeAString() throws IOException {
        assertThrows(TinyscriptTypeError.class, () -> runModuleWithInput(script("readLine(42);"), "a\n"));
        assertThrows(TinyscriptTypeError.class, () -> runModuleWithInput(script("readLine(null);"), "a\n"));
        assertThrows(TinyscriptTypeError.class, () -> runModuleWithInput(script("readLine(\"a\", \"b\");"), "a\n"));
        assertThrows(TinyscriptTypeError.class, () -> runModuleWithInput(script("lines(1);"), "a\n"));
    }

    @Test
    public void testReadErrorIsAnIOError() throws IOException {
        Path main = script("try { readLine(); } catch (e) { print(e.name, e.message); }");
        String out = runModuleWithEngine(main, engine -> engine.defineStdIn(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("broken");
            }
        }));
        assertEquals("IOError Cannot read the standard input: broken\n", out);
    }

    @Test
    public void testNotAvailableWhenSandboxed() throws IOException {
        Path main = script("print(1);");
        TinyscriptRuntimeException e = assertThrows(TinyscriptUnsupportedOperationError.class, () -> runModuleSandboxed(main));
        assertTrue(e.getMessage().contains("std:io is not available when running sandboxed"), e.getMessage());
    }

    private static final Path EXAMPLES = Path.of("../examples/io").toAbsolutePath().normalize();

    @Test
    public void testWordCountExample() {
        String input = "The cat and the dog.\nThe end\r\n";
        Path main = EXAMPLES.resolve("wordcount.tys");
        assertEquals("2 lines, 7 words, 27 characters\n   3  the\n   1  and\n   1  cat\n   1  dog.\n   1  end\n",
                runModuleWithInput(main, input));
        assertSameOutputModuleWithInput(main, input);
    }

    @Test
    public void testGuessExample() {
        // The number is random: guessing every number finds it; the end of the input gives up
        StringBuilder input = new StringBuilder("abc\n");
        for (int i = 1; i <= 100; i++)
            input.append(i).append('\n');
        String out = runModuleWithInput(EXAMPLES.resolve("guess.tys"), input.toString());
        assertTrue(out.startsWith("I am thinking of a number from 1 to 100.\nYour guess? Please type a whole number.\n"), out);
        assertTrue(out.matches("(?s).*You got it in \\d+ tr(y|ies)\\.\n"), out);
        assertTrue(runModuleWithInput(EXAMPLES.resolve("guess.tys"), "").matches("(?s).*Your guess\\? \nBye! It was \\d+\\.\n"));
    }
}
