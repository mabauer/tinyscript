package de.mkbauer.tinyscript.repl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the command line behavior of the REPL: options, exit codes and error messages.
 */
public class TinyscriptReplMainTest {

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        return runWithInput("", args);
    }

    private int runWithInput(String input, String... args) {
        return new TinyscriptReplMain(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true), new PrintStream(err, true), tempDir.resolve("history")).run(args);
    }

    private Path script(String name, String code) throws IOException {
        return Files.writeString(tempDir.resolve(name), code);
    }

    @Test
    public void testHelp() {
        assertEquals(TinyscriptReplMain.EXIT_OK, run("--help"));
        assertTrue(out.toString().startsWith("Usage: tinyscript"), out.toString());
        assertEquals(TinyscriptReplMain.EXIT_OK, run("-h"));
    }

    @Test
    public void testVersion() {
        assertEquals(TinyscriptReplMain.EXIT_OK, run("--version"));
        assertTrue(out.toString().startsWith("tinyscript "), out.toString());
    }

    @Test
    public void testUnknownOptionIsAUsageError() {
        assertEquals(TinyscriptReplMain.EXIT_USAGE, run("-x"));
        assertTrue(err.toString().contains("Unknown option: -x"), err.toString());
    }

    @Test
    public void testMissingFileIsAUsageError() {
        assertEquals(TinyscriptReplMain.EXIT_USAGE, run(tempDir.resolve("missing.tys").toString()));
        assertTrue(err.toString().contains("File not found"), err.toString());
    }

    @Test
    public void testFileWithoutTysExtensionIsAUsageError() throws IOException {
        Path file = script("old.ts", "print(1);");
        assertEquals(TinyscriptReplMain.EXIT_USAGE, run(file.toString()));
        assertTrue(err.toString().contains("Not a tinyscript file (expected .tys)"), err.toString());
    }

    @Test
    public void testRunsAFile() throws IOException {
        Path file = script("hello.tys", "print(\"Hello\");");
        assertEquals(TinyscriptReplMain.EXIT_OK, run(file.toString()));
        assertEquals("Hello\n", out.toString());
        assertEquals("", err.toString());
    }

    @Test
    public void testRunsAFileInADirectoryWithSpaces() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("dir with spaces"));
        Path file = Files.writeString(dir.resolve("hello.tys"), "print(1 + 1);");
        assertEquals(TinyscriptReplMain.EXIT_OK, run(file.toString()));
        assertEquals("2\n", out.toString());
    }

    @Test
    public void testRuntimeErrorExitsWithOne() throws IOException {
        Path file = script("error.tys", "var n = 1;\nprint(n.x);");
        assertEquals(TinyscriptReplMain.EXIT_ERROR, run(file.toString()));
        assertTrue(err.toString().contains("TinyscriptTypeError: Numbers have no properties"), err.toString());
        assertTrue(err.toString().contains("error.tys:2"), err.toString());
    }

    @Test
    public void testSyntaxErrorExitsWithOne() throws IOException {
        Path file = script("syntax.tys", "print(1);\nvar ;");
        assertEquals(TinyscriptReplMain.EXIT_ERROR, run(file.toString()));
        assertTrue(err.toString().contains("TinyscriptSyntaxError"), err.toString());
        assertTrue(err.toString().contains("syntax.tys:2"), err.toString());
        assertEquals("", out.toString());
    }

    @Test
    public void testFilesDoNotShareGlobals() throws IOException {
        // Files share names only via import / export; each file runs on its own
        Path first = script("first.tys", "var shared = 5;");
        Path second = script("second.tys", "print(shared);");
        assertEquals(TinyscriptReplMain.EXIT_ERROR, run(first.toString(), second.toString()));
        assertTrue(err.toString().contains("Couldn't resolve reference to Identifier 'shared'"), err.toString());
    }

    @Test
    public void testInteractiveLinesSeeEarlierLines() {
        assertEquals(TinyscriptReplMain.EXIT_OK,
                runWithInput("var x = 21;\nfunction twice(a) {\nreturn 2 * a;\n}\nprint(twice(x));\n!quit\n"));
        assertTrue(out.toString().contains("42\n"), out.toString());
        assertEquals("", err.toString());
    }

    @Test
    public void testRunsFilesInOrderAndStopsAtTheFirstError() throws IOException {
        Path first = script("first.tys", "print(\"first\");");
        Path failing = script("failing.tys", "var n = 1;\nn.x = 1;");
        Path third = script("third.tys", "print(\"third\");");
        assertEquals(TinyscriptReplMain.EXIT_ERROR, run(first.toString(), failing.toString(), third.toString()));
        assertEquals("first\n", out.toString());
    }
}
