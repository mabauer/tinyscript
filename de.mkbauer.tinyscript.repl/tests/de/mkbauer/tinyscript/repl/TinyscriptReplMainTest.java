package de.mkbauer.tinyscript.repl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
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
        return runWith(input, null, args);
    }

    private int runWith(String input, String tinyscriptPath, String... args) {
        return new TinyscriptReplMain(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true), new PrintStream(err, true), tempDir.resolve("history"), tinyscriptPath).run(args);
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
        assertTrue(err.toString().contains("TinyscriptTypeError: Cannot read 'x' of n: numbers have no properties"), err.toString());
        assertTrue(err.toString().contains("error.tys:2"), err.toString());
    }

    @Test
    public void testRuntimeErrorShowsTheSourceLine() throws IOException {
        Path file = script("area.tys", "function area(shape) {\n    return shape.width * shape.height;\n}\nprint(area({ width: 2 }));\n");
        assertEquals(TinyscriptReplMain.EXIT_ERROR, run(file.toString()));
        assertEquals("TinyscriptTypeError: '*' needs two numbers, but shape.height is undefined\n"
                + "        return shape.width * shape.height;\n"
                + "               ^\n"
                + "    at area(" + file + ":2)\n"
                + "    at global(" + file + ":4)\n", err.toString());
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
        assertTrue(err.toString().contains("Unknown name 'shared'"), err.toString());
    }

    @Test
    public void testInteractiveLinesSeeEarlierLines() {
        assertEquals(TinyscriptReplMain.EXIT_OK,
                runWithInput("var x = 21;\nfunction twice(a) {\nreturn 2 * a;\n}\nprint(twice(x));\n!quit\n"));
        assertTrue(out.toString().contains("42\n"), out.toString());
        assertEquals("", err.toString());
    }

    @Test
    public void testImportsRelativeToTheMainFile() throws IOException {
        script("util.tys", "export function twice(x) { return 2 * x; }");
        Path main = script("main.tys", "import twice from \"./util\";\nprint(twice(21));");
        assertEquals(TinyscriptReplMain.EXIT_OK, run(main.toString()));
        assertEquals("42\n", out.toString());
    }

    @Test
    public void testModulePathDefaultsToTheDirectoryOfTheMainFile() throws IOException {
        script("util.tys", "export function twice(x) { return 2 * x; }");
        Path main = script("main.tys", "import twice from \"util\";\nprint(twice(2));");
        assertEquals(TinyscriptReplMain.EXIT_OK, run(main.toString()));
        assertEquals("4\n", out.toString());
    }

    @Test
    public void testModulePathOption() throws IOException {
        Path mods = Files.createDirectory(tempDir.resolve("mods"));
        Path other = Files.createDirectory(tempDir.resolve("other"));
        Files.writeString(mods.resolve("half.tys"), "export function half(x) { return x / 2; }");
        Path main = script("main.tys", "import half from \"half\";\nprint(half(8));");
        assertEquals(TinyscriptReplMain.EXIT_OK, run("--module-path", other + File.pathSeparator + mods, main.toString()));
        assertEquals("4\n", out.toString());
    }

    @Test
    public void testModulePathFromEnvironment() throws IOException {
        Path mods = Files.createDirectory(tempDir.resolve("mods"));
        Files.writeString(mods.resolve("half.tys"), "export function half(x) { return x / 2; }");
        Path main = script("main.tys", "import half from \"half\";\nprint(half(8));");
        assertEquals(TinyscriptReplMain.EXIT_OK, runWith("", mods.toString(), main.toString()));
        assertEquals("4\n", out.toString());
    }

    @Test
    public void testMissingModuleIsAnError() throws IOException {
        Path main = script("main.tys", "import half from \"half\";\nprint(half(8));");
        assertEquals(TinyscriptReplMain.EXIT_ERROR, run(main.toString()));
        assertTrue(err.toString().contains("Module \"half\" not found"), err.toString());
    }

    @Test
    public void testModulePathOptionNeedsAValue() {
        assertEquals(TinyscriptReplMain.EXIT_USAGE, run("--module-path"));
        assertTrue(err.toString().contains("--module-path needs a value"), err.toString());
    }

    @Test
    public void testRunsFilesInOrderAndStopsAtTheFirstError() throws IOException {
        Path first = script("first.tys", "print(\"first\");");
        Path failing = script("failing.tys", "var n = 1;\nn.x = 1;");
        Path third = script("third.tys", "print(\"third\");");
        assertEquals(TinyscriptReplMain.EXIT_ERROR, run(first.toString(), failing.toString(), third.toString()));
        assertEquals("first\n", out.toString());
    }

    @Test
    public void testSyntaxErrorShowsOnlyTheFirstErrorWithTheSourceLine() throws IOException {
        Path file = script("cfor.tys", "let sum = 0;\nfor (let i = 0; i < 10; i++) {\n    sum += i;\n}\n");
        assertEquals(TinyscriptReplMain.EXIT_ERROR, run(file.toString()));
        assertEquals("TinyscriptSyntaxError: tinyscript's for loop has no ';': for (const i : range(n)) { … } counts from 0 to n - 1 in " + file + ":2:15\n"
                + "    for (let i = 0; i < 10; i++) {\n"
                + "                  ^\n", err.toString());
    }

    @Test
    public void testDeepRecursion() throws IOException {
        // Far deeper than the default Java stack allows
        Path file = script("deep.tys", "function depth(n) { if (n == 0) { return 0; } return 1 + depth(n - 1); }\nprint(depth(100000));");
        assertEquals(TinyscriptReplMain.EXIT_OK, run(file.toString()), err.toString());
        assertEquals("100000\n", out.toString());
    }

    @Test
    public void testEndlessRecursionIsAnError() throws IOException {
        Path file = script("endless.tys", "function f(n) { return f(n + 1); }\nf(0);");
        assertEquals(TinyscriptReplMain.EXIT_ERROR, run(file.toString()));
        assertTrue(err.toString().startsWith("TinyscriptStackOverflowError: Stack overflow"), err.toString());
        assertTrue(err.toString().lines().count() < 30, err.toString());
    }

    @Test
    public void testEndlessRecursionDoesNotEndTheInteractiveSession() {
        assertEquals(TinyscriptReplMain.EXIT_OK, runWithInput("function f(n) { return f(n + 1); }\nf(0);\nprint(\"alive\");\n"));
        assertTrue(out.toString().contains("TinyscriptStackOverflowError"), out.toString());
        assertTrue(out.toString().contains("alive"), out.toString());
    }

    @Test
    public void testScriptArgumentsAfterDoubleDash() throws IOException {
        Path file = script("args.tys", "print(System.args.length, \" \", System.args.join(\"|\"));");
        assertEquals(TinyscriptReplMain.EXIT_OK, run(file.toString(), "--", "input.txt", "-v", "x.tys"));
        assertEquals("3 input.txt|-v|x.tys\n", out.toString());
    }

    @Test
    public void testNoScriptArguments() throws IOException {
        Path file = script("args.tys", "print(System.args.length);");
        assertEquals(TinyscriptReplMain.EXIT_OK, run(file.toString()));
        assertEquals("0\n", out.toString());
    }
}
