package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptReferenceError;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * class without inheritance: a constructor function with methods on its prototype, as in
 * Javascript; only at the top level, only with new.
 */
public class ClassTest extends TinyscriptTranspilerTestBase {

    @TempDir
    Path dir;

    private static final String POINT = "class Point {\n"
            + "  constructor(x, y) {\n    this.x = x;\n    this.y = y;\n  }\n"
            + "  plus(other) { return new Point(this.x + other.x, this.y + other.y); }\n"
            + "  length() { return Math.hypot(this.x, this.y); }\n"
            + "  toString() { return \"(\" + this.x + \", \" + this.y + \")\"; }\n"
            + "}\n";

    @Test
    public void testConstructorAndMethods() {
        String code = POINT
                + "const p = new Point(3, 4);\n"
                + "print(p, p.length(), p.plus(new Point(1, 1)), [p, p]);\n"
                + "print(p instanceof Point, typeof Point, typeof p.plus, Object.keys(p).join());";
        assertEquals("(3, 4) 5 (4, 5) [(3, 4), (3, 4)]\ntrue function function x,y\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testPrintingClassesAndInstances() {
        // Without toString: the class name and the fields, as node shows them; methods are not enumerable
        String code = "class Num {\n  constructor(value) { this.value = value; }\n  twice() { return this.value * 2; }\n}\n"
                + "class Empty { }\n"
                + "const n = new Num(3);\n"
                + "print(n, [n], { n }, new Empty(), Num, [Empty]);\n"
                + "print(\"\" + n, Object.keys(Num.prototype).length, n.twice());\n"
                + "function Old(value) { this.value = value; }\n"
                + "print(new Old(1));";
        assertEquals("Num {value: 3} [Num {value: 3}] {n: Num {value: 3}} Empty {} [class Num] [[class Empty]]\n"
                + "Num {value: 3} 0 6\n"
                + "{value: 1}\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testParametersAndMethodNames() {
        String code = "class Bag {\n"
                + "  constructor(name, ...items) { this.name = name; this.items = items; }\n"
                + "  add([a, b]) { this.items.push(a + b); return this; }\n"
                + "  delete() { this.items = []; }\n"
                + "  \"size of\"() { return this.items.length; }\n"
                + "  sum() { return this.items.reduce((s, x) => s + x, 0); }\n"
                + "}\n"
                + "const bag = new Bag(\"b\", 1, 2).add([3, 4]);\n"
                + "print(bag.name, bag.items, bag.sum(), bag[\"size of\"]());\n"
                + "bag.delete(); print(bag.items);";
        assertEquals("b [1, 2, 7] 10 3\n[]\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testClassesAndFunctionsCanMentionLaterClasses() {
        // Methods and functions run later, when the classes exist
        String code = "function make(n) { return n == 0 ? new Leaf() : new Node(make(n - 1)); }\n"
                + "class Node {\n  constructor(child) { this.child = child; }\n  depth() { return 1 + this.child.depth(); }\n}\n"
                + "class Leaf {\n  depth() { return 0; }\n  wrap() { return new Node(this); }\n}\n"
                + "print(make(3).depth(), new Leaf().wrap().depth());";
        assertEquals("3 1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testAClassWithoutConstructor() {
        String code = "class Counter {\n  inc() { this.n = (this.n ?? 0) + 1; return this; }\n}\n"
                + "const c = new Counter(); c.inc().inc();\n"
                + "print(c.n, new Counter().n, Counter.length);";
        assertEquals("2 undefined 0\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testExportAndImportAClass() throws IOException {
        Files.writeString(dir.resolve("shapes.tys"), "export class Circle {\n"
                + "  constructor(r) { this.r = r; }\n  area() { return 3 * this.r * this.r; }\n}\n"
                + "class Hidden { }\n");
        Path main = Files.writeString(dir.resolve("main.tys"), "import Circle from \"./shapes\";\n"
                + "import * as shapes from \"./shapes\";\n"
                + "const c = new Circle(2);\n"
                + "print(c.area(), c instanceof shapes.Circle, Object.keys(shapes).join(), new shapes.Circle(1));");
        assertEquals("12 true Circle Circle {r: 1}\n", runModule(main));
        assertSameOutputModule(main);
    }

    @Test
    public void testAClassNeedsNew() {
        TinyscriptRuntimeException e = assertThrows(TinyscriptTypeError.class,
                () -> interpreterOutput(POINT + "const p = Point(1, 2);"));
        assertEquals("Point is a class: use new Point(…)", e.getMessage());
        assertSameFailure(TinyscriptTypeError.class, POINT + "print([1, 2].map(Point));");
    }

    @Test
    public void testAMethodTakenOffItsObjectHasNoThis() {
        assertSameFailure(TinyscriptTypeError.class, POINT + "const f = new Point(1, 2).length;\nprint(f());");
    }

    @Test
    public void testAFunctionCalledBeforeTheClassStatement() {
        TinyscriptRuntimeException e = assertThrows(TinyscriptReferenceError.class,
                () -> interpreterOutput("function make() { return new Late(); }\nprint(make());\nclass Late { }"));
        assertEquals("Late is used before its declaration (line 3)", e.getMessage());
        assertEquals(1, e.getAffectedLine());
    }

    @Test
    public void testTheStackTraceNamesClassAndMethod() {
        TinyscriptRuntimeException e = assertThrows(TinyscriptTypeError.class, () -> interpreterOutput(
                "class Box {\n  constructor(v) { this.v = v; this.check(); }\n  check() {\n    const z = null;\n    return z.x;\n  }\n}\nnew Box(1);"));
        String trace = e.getTinyscriptStacktraceAsString();
        assertTrue(trace.contains("at Box.check(") && trace.contains("at Box("), trace);
    }

    @Test
    public void testCompileTimeErrors() {
        assertEquals(List.of("2:9: A class can only be declared at the top level of a file, not in a block or function"),
                reportedErrors("function f() {\n  class Inner { }\n}"));
        assertEquals(List.of("3:3: Duplicate method 'a' in class C"), reportedErrors("class C {\n  a() { }\n  a(x) { }\n}"));
        assertEquals(List.of("3:3: A class has only one constructor"),
                reportedErrors("class C {\n  constructor() { }\n  constructor(x) { }\n}"));
        assertEquals(List.of("2:26: A constructor cannot return a value: new C(…) always gives the new object"),
                reportedErrors("class C {\n  constructor() { return 1; }\n}"));
        assertEquals(List.of("2:1: Cannot assign to the class C"), reportedErrors("class C { }\nC = 1;"));
        assertEquals(List.of("1:10: Duplicate identifier", "2:7: Duplicate identifier"), reportedErrors("function C() { }\nclass C { }"));
        assertEquals(List.of("1:15: C is used before its declaration (line 2)"), reportedErrors("const c = new C();\nclass C { }"));
        // A plain return in a constructor is fine, as is a class name as property name
        assertEquals(List.of(), reportedErrors("class C {\n  constructor(x) { if (x == null) { return; } this.x = x; }\n}\nconst o = { class: 1 };"));
    }

    @Test
    public void testUsedBeforeItsDeclarationAlsoForVariables() {
        assertEquals(List.of("1:7: limit is used before its declaration (line 2)"), reportedErrors("print(limit);\nconst limit = 3;"));
        // At runtime, as in Javascript: a function that runs before the statement
        TinyscriptRuntimeException e = assertThrows(TinyscriptReferenceError.class,
                () -> interpreterOutput("function f() { return limit; }\nprint(f());\nconst limit = 3;"));
        assertEquals("limit is used before its declaration (line 3)", e.getMessage());
        assertSameFailure(TinyscriptReferenceError.class,
                "function g() {\n  const read = () => [a, b];\n  const r = read();\n  let [a, b] = [1, 2];\n  return r;\n}\ng();");
        // Declared, but without a value, is fine; and so is a function that runs after the statement
        String code = "function h() { let x; return x; }\nfunction later() { return limit * 2; }\nconst limit = 3;\nprint(h(), later());";
        assertEquals("undefined 6\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testHintsForJavascriptClassFeatures() {
        assertEquals("Classes in tinyscript have no inheritance (no extends): share code with functions",
                onlyError("class A { }\nclass B extends A { }"));
        assertEquals("Classes in tinyscript have no static members: write a function or a constant next to the class",
                onlyError("class A {\n  static make() { return new A(); }\n}"));
        assertEquals("Classes in tinyscript have no getters and setters: write a method, e.g. area() { … }",
                onlyError("class A {\n  get area() { return 1; }\n}"));
        assertEquals("A class has no field declarations: fields get their values in the constructor, this.count = …;",
                onlyError("class A {\n  count = 0;\n}"));
    }

    @Test
    public void testSyntaxErrorsInsideAClassAreReportedWhereTheyAre() {
        assertEquals(List.of("4:5: Missing ';' at the end of line 3"),
                reportedErrors("class A {\n  f() {\n    let x = 1\n    return x;\n  }\n}"));
    }

    @Test
    public void testAClassExpressionIsASyntaxErrorAndNoCrash() {
        // Used to crash the validation (a declaration without name after the syntax error)
        List<String> errors = reportedErrors("print(Math.PI);\nconst Q = class { };");
        assertEquals("2:11: A class is declared as a statement at the top level of a file (class Q { … }), not used as a value",
                errors.get(0), errors.toString());
        errors = reportedErrors("const Q = class { };\nprint(limit);\nconst limit = 1;");
        assertTrue(!errors.isEmpty(), errors.toString());
    }

    private String onlyError(String code) {
        List<String> errors = reportedErrors(code);
        assertEquals(1, errors.size(), errors.toString());
        return errors.get(0).replaceFirst("^\\d+:\\d+: ", "");
    }
}
