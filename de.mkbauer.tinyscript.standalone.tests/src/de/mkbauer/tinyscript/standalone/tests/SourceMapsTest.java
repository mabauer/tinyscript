package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.mkbauer.tinyscript.generator.SourceMaps;

/**
 * Source maps of the generated Javascript: node (with --enable-source-maps) reports an error that
 * ends the program with the tinyscript line, as the interpreter does.
 */
public class SourceMapsTest extends TinyscriptTranspilerTestBase {

    @TempDir
    Path tempDir;

    @Test
    public void testBase64Vlq() {
        assertEquals("A", SourceMaps.vlq(0));
        assertEquals("C", SourceMaps.vlq(1));
        assertEquals("D", SourceMaps.vlq(-1));
        assertEquals("gB", SourceMaps.vlq(16));
        assertEquals("hB", SourceMaps.vlq(-16));
        assertEquals("2H", SourceMaps.vlq(123));
    }

    @Test
    public void testMarkersBecomeAMap() {
        String code = SourceMaps.marker(0, 0, 0) + "let a = 1;\n  " + SourceMaps.marker(0, 1, 2) + "f(" + SourceMaps.marker(1, 4, 0) + "a);\n";
        String result = SourceMaps.attach(code, List.of("file:///x/main.tys", "std:array"), List.of("let a = 1;\n  f(a);", "x"));
        String[] lines = result.split("\n");
        assertEquals("let a = 1;", lines[0]);
        assertEquals("  f(a);", lines[1]);
        assertTrue(lines[2].startsWith("//# sourceMappingURL=data:application/json;charset=utf-8;base64,"), lines[2]);
        String map = new String(Base64.getDecoder().decode(lines[2].substring(lines[2].indexOf(",") + 1)), StandardCharsets.UTF_8);
        assertEquals("{\"version\":3,\"sources\":[\"file:///x/main.tys\",\"std:array\"],\"sourcesContent\":[\"let a = 1;\\n  f(a);\",\"x\"],"
                + "\"names\":[],\"mappings\":\"AAAA;EACE,ECGF;\"}", map);
        assertEquals("let a = 1;\n  f(a);\n", SourceMaps.strip(code));
    }

    private String nodeErrors(Path main) {
        assumeNodeAvailable();
        NodeJsRunner.Result result = NodeJsRunner.execute(transpileModule(main), "");
        assertEquals(1, result.exitCode(), result.stderr());
        return result.stderr();
    }

    @Test
    public void testAnErrorShowsTheTinyscriptLine() throws IOException {
        Path main = Files.writeString(tempDir.resolve("area.tys"), "function area(shape) {\n"
                + "\treturn shape.width * shape.height;\n}\nprint(area({ width: 2, height: 3 }));\nprint(area({ width: 2 }));\n");
        assertEquals("TypeError: '*' needs two numbers, not number and undefined\n"
                + "    \treturn shape.width * shape.height;\n"
                + "    \t       ^\n"
                + "    at area(" + main + ":2)\n"
                + "    at global(" + main + ":5)\n", nodeErrors(main));
    }

    @Test
    public void testErrorsInModulesAndTheStandardLibrary() throws IOException {
        Files.writeString(tempDir.resolve("grid.tys"), "export function cell(grid, r) {\n    return grid[r].length;\n}\n");
        Path main = Files.writeString(tempDir.resolve("main.tys"), "import cell from \"./grid\";\nprint(cell([[1]], 0));\nprint(cell([], 1));\n");
        String errors = nodeErrors(main);
        assertTrue(errors.startsWith("TypeError: Cannot read properties of undefined (reading 'length')\n"
                + "        return grid[r].length;\n"
                + "               ^\n"), errors);
        // (the module loader resolves the real path of a module)
        assertTrue(errors.endsWith("    at cell(" + tempDir.toRealPath().resolve("grid.tys") + ":2)\n    at global(" + main + ":3)\n"), errors);
        // An error inside std: is shown at the call, as by the interpreter
        Path std = Files.writeString(tempDir.resolve("std.tys"), "import min from \"std:array\";\nprint(1);\nprint(min([]));\n");
        String stdErrors = nodeErrors(std);
        assertTrue(stdErrors.startsWith("Error: min: there are no elements\n    print(min([]));\n"), stdErrors);
        assertFalse(stdErrors.contains("std:"), stdErrors);
        assertTrue(stdErrors.endsWith("    at global(" + std + ":3)\n"), stdErrors);
    }
}
