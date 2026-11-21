package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptIOError;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TinyscriptUnsupportedOperationError;

/**
 * The native module std:fs (strict: errors end the script; UTF-8).
 */
public class StdFsTest extends TinyscriptTranspilerTestBase {

    @TempDir
    Path dir;

    /** A script that imports std:fs and can use the variable dir (the temp directory). */
    private Path script(String code) throws IOException {
        return Files.writeString(dir.resolve("main.tys"),
                "import * as fs from \"std:fs\";\nlet dir = \"" + dir.toString().replace("\\", "\\\\") + "\";\n" + code);
    }

    @Test
    public void testWriteAndReadFile() throws IOException {
        Path main = script("let p = dir + \"/a.txt\";\n"
                + "print(fs.exists(p));\n"
                + "print(fs.writeFile(p, \"héllo €\") == null);\n"
                + "print(fs.exists(p)); print(fs.readFile(p)); print(fs.readFile(p).length);");
        assertEquals("false\ntrue\ntrue\nhéllo €\n7\n", runModule(main));
        assertEquals("héllo €", Files.readString(dir.resolve("a.txt"), StandardCharsets.UTF_8));
        // The differential test runs the script twice (interpreter, node), so no exists() before writing
        assertSameOutputModule(script("let p = dir + \"/b.txt\";\n"
                + "print(fs.writeFile(p, \"héllo €\") == null); print(fs.exists(p)); print(fs.readFile(p));"));
    }

    @Test
    public void testWriteLinesAndReadLinesRoundTrip() throws IOException {
        Path main = script("let p = dir + \"/lines.txt\";\n"
                + "fs.writeLines(p, [\"a\", \"b\", \"\"]);\n"
                + "let lines = fs.readLines(p);\n"
                + "print(lines.length); print(lines.join(\"|\")); print(fs.readFile(p) == \"a\\nb\\n\\n\");\n"
                + "fs.writeLines(p, []); print(fs.readLines(p).length);");
        assertEquals("3\na|b|\ntrue\n0\n", runModule(main));
        assertSameOutputModule(main);
    }

    @Test
    public void testReadLinesSplitsAtCrLfAndWithoutFinalLineBreak() throws IOException {
        Files.writeString(dir.resolve("crlf.txt"), "x\r\ny\r\nz");
        Path main = script("let lines = fs.readLines(dir + \"/crlf.txt\");\nprint(lines.length); print(lines.join(\"|\"));");
        assertEquals("3\nx|y|z\n", runModule(main));
        assertSameOutputModule(main);
    }

    @Test
    public void testAppendFile() throws IOException {
        Path main = script("let p = dir + \"/log.txt\";\n"
                + "fs.writeFile(p, \"\");\n"
                + "fs.appendFile(p, \"one\\n\"); fs.appendFile(p, \"two\\n\");\n"
                + "print(fs.readLines(p).join(\",\"));");
        assertEquals("one,two\n", runModule(main));
        assertSameOutputModule(main);
    }

    @Test
    public void testAppendFileCreatesTheFile() throws IOException {
        Path main = script("let p = dir + \"/new.txt\";\nfs.appendFile(p, \"x\");\nprint(fs.readFile(p));");
        assertEquals("x\n", runModule(main));
    }

    @Test
    public void testListDir() throws IOException {
        Files.createDirectory(dir.resolve("sub"));
        Files.writeString(dir.resolve("sub/b.txt"), "");
        Files.writeString(dir.resolve("sub/a.txt"), "");
        Files.createDirectory(dir.resolve("sub/c"));
        Path main = script("print(fs.listDir(dir + \"/sub\").join(\",\"));");
        assertEquals("a.txt,b.txt,c\n", runModule(main));
        assertSameOutputModule(main);
    }

    @Test
    public void testReadingAMissingFileEndsTheScript() throws IOException {
        Path main = script("print(\"before\");\nfs.readFile(dir + \"/nope.txt\");\nprint(\"after\");");
        TinyscriptRuntimeException e = assertThrows(TinyscriptIOError.class, () -> runModule(main));
        assertTrue(e.getMessage().contains("Cannot read file") && e.getMessage().contains("nope.txt") && e.getMessage().contains("not found"),
                e.getMessage());
        assertEquals(4, e.getAffectedLine());
    }

    @Test
    public void testListingAMissingDirectoryEndsTheScript() throws IOException {
        Path main = script("fs.listDir(dir + \"/nope\");");
        TinyscriptRuntimeException e = assertThrows(TinyscriptIOError.class, () -> runModule(main));
        assertTrue(e.getMessage().contains("Cannot list directory"), e.getMessage());
    }

    @Test
    public void testOnlyStringsCanBeWritten() throws IOException {
        assertThrows(TinyscriptTypeError.class, () -> runModule(script("fs.writeFile(dir + \"/x\", 42);")));
        assertThrows(TinyscriptTypeError.class, () -> runModule(script("fs.writeLines(dir + \"/x\", \"a\");")));
        assertThrows(TinyscriptTypeError.class, () -> runModule(script("fs.writeLines(dir + \"/x\", [\"a\", 1]);")));
        assertThrows(TinyscriptTypeError.class, () -> runModule(script("fs.appendFile(dir + \"/x\", true);")));
        assertThrows(TinyscriptTypeError.class, () -> runModule(script("fs.exists(42);")));
    }

    @Test
    public void testNotAvailableWhenSandboxed() throws IOException {
        Path main = script("print(1);");
        TinyscriptRuntimeException e = assertThrows(TinyscriptUnsupportedOperationError.class, () -> runModuleSandboxed(main));
        assertTrue(e.getMessage().contains("std:fs is not available when running sandboxed"), e.getMessage());
        assertEquals(1, e.getAffectedLine());
    }
}
