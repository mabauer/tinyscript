package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Builtins cannot be replaced or extended (TODO.md, language review of 2026-11-28, item 1):
 * the builtin objects, functions and prototypes are frozen. Where the source shows the builtin,
 * changing it is an error before the run; otherwise a TypeError in both backends. Objects that
 * inherit from a builtin prototype still get properties of their own (o.toString = …,
 * e.name = …), unlike Javascript after Object.freeze (the "override mistake").
 */
public class FrozenBuiltinsTest extends TinyscriptTranspilerTestBase {

    private String onlyError(String code) {
        List<String> errors = reportedErrors(code);
        assertEquals(1, errors.size(), errors.toString());
        return errors.get(0);
    }

    @Test
    public void testChangingABuiltinIsAnErrorBeforeTheRun() {
        String hint = ": builtins are read-only (declare a function or a constant of your own instead)";
        assertEquals("1:1: Cannot change Math" + hint, onlyError("Math.TAU = 2 * Math.PI;"));
        assertEquals("1:1: Cannot change Math" + hint, onlyError("Math.sqrt = x => 0;"));
        assertEquals("1:1: Cannot change Math" + hint, onlyError("Math.PI += 1;"));
        assertEquals("1:1: Cannot change Math" + hint, onlyError("Math[\"TAU\"] = 1;"));
        assertEquals("1:1: Cannot change Math" + hint, onlyError("Math.sqrt.cache = 1;"));
        assertEquals("1:1: Cannot change Array" + hint, onlyError("Array.prototype.first = function () { return this[0]; };"));
        assertEquals("1:1: Cannot change String" + hint, onlyError("String.prototype.shout = function () { return this + \"!\"; };"));
        assertEquals("1:1: Cannot change Object" + hint, onlyError("Object.prototype.toString = function () { return \"o\"; };"));
        assertEquals("1:1: Cannot change JSON" + hint, onlyError("JSON.parse = text => null;"));
        assertEquals("1:1: Cannot change print" + hint, onlyError("print.prefix = \"> \";"));
        assertEquals("1:1: Cannot change System" + hint, onlyError("System.args = [];"));
        assertEquals("1:1: Cannot change Math" + hint, onlyError("Math.counter++;"));
        assertEquals("1:8: Cannot change Math" + hint, onlyError("delete Math.PI;"));
    }

    @Test
    public void testANewMemberOfABuiltinCannotBeRead() {
        // Before, Math.TAU could be read once the script assigned it somewhere
        assertEquals(List.of("1:1: Cannot change Math: builtins are read-only (declare a function or a constant of your own instead)",
                "2:7: Math has no member 'TAU'"), reportedErrors("Math.TAU = 2 * Math.PI;\nprint(Math.TAU);"));
        assertEquals(List.of(), reportedErrors("print(typeof Math.TAU);"));
    }

    @Test
    public void testChangingABuiltinIsATypeErrorAtRunTime() {
        assertEquals("Cannot set 'sqrt': builtins are read-only",
                assertSameFailure(TinyscriptTypeError.class, "const m = Math;\nm.sqrt = x => 0;").getMessage());
        assertEquals("Cannot set 'TAU': builtins are read-only",
                assertSameFailure(TinyscriptTypeError.class, "const m = Math;\nm.TAU = 6.28;").getMessage());
        assertEquals("Cannot set 'PI': builtins are read-only",
                assertSameFailure(TinyscriptTypeError.class, "const m = Math;\nm.PI = 3;").getMessage());
        assertSameFailure(TinyscriptTypeError.class, "const p = Object.getPrototypeOf([]);\np.first = 1;");
        assertSameFailure(TinyscriptTypeError.class, "const p = Object.getPrototypeOf([]);\np.push = x => 42;");
        assertSameFailure(TinyscriptTypeError.class, "const p = Object.getPrototypeOf({});\np.toString = () => \"o\";");
        assertSameFailure(TinyscriptTypeError.class, "const p = String.prototype;\np.shout = 1;");
        assertSameFailure(TinyscriptTypeError.class, "const p = Object.getPrototypeOf(new Error(\"e\"));\np.name = \"E\";");
        assertSameFailure(TinyscriptTypeError.class, "const p = Object.getPrototypeOf(x => x);\np.call = 1;");
        assertSameFailure(TinyscriptTypeError.class, "const p = Object.getPrototypeOf(new Map());\np.getOr = 1;");
        assertSameFailure(TinyscriptTypeError.class, "const f = Math.sqrt;\nf.cache = 1;");
        assertSameFailure(TinyscriptTypeError.class, "const s = System;\ns.args = [];");
        assertEquals("Cannot delete 'at': builtins are read-only",
                assertSameFailure(TinyscriptTypeError.class, "const p = Object.getPrototypeOf([]);\ndelete p.at;").getMessage());
        assertEquals("Cannot define 'TAU': builtins are read-only",
                assertSameFailure(TinyscriptTypeError.class, "Object.defineProperty(Math, \"TAU\", { value: 6.28 });").getMessage());
        assertSameFailure(TinyscriptTypeError.class, "Object.assign(Math, { TAU: 6.28 });");
    }

    @Test
    public void testOwnPropertiesOverTheFrozenPrototypesStayAllowed() {
        String code = "const o = { x: 1 };\n"
                + "o.toString = function () { return \"o\" + this.x; };\n"
                + "print(o, o.hasOwnProperty(\"toString\"), Object.keys(o));\n"
                + "const e = new Error(\"bad input\");\n"
                + "e.name = \"ParseError\";\n"
                + "print(e, e.name);\n"
                + "const empty = new Error();\n"
                + "empty.message = \"set later\";\n"
                + "print(empty.message, empty.hasOwnProperty(\"message\"));\n"
                + "function f() { }\n"
                + "f.toString = () => \"F\";\n"
                + "print(f);\n"
                + "const c = Object.create(Object.prototype);\n"
                + "c.constructor = \"mine\";\n"
                + "print(c.constructor, Object.assign({}, { toString: () => \"a\" }));\n"
                + "function Car(engine) { this.engine = engine; }\n"
                + "Car.prototype.toString = function () { return \"Car \" + this.engine; };\n"
                + "print(new Car(\"V8\"));\n"
                + "System.args.push(\"x\");\n"
                + "print(System.args.length > 0);";
        assertSameOutput("o1 true [\"x\", \"toString\"]\n"
                + "ParseError: bad input ParseError\n"
                + "set later true\n"
                + "F\n"
                + "mine a\n"
                + "Car V8\n"
                + "true\n", code);
    }

    @Test
    public void testADeclarationMayStillHideABuiltin() {
        assertSameOutput("s\n", "const JSON = { stringify(x) { return \"s\"; } };\nJSON.stringify = x => \"s\";\nprint(JSON.stringify(1));");
    }
}
