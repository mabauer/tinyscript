package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Interfaces: interface Shape { area(); } lists the methods that a class with implements Shape
 * must have. A class of the same file is checked before the run; one that implements an
 * imported interface when it is declared.
 */
public class InterfaceTest extends TinyscriptTranspilerTestBase {

    @TempDir
    Path tempDir;

    private static final String SHAPES = "interface Shape {\n"
            + "    area();\n"
            + "    scale(factor);\n"
            + "}\n"
            + "interface Named { name(); }\n"
            + "class Square implements Shape, Named {\n"
            + "    constructor(side) { this.side = side; }\n"
            + "    area() { return this.side * this.side; }\n"
            + "    scale(factor) { return new Square(this.side * factor); }\n"
            + "    name() { return \"square\"; }\n"
            + "    perimeter() { return 4 * this.side; }\n"
            + "}\n"
            + "class Circle implements Shape {\n"
            + "    constructor(r) { this.r = r; }\n"
            + "    area() { return 3 * this.r * this.r; }\n"
            + "    scale(factor) { return new Circle(this.r * factor); }\n"
            + "}\n";

    @Test
    public void testAClassImplementsInterfaces() {
        String code = SHAPES
                + "for (const shape : [new Square(2), new Circle(1)]) { print(shape.scale(2).area()); }\n"
                + "print(Shape, typeof Shape, \"\" + Named, [Shape]);";
        assertEquals("16\n12\n[interface Shape] object [interface Named] [[interface Shape]]\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testAnInterfaceWithoutMethodsAndAClassWithout() {
        String code = "interface Marker { }\nclass Thing implements Marker { }\nprint(new Thing());";
        assertEquals("Thing {}\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    private String onlyError(String code) {
        List<String> errors = reportedErrors(code);
        assertEquals(1, errors.size(), errors.toString());
        return errors.get(0).replaceFirst("^\\d+:\\d+: ", "");
    }

    @Test
    public void testMissingMethodsBeforeTheRun() {
        assertEquals("Circle implements Shape but has no method scale",
                onlyError("interface Shape { area(); scale(factor); }\nclass Circle implements Shape {\n  area() { return 1; }\n}"));
        assertEquals("Circle implements Shape but has no methods area, scale",
                onlyError("interface Shape { area(); scale(factor); }\nclass Circle implements Shape { }"));
        // The constructor is no method
        assertEquals("Circle implements Shape but has no method area",
                onlyError("interface Shape { area(); }\nclass Circle implements Shape {\n  constructor() { this.area = 1; }\n}"));
    }

    @Test
    public void testOnlyInterfacesCanBeImplemented() {
        assertEquals("B implements A, but A is a class, not an interface", onlyError("class A { }\nclass B implements A { }"));
        assertEquals("B implements f, but f is a function, not an interface", onlyError("function f() { }\nclass B implements f { }"));
        assertEquals("B implements X, but X is a variable, not an interface", onlyError("const X = {};\nclass B implements X { }"));
        assertEquals("Unknown name 'Shape'", onlyError("class B implements Shape { }"));
        assertEquals("B implements I twice", onlyError("interface I { }\nclass B implements I, I { }"));
    }

    @Test
    public void testInterfaceDeclarations() {
        assertEquals("Duplicate method 'area' in interface Shape", onlyError("interface Shape { area(); area(); }"));
        assertEquals("An interface lists methods, not the constructor", onlyError("interface Shape { constructor(); }"));
        assertEquals("An interface can only be declared at the top level of a file, not in a block or function",
                onlyError("function f() {\n  interface I { }\n}"));
        assertEquals("Cannot assign to the interface Shape", onlyError("interface Shape { }\nShape = 1;"));
        assertEquals("Duplicate identifier", onlyError("const Shape = 1;\ninterface Shape { }"));
        assertSameFailure(TinyscriptTypeError.class, "interface Shape { }\nconst s = new Shape();");
    }

    @Test
    public void testParametersAreOnlyForTheReader() {
        String code = "interface Visitor { visit(node, ...path); leave(); }\n"
                + "class Printer implements Visitor { visit(node) { print(node); } leave() { } }\n"
                + "new Printer().visit(1);";
        assertEquals("1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    private Path write(String name, String code) throws IOException {
        return Files.writeString(tempDir.resolve(name), code);
    }

    @Test
    public void testAnImportedInterface() throws IOException {
        write("shapes.tys", "export interface Shape { area(); }\n");
        Path main = write("main.tys", "import Shape from \"./shapes\";\n"
                + "class Square implements Shape { constructor(s) { this.s = s; } area() { return this.s * this.s; } }\n"
                + "print(new Square(3).area(), Shape);");
        assertEquals("9 [interface Shape]\n", runModule(main));
        assertSameOutputModule(main);
    }

    @Test
    public void testAClassWithoutTheMethodsOfAnImportedInterfaceFailsWhenItIsDeclared() throws IOException {
        write("shapes.tys", "export interface Shape { area(); perimeter(); }\nexport class Box { }\n");
        Path main = write("main.tys", "import * as shapes from \"./shapes\";\nimport Shape, Box from \"./shapes\";\n"
                + "print(\"before\");\n"
                + "class Circle implements Shape { area() { return 1; } }\n"
                + "print(\"never\");");
        TinyscriptRuntimeException e = assertThrows(TinyscriptTypeError.class, () -> runModule(main));
        assertEquals("Circle implements Shape but has no method perimeter", e.getMessage());
        assertNodeFails("Circle implements Shape but has no method perimeter", main);
        Path other = write("other.tys", "import Box from \"./shapes\";\nclass C implements Box { }");
        e = assertThrows(TinyscriptTypeError.class, () -> runModule(other));
        assertEquals("C implements Box, but Box is a class, not an interface", e.getMessage());
        assertNodeFails("C implements Box, but Box is a class, not an interface", other);
    }

    // The generated Javascript fails in the same way, before the rest of the script runs
    private void assertNodeFails(String message, Path main) {
        assumeNodeAvailable();
        String jsCode = transpileModule(main);
        AssertionError e = assertThrows(AssertionError.class, () -> NodeJsRunner.run(jsCode));
        assertTrue(e.getMessage().contains("TypeError: " + message), e.getMessage());
        assertFalse(e.getMessage().contains("never"), e.getMessage());
    }
}
