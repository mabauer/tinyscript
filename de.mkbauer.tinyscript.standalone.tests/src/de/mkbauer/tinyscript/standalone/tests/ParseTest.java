package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptError;

/**
 * parse and findAll of std:string: reading structured text with a format, as Python's parse
 * library does. Written in tinyscript, so each test also runs against node.
 */
public class ParseTest extends TinyscriptTranspilerTestBase {

    private static final String IMPORT = "import parse, findAll from \"std:string\";\n";

    @TempDir
    Path dir;

    private Path script(String code) {
        try {
            return Files.writeString(dir.resolve("main.tys"), IMPORT + code);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void assertOutput(String expected, String code) {
        Path main = script(code);
        assertEquals(expected, runModule(main));
        assertSameOutputModule(main);
    }

    @Test
    public void testFieldTypes() {
        assertOutput("[47, 53]\n[94, 34]\n[0, 4, 3, -3]\n[\"x00\", \"AND\", \"y00\", \"z00\"]\n"
                + "[3.5, -0.25, \"kg\"]\n[1000, -2.5, 7, 0.5]\n[\"Hello\", \"wörld_2\", \"a-b/c\"]\n",
                "print(parse(\"{:d}|{:d}\", \"47|53\"));\n"
                + "print(parse(\"Button A: X+{:d}, Y+{:d}\", \"Button A: X+94, Y+34\"));\n"
                + "print(parse(\"p={:d},{:d} v={:d},{:d}\", \"p=0,4 v=3,-3\"));\n"
                + "print(parse(\"{:w} {:w} {:w} -> {:w}\", \"x00 AND y00 -> z00\"));\n"
                + "print(parse(\"{:f} {:f} {:l}\", \"3.5 -.25 kg\"));\n"
                + "print(parse(\"{:g},{:g},{:g},{:g}\", \"1e3,-2.5,+7,.5\"));\n"
                + "print(parse(\"{:l}, {:w}: {:S}\", \"Hello, wörld_2: a-b/c\"));");
    }

    @Test
    public void testTextFields() {
        assertOutput("[190, \"10 19\"]\n[\"a\", \"b c\"]\n[\"key\", \" value \"]\n[\"x\"]\n[]\n",
                "print(parse(\"{:d}: {}\", \"190: 10 19\"));\n"
                + "print(parse(\"{} -> {}\", \"a -> b c\"));\n"
                + "print(parse(\"{}={}.\", \"key= value .\"));\n"
                + "print(parse(\"{{{}}}\", \"{x}\"));\n"
                + "print(parse(\"done\", \"done\"));");
    }

    @Test
    public void testTheWholeTextMustFit() {
        // Trailing whitespace is fine; a space in the format matches one or more spaces or tabs
        assertOutput("[3, 4]\n[3, 4]\nnull\nnull\nnull\nnull\nnull\nnull\nnull\n[\"x\"]\n",
                "print(parse(\"{:d} {:d}\", \"3   4\\n\"));\n"
                + "print(parse(\"{:d} {:d}\", \"3\\t4  \"));\n"
                + "print(parse(\"{:d} {:d}\", \"34\"));\n"
                + "print(parse(\"{:d}|{:d}\", \"47-53\"));\n"
                + "print(parse(\"{:d}\", \"12x\"));\n"
                + "print(parse(\"{:d}\", \"1.5\"));\n"
                + "print(parse(\"{:f}\", \"3\"));\n"
                + "print(parse(\"{}\", \"\"));\n"
                + "print(parse(\"a{}b\", \"ab\"));\n"
                + "print(parse(\"a{}b\", \"axb\"));");
    }

    @Test
    public void testFormatsCanBeTriedOneAfterTheOther() {
        assertOutput("move 3\nturn L\nunknown: jump\n",
                "for (const line : [\"move 3\", \"turn L\", \"jump\"]) {\n"
                + "    const move = parse(\"move {:d}\", line);\n"
                + "    const turn = parse(\"turn {:l}\", line);\n"
                + "    if (move != null) { print(\"move\", move[0]); }\n"
                + "    else if (turn != null) { print(\"turn\", turn[0]); }\n"
                + "    else { print(\"unknown:\", line); }\n"
                + "}");
    }

    @Test
    public void testNoBacktracking() {
        // Fields take as much as their type allows: {:d}{:d} cannot split "123", {:w}abc not "xyzabc"
        assertOutput("null\nnull\n[\"xyz\"]\n",
                "print(parse(\"{:d}{:d}\", \"123\"));\n"
                + "print(parse(\"{:w}abc\", \"xyzabc\"));\n"
                + "print(parse(\"{}abc\", \"xyzabc\"));");
    }

    @Test
    public void testFindAll() {
        assertOutput("[[2, 4], [5, 5], [11, 8], [8, 5]]\n40\n[]\n[[\"a\", 1], [\"b\", 22]]\n[[\"x\"], [\"y z\"]]\n",
                "const memory = \"xmul(2,4)%&mul[3,7]!@^do_not_mul(5,5)+mul(32,64]then(mul(11,8)mul(8,5))\";\n"
                + "const pairs = findAll(\"mul({:d},{:d})\", memory);\n"
                + "print(pairs);\n"
                + "let total = 0;\n"
                + "for (const [a, b] : findAll(\"mul({:d},{:d})\", \"mul(2,4)mul(4,8)\")) { total += a * b; }\n"
                + "print(total);\n"
                + "print(findAll(\"mul({:d},{:d})\", \"nothing\"));\n"
                + "print(findAll(\"{:l}={:d}\", \"a=1, b=22; c=x\"));\n"
                + "print(findAll(\"<{}>\", \"<x> and <y z>\"));");
    }

    @Test
    public void testErrorsInTheFormat() {
        assertEquals("parse: unknown field {:x} in \"{:x}\" (fields: {}, {:d}, {:f}, {:g}, {:w}, {:l}, {:S}; {{ and }} for braces)",
                errorMessage("parse(\"{:x}\", \"1\");"));
        assertEquals("parse: unknown field {name} in \"{name}\" (fields: {}, {:d}, {:f}, {:g}, {:w}, {:l}, {:S}; {{ and }} for braces)",
                errorMessage("parse(\"{name}\", \"1\");"));
        assertTrue(errorMessage("parse(\"{:d\", \"1\");").startsWith("parse: unknown field {:d in"));
        assertEquals("parse: a single } in \"a}\" (write }} for a brace)", errorMessage("parse(\"a}\", \"a\");"));
        assertEquals("parse: {} needs text after it in \"{}{:d}\", to know where it ends", errorMessage("parse(\"{}{:d}\", \"a1\");"));
        assertEquals("findAll: {} needs text after it in \"x{}\", to know where it ends", errorMessage("findAll(\"x{}\", \"xa\");"));
        assertEquals("parse: the format is empty", errorMessage("parse(\"\", \"\");"));
        assertEquals("parse: the text must be a string, not number", errorMessage("parse(\"{:d}\", 5);"));
        assertEquals("findAll: the format must be a string, not undefined", errorMessage("findAll(undefined, \"x\");"));
    }

    private String errorMessage(String code) {
        TinyscriptRuntimeException e = assertThrows(TinyscriptError.class, () -> runModule(script(code)));
        return e.getMessage();
    }
}
