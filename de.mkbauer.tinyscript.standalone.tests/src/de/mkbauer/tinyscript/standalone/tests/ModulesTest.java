package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.TinyscriptSyntaxError;
import de.mkbauer.tinyscript.interpreter.FileSystemModuleLoader;
import de.mkbauer.tinyscript.interpreter.StandardLibraryModuleLoader;
import de.mkbauer.tinyscript.interpreter.TinyscriptReferenceError;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TinyscriptUnsupportedOperationError;

/**
 * Modules: import / export across files (see the plan in TODO.md).
 */
public class ModulesTest extends TinyscriptTranspilerTestBase {

    @TempDir
    Path dir;

    private Path write(String name, String code) throws IOException {
        Path file = dir.resolve(name);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, code);
    }

    @BeforeEach
    public void writeGeometry() throws IOException {
        write("geometry.tys", "export function area(r) { return 3 * r * r; }\n"
                + "export let unit = \"cm\";\n"
                + "let helper = 2;\n");
    }

    // =========================================================================
    // Importing
    // =========================================================================

    @Test
    public void testNamedImports() throws IOException {
        Path main = write("main.tys", "import area, unit from \"./geometry\";\nprint(area(2), unit);");
        assertEquals("12 cm\n", runModule(main));
    }

    @Test
    public void testNamespaceImport() throws IOException {
        Path main = write("main.tys", "import * as geo from \"./geometry.tys\";\n"
                + "print(geo.area(1)); print(geo.unit); print(geo.helper == null);");
        assertEquals("3\ncm\ntrue\n", runModule(main));
    }

    @Test
    public void testPrivateNamesAreNotExported() throws IOException {
        Path main = write("main.tys", "import helper from \"./geometry\";\nprint(helper);");
        TinyscriptRuntimeException e = assertThrows(TinyscriptReferenceError.class, () -> runModule(main));
        assertTrue(e.getMessage().contains("does not export \"helper\""), e.getMessage());
        assertEquals(1, e.getAffectedLine());
    }

    @Test
    public void testRelativePathsResolveAgainstTheImportingFile() throws IOException {
        write("util.tys", "export function twice(x) { return 2 * x; }");
        write("lib/vector.tys", "import twice from \"../util\";\nexport function scale(x) { return twice(x); }");
        Path main = write("main.tys", "import scale from \"./lib/vector\";\nprint(scale(21));");
        assertEquals("42\n", runModule(main));
    }

    @Test
    public void testBarePathsUseTheModulePath() throws IOException {
        write("mods/math/half.tys", "export function half(x) { return x / 2; }");
        Path main = write("app/main.tys", "import half from \"math/half\";\nprint(half(8));");
        assertEquals("4\n", runModule(main, dir.resolve("mods")));
    }

    @Test
    public void testModuleNotFoundNamesWhatWasSearched() throws IOException {
        Path main = write("main.tys", "print(1);\nimport x from \"nope\";");
        assertThrows(TinyscriptSyntaxError.class, () -> runModule(main));   // imports only at the top
        Path main2 = write("main2.tys", "import x from \"nope\";\nprint(x);");
        TinyscriptRuntimeException e = assertThrows(TinyscriptReferenceError.class, () -> runModule(main2, dir.resolve("mods")));
        assertTrue(e.getMessage().contains("Module \"nope\" not found (searched: "), e.getMessage());
        assertTrue(e.getMessage().contains("nope.tys"), e.getMessage());
    }

    @Test
    public void testRelativeImportsOfNonFilesResolveAgainstTheWorkingDirectory() throws IOException {
        // e.g. a line typed in the interactive REPL, whose resource is not a file
        Path module = write("m.tys", "export let x = 1;");
        Path relative = Path.of("").toAbsolutePath().relativize(module);
        URI resolved = new FileSystemModuleLoader(List.of()).resolve("./" + relative, URI.createURI("__repl0.tys"));
        assertEquals(module.toRealPath().toString(), resolved.toFileString());
    }

    @Test
    public void testOnlyTysFilesAreModules() throws IOException {
        write("m.js", "export let x = 1;");
        Path main = write("main.tys", "import x from \"./m.js\";\nprint(x);");
        TinyscriptRuntimeException e = assertThrows(TinyscriptReferenceError.class, () -> runModule(main));
        assertTrue(e.getMessage().contains("not a tinyscript module"), e.getMessage());
    }

    // =========================================================================
    // The import graph: each module runs once
    // =========================================================================

    @Test
    public void testModuleRunsOnceInADiamond() throws IOException {
        write("m1.tys", "print(\"m1 runs\");\nexport let id1 = {};");
        write("m2.tys", "import id1 from \"./m1\";\nexport function same(x) { return x == id1; }");
        Path main = write("main.tys", "import id1 from \"./m1\";\nimport same from \"./m2\";\nprint(same(id1));");
        assertEquals("m1 runs\ntrue\n", runModule(main));
    }

    @Test
    public void testASymlinkIsTheSameModule() throws IOException {
        write("m1.tys", "print(\"m1 runs\");\nexport let id1 = {};");
        Files.createSymbolicLink(dir.resolve("alias.tys"), dir.resolve("m1.tys"));
        Path main = write("main.tys", "import * as a from \"./m1\";\nimport * as b from \"./alias\";\nprint(a == b);");
        assertEquals("m1 runs\ntrue\n", runModule(main));
    }

    @Test
    public void testCyclicImportsAreAnError() throws IOException {
        write("a.tys", "import fb from \"./b\";\nexport function fa() { return 1; }");
        write("b.tys", "import fa from \"./a\";\nexport function fb() { return 2; }");
        Path main = write("main.tys", "import fa from \"./a\";\nprint(fa());");
        TinyscriptRuntimeException e = assertThrows(TinyscriptReferenceError.class, () -> runModule(main));
        assertTrue(e.getMessage().contains("Cyclic import: main.tys → a.tys → b.tys → a.tys"), e.getMessage());
    }

    // =========================================================================
    // Modules are isolated; exports are read-only
    // =========================================================================

    @Test
    public void testModuleStateViaClosures() throws IOException {
        write("counter.tys", "let count = 0;\nexport function next() { count = count + 1; return count; }");
        Path main = write("main.tys", "import next from \"./counter\";\nnext();\nprint(next());");
        assertEquals("2\n", runModule(main));
    }

    @Test
    public void testExportsAreReadOnly() throws IOException {
        Path main = write("main.tys", "import * as geo from \"./geometry\";\ngeo.unit = \"m\";");
        assertThrows(TinyscriptTypeError.class, () -> runModule(main));
        Path increment = write("increment.tys", "import * as geo from \"./geometry\";\ngeo.unit += \"m\";");
        assertThrows(TinyscriptTypeError.class, () -> runModule(increment));
    }

    @Test
    public void testModuleSeesOnlyTheBuiltins() throws IOException {
        write("m.tys", "export function f() { return secret; }");
        Path main = write("main.tys", "import f from \"./m\";\nlet secret = 1;\nprint(f());");
        TinyscriptRuntimeException e = assertThrows(TinyscriptSyntaxError.class, () -> runModule(main));
        assertTrue(e.getMessage().contains("secret"), e.getMessage());
    }

    @Test
    public void testSyntaxErrorInAModuleNamesTheModule() throws IOException {
        write("bad.tys", "print(1);\nlet ;");
        Path main = write("main.tys", "import x from \"./bad\";\nprint(x);");
        TinyscriptRuntimeException e = assertThrows(TinyscriptSyntaxError.class, () -> runModule(main));
        assertTrue(e.getMessage().contains("bad.tys:2"), e.getMessage());
    }

    @Test
    public void testImportWithoutModuleLoaderIsAnError() {
        // e.g. in the sandboxed webdemo
        assertInterpreterThrows(TinyscriptUnsupportedOperationError.class, "import x from \"./m\";\nprint(x);");
    }

    // =========================================================================
    // Standard library (std:…), loaded from the classpath
    // =========================================================================

    @Test
    public void testStdArrayWorksOnEverythingTheForLoopGoesThrough() throws IOException {
        Path main = write("main.tys", "import sum, contains, reverse from \"std:array\";\n"
                + "const s = new Set([1, 2]); const m = new Map([[\"a\", 1]]); const o = { x: 1 };\n"
                + "print(contains(range(10), 3), contains(range(10), 10), contains(s, 1), contains(s, \"1\"));\n"
                + "print(contains(\"abc\", \"b\"), contains(\"abc\", \"bc\"), contains(\"\", \"\"), contains([], 1), contains([o], o), contains([{ x: 1 }], o));\n"
                + "print(contains(m, \"a\"), contains(m.keys(), \"a\"), contains(m.values(), 1));\n"
                + "print(reverse(range(4)).join(), reverse(\"abc\").join(\"\"), reverse(s).join(), reverse([]).length, reverse(\"\").length);\n"
                + "for (const [k, v] : reverse(new Map([[\"a\", 1], [\"b\", 2]]))) { print(k, v); }\n"
                + "const a = [1, 2, 3]; const r = reverse(a); print(r.join(), a.join(), r == a);");
        assertEquals("true false true false\n"
                + "true false false false true false\n"
                + "false true true\n"
                + "3,2,1,0 cba 2,1 0 0\n"
                + "b 2\na 1\n"
                + "3,2,1 1,2,3 false\n", runModule(main));
        assertSameOutputModule(main);
    }

    @Test
    @Timeout(10)
    public void testStdArrayReverseIsLinear() throws IOException {
        // With unshift in a loop, reverse was quadratic: 200,000 elements took 2.2 s, a million about a minute
        Path main = write("main.tys", "import reverse from \"std:array\";\n"
                + "const r = reverse(range(1000000).toArray()); print(r.length, r[0], r[999999]);");
        assertEquals("1000000 999999 0\n", runModule(main));
    }

    @Test
    public void testStandardLibrary() throws IOException {
        Path main = write("main.tys", "import sum, contains, reverse from \"std:array\";\n"
                + "print(range(5).toArray().join(\",\")); print(range(0).length); print(sum(range(5)));\n"
                + "print(contains([1, 2], 2)); print(contains([1, 2], 3)); print(reverse([1, 2, 3]).join(\",\"));");
        assertEquals("0,1,2,3,4\n0\n10\ntrue\nfalse\n3,2,1\n", runModule(main));
        assertSameOutputModule(main);
    }

    @Test
    public void testStandardLibraryModulesAreLoadedOnce() throws IOException {
        Path main = write("main.tys", "import * as a from \"std:array\";\nimport * as b from \"std:array\";\nprint(a == b);");
        assertEquals("true\n", runModule(main));
    }

    @Test
    public void testUnknownStandardLibraryModule() throws IOException {
        Path main = write("main.tys", "import x from \"std:nope\";\nprint(x);");
        TinyscriptRuntimeException e = assertThrows(TinyscriptReferenceError.class, () -> runModule(main));
        assertTrue(e.getMessage().contains("Standard library module \"std:nope\" not found"), e.getMessage());
        // Math is a global (as in Javascript), not a module
        Path main2 = write("main2.tys", "import * as math from \"std:math\";\nprint(math);");
        assertThrows(TinyscriptReferenceError.class, () -> runModule(main2));
    }

    @Test
    public void testStandardLibraryOnlyLoaderRejectsFiles() {
        // The webdemo's loader
        TinyscriptRuntimeException e = assertThrows(TinyscriptReferenceError.class,
                () -> new StandardLibraryModuleLoader().resolve("./m", URI.createURI("__webdemo0.tys")));
        assertTrue(e.getMessage().contains("Only standard library modules (std:…) are available here"), e.getMessage());
    }

    // =========================================================================
    // Javascript generator: bundles the modules into one program
    // =========================================================================

    @Test
    public void testTranspiledNamedAndNamespaceImports() throws IOException {
        Path main = write("main.tys", "import area, unit from \"./geometry\";\nimport * as geo from \"./geometry.tys\";\n"
                + "let helper = \"main\";\nprint(area(2) + unit); print(geo.area(1)); print(geo.helper == null); print(helper);");
        assertEquals("12cm\n3\ntrue\nmain\n", runModule(main));
        assertSameOutputModule(main);
    }

    @Test
    public void testTranspiledModuleRunsOnceInADiamond() throws IOException {
        write("m1.tys", "print(\"m1 runs\");\nexport let id1 = {};");
        write("m2.tys", "import id1 from \"./m1\";\nexport function same(x) { return x == id1; }");
        Path main = write("main.tys", "import id1 from \"./m1\";\nimport same from \"./m2\";\nprint(same(id1));");
        assertSameOutputModule(main);
    }

    @Test
    public void testTranspiledModuleStateViaClosures() throws IOException {
        write("counter.tys", "let count = 0;\nexport function next() { count = count + 1; return count; }");
        Path main = write("main.tys", "import next from \"./counter\";\nnext();\nprint(next());");
        assertSameOutputModule(main);
    }

    @Test
    public void testTranspiledRelativeAndBarePaths() throws IOException {
        write("util.tys", "export function twice(x) { return 2 * x; }");
        write("lib/vector.tys", "import twice from \"../util\";\nexport function scale(x) { return twice(x); }");
        write("mods/half.tys", "export function half(x) { return x / 2; }");
        Path main = write("app/main.tys", "import scale from \"../lib/vector\";\nimport half from \"half\";\nprint(half(scale(21)));");
        assertSameOutputModule(main, dir.resolve("mods"));
    }

    @Test
    public void testTranspilingACycleIsAnError() throws IOException {
        write("a.tys", "import fb from \"./b\";\nexport function fa() { return 1; }");
        write("b.tys", "import fa from \"./a\";\nexport function fb() { return 2; }");
        Path main = write("main.tys", "import fa from \"./a\";\nprint(fa());");
        TinyscriptRuntimeException e = assertThrows(TinyscriptReferenceError.class, () -> transpileModule(main));
        assertTrue(e.getMessage().contains("Cyclic import: main.tys → a.tys → b.tys → a.tys"), e.getMessage());
    }

    @Test
    public void testTranspilingAMissingExportIsAnError() throws IOException {
        Path main = write("main.tys", "import helper from \"./geometry\";\nprint(helper);");
        TinyscriptRuntimeException e = assertThrows(TinyscriptReferenceError.class, () -> transpileModule(main));
        assertTrue(e.getMessage().contains("\"geometry.tys\" does not export \"helper\""), e.getMessage());
    }

    @Test
    public void testTranspilingImportsWithoutModuleLoaderIsAnError() {
        // e.g. the webdemo's cross compiler
        TinyscriptRuntimeException e = assertThrows(TinyscriptUnsupportedOperationError.class,
                () -> transpile(parse("import x from \"./m\";\nprint(x);")));
        assertTrue(e.getMessage().contains("Modules are not available here"), e.getMessage());
    }

    // =========================================================================
    // Validation
    // =========================================================================

    @Test
    public void testValidImportsAndExportsAreAccepted() {
        // Guards the rejections below: they must come from the rules, not from a syntax error
        parse("import a, b from \"./m\";\nimport * as m from \"./m2\";\nprint(a, b, m.x);\nlet c = a; c = 2;");
        parse("export let x = 1, y;\nexport function f() { return x; }\nlet privat = 2;");
    }

    @Test
    public void testImportedNamesAreReadOnly() {
        assertRejected("import a from \"./m\";\na = 1;");
        assertRejected("import * as m from \"./m\";\nm = 1;");
        assertRejected("import a from \"./m\";\nfor (a : [1]) { }");
        assertRejected("import a from \"./m\";\na += 1;");
        assertRejected("import a from \"./m\";\na++;");
    }

    @Test
    public void testDuplicateImportedNamesAreRejected() {
        assertRejected("import a from \"./m1\";\nimport a from \"./m2\";");
        assertRejected("import a, a from \"./m\";");
        assertRejected("import a from \"./m\";\nlet a = 1;");
        assertRejected("import * as a from \"./m\";\nfunction a() { }");
    }

    @Test
    public void testExportOnlyAtTopLevel() {
        assertRejected("function f() { export let x = 1; }");
        assertRejected("if (true) { export function g() { } }");
    }
}
