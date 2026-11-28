package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Library functions: Math,
 * arrays, Object.values / entries / fromEntries / assign, parseFloat, Number.isFinite, strings.
 * As in Javascript, so each test also runs against node.
 */
public class LibraryTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testMath() {
        String code = "print(Math.pow(2, 10), Math.pow(2, 0.5), Math.pow(2, -1), Math.pow(-8, 1 / 3), Math.pow(1, 1 / 0), Math.pow(0, 0));\n"
                + "print(Math.trunc(2.7), Math.trunc(-2.7), Math.trunc(-0.5), Math.sign(-3), Math.sign(0), Math.sign(2.5), Math.sign(0 / 0));\n"
                + "print(Math.E, Math.log(Math.E), Math.log2(8), Math.log2(1024), Math.log2(10), Math.log10(1000), Math.log10(2));\n"
                + "print(Math.cbrt(27), Math.cbrt(-8), Math.hypot(3, 4), Math.hypot(), Math.hypot(1e200, 1e200), Math.hypot(1, 1 / 0, 0 / 0));\n"
                + "print(Math.sin(0), Math.cos(0), Math.sin(Math.PI / 2), Math.tan(Math.PI / 4), Math.atan2(1, 1) * 4 == Math.PI);\n"
                + "print(Math.asin(1), Math.acos(1), Math.atan(1), Math.sin(1), Math.cos(1), Math.exp(2), Math.log(10));\n"
                + "print(Math.round(2.5), Math.round(-2.5), Math.round(2.4), Math.round(-0.4), Math.round(1e20), Math.round(0 / 0), Math.round(0.49999999999999994));";
        assertSameOutput("1024 1.4142135623730951 0.5 NaN NaN 1\n"
                + "2 -2 0 -1 0 1 NaN\n"
                + "2.718281828459045 1 3 10 3.321928094887362 3 0.3010299956639812\n"
                + "3 -2 5 0 1.414213562373095e+200 Infinity\n"
                + "0 1 1 0.9999999999999999 true\n"
                + "1.5707963267948966 0 0.7853981633974483 0.8414709848078965 0.5403023058681398 7.38905609893065 2.302585092994046\n"
                + "3 -2 2 0 100000000000000000000 NaN 0\n", code);
    }

    @Test
    public void testMathChecksItsArguments() {
        assertSameFailure(TinyscriptTypeError.class, "const s = \"2\";\nMath.pow(s, 3);");
        assertSameFailure(TinyscriptTypeError.class, "const s = \"4\";\nMath.hypot(3, s);");
        assertEquals(java.util.List.of("1:1: pow() needs 2 arguments, not 1"), reportedErrors("Math.pow(2);"));
    }

    @Test
    public void testReverseAndToReversed() {
        String code = "const a = [1, 2, 3]; const r = a.reverse(); print(a, r == a);\n"
                + "const b = [\"x\", \"y\"]; const c = b.toReversed(); print(b, c, c == b, [].toReversed(), [].reverse());";
        assertSameOutput("[3, 2, 1] true\n[\"x\", \"y\"] [\"y\", \"x\"] false [] []\n", code);
    }

    @Test
    public void testSplice() {
        String code = "let a = [1, 2, 3, 4, 5]; print(a.splice(1, 2), a);\n"
                + "a = [1, 2, 3]; print(a.splice(1, 0, \"x\", \"y\"), a);\n"
                + "a = [1, 2, 3, 4]; print(a.splice(-2), a);\n"
                + "a = [1, 2, 3]; print(a.splice(1), a, a.splice(), a.splice(0, 99), a);\n"
                + "a = [1, 2, 3]; print(a.splice(1, -1, 9), a, a.splice(10, 1, 0), a);";
        // print evaluates all its arguments first, so a shows its state after the last splice
        assertSameOutput("[2, 3] [1, 4, 5]\n"
                + "[] [1, \"x\", \"y\", 2, 3]\n"
                + "[3, 4] [1, 2]\n"
                + "[2, 3] [] [] [1] []\n"
                + "[] [1, 9, 2, 3, 0] [] [1, 9, 2, 3, 0]\n", code);
        assertSameFailure(TinyscriptTypeError.class, "[1].splice(0.5);");
        assertSameFailure(TinyscriptTypeError.class, "[1].splice(0, \"1\");");
    }

    @Test
    public void testSearchingFromTheEnd() {
        String code = "const a = [1, 2, 3, 2, 1];\n"
                + "print(a.lastIndexOf(2), a.lastIndexOf(2, 2), a.lastIndexOf(2, -3), a.lastIndexOf(9), a.lastIndexOf(1, -9), [].lastIndexOf(1));\n"
                + "print(a.findLast(x => x > 1), a.findLastIndex(x => x > 1), a.findLast(x => x > 5), a.findLastIndex(x => x > 5));";
        assertSameOutput("3 1 1 -1 -1 -1\n2 3 undefined -1\n", code);
    }

    @Test
    public void testArrayEntries() {
        // Like the entries of a Map: an array of [index, element] pairs (Javascript: an iterator)
        String code = "const a = [\"x\", \"y\"];\n"
                + "for (const [i, x] : a.entries()) { print(i, x); }\n"
                + "print(a.entries(), [].entries(), a.entries().length, [...a.entries()][1], a.entries().map(([i, x]) => x + i));\n"
                + "const e = a.entries(); a.push(\"z\"); print(e.length);";
        assertSameOutput("0 x\n1 y\n[[0, \"x\"], [1, \"y\"]] [] 2 [1, \"y\"] [\"x0\", \"y1\"]\n2\n", code);
    }

    @Test
    public void testFlatAndFlatMap() {
        String code = "const a = [1, [2, [3, [4]]], 5];\n"
                + "print(a.flat(), a.flat(2), a.flat(1 / 0), a.flat(0), [].flat());\n"
                + "print([\"a b\", \"c\"].flatMap(s => s.split(\" \")), [1, 2].flatMap(x => [x, x * 10]), [1, 2].flatMap(x => x), [[1], 2].flatMap(x => [x]));";
        assertSameOutput("[1, 2, [3, [4]], 5] [1, 2, 3, [4], 5] [1, 2, 3, 4, 5] [1, [2, [3, [4]]], 5] []\n"
                + "[\"a\", \"b\", \"c\"] [1, 10, 2, 20] [1, 2] [[1], 2]\n", code);
        assertSameFailure(TinyscriptTypeError.class, "[1].flat(-1);");
        assertSameFailure(TinyscriptTypeError.class, "[1].flat(1.5);");
    }

    @Test
    public void testObjectValuesEntriesFromEntriesAssign() {
        String code = "const o = { a: 1, b: \"x\" };\n"
                + "print(Object.values(o), Object.entries(o), Object.values([7, 8]), Object.entries({}));\n"
                + "for (const [key, value] : Object.entries(o)) { print(key + \"=\" + value); }\n"
                + "print(Object.fromEntries([[\"a\", 1], [\"b\", [2]], [3, \"three\"]]), Object.fromEntries(new Map([[\"m\", 1]])), Object.fromEntries([]));\n"
                + "const target = { a: 0, z: 9 }; const r = Object.assign(target, { a: 1, b: 2 }, null, { c: 3 }); print(target, r == target);\n"
                + "print(Object.fromEntries(Object.entries(o).map(([k, v]) => [k + k, v])));";
        assertSameOutput("[1, \"x\"] [[\"a\", 1], [\"b\", \"x\"]] [7, 8] []\n"
                + "a=1\nb=x\n"
                + "{3: \"three\", a: 1, b: [2]} {m: 1} {}\n"
                + "{a: 1, z: 9, b: 2, c: 3} true\n"
                + "{aa: 1, bb: \"x\"}\n", code);
        assertSameFailure(TinyscriptTypeError.class, "Object.values(5);");
        assertSameFailure(TinyscriptTypeError.class, "Object.fromEntries([[\"a\"]]);");
        assertSameFailure(TinyscriptTypeError.class, "Object.fromEntries([[true, 1]]);");
        assertSameFailure(TinyscriptTypeError.class, "Object.assign(null, {});");
    }

    @Test
    public void testObjectFunctionsLikeNode() {
        // Javascript puts integer keys first ({3: …, a: …}); tinyscript keeps the insertion order,
        // so this compares only objects with string keys
        assertSameOutput("const o = { a: 1, b: \"x\" };\n"
                + "print(Object.values(o), Object.entries(o), Object.fromEntries([[\"k\", 1]]), Object.assign({ a: 0 }, { b: 2 }));");
    }

    @Test
    public void testNumbers() {
        String code = "print(parseFloat(\"3.5kg\"), parseFloat(\"  -1e3 \"), parseFloat(\".5\"), parseFloat(\"1e\"), parseFloat(\"Infinity!\"), parseFloat(\"x1\"), parseFloat(\"\"));\n"
                + "print(Number.isFinite(1), Number.isFinite(1 / 0), Number.isFinite(0 / 0), Number.isFinite(\"1\"));\n"
                + "print(Number.MAX_SAFE_INTEGER, Number.MIN_SAFE_INTEGER, Number.MAX_SAFE_INTEGER + 2);\n"
                + "print(Number.MAX_VALUE, Number.MIN_VALUE, Number.EPSILON, Number.MAX_VALUE * 2, 1 + Number.EPSILON > 1, Number.MIN_VALUE / 2);";
        assertSameOutput("3.5 -1000 0.5 1 Infinity NaN NaN\n"
                + "true false false false\n"
                + "9007199254740991 -9007199254740991 9007199254740992\n"
                + "1.7976931348623157e+308 5e-324 2.220446049250313e-16 Infinity true 0\n", code);
        assertSameFailure(TinyscriptTypeError.class, "parseFloat(3.5);");
    }

    @Test
    public void testStrings() {
        String code = "const s = \"  a b  \"; print(\"[\" + s.trimStart() + \"]\", \"[\" + s.trimEnd() + \"]\", \"[\" + s.trim() + \"]\");\n"
                + "print(\"A\".codePointAt(0), \"😀\".codePointAt(0), \"😀\".length, \"ab\".codePointAt(5), \"äb\".codePointAt(1));";
        assertSameOutput("[a b  ] [  a b] [a b]\n65 128512 2 undefined 98\n", code);
    }

    @Test
    public void testDelete() {
        String code = "const o = { a: 1, b: 2, c: 3 }; const key = \"c\";\n"
                + "print(delete o.a, o, Object.keys(o), o.a);\n"
                + "delete o[key]; delete o.missing; print(o);\n"
                + "const m = new Map([[\"k\", 1]]); print(m.delete(\"k\"), m.size);\n"
                + "function Car() { this.wheels = 4; } const car = new Car(); delete car.wheels; print(car.wheels, typeof Car.prototype);";
        assertSameOutput("true {b: 2, c: 3} [\"b\", \"c\"] undefined\n"
                + "{b: 2}\n"
                + "true 0\n"
                + "undefined object\n", code);
    }

    @Test
    public void testDeleteOnlyRemovesPropertiesOfObjects() {
        assertSameFailure(TinyscriptTypeError.class, "const a = [1, 2]; delete a[0];");
        assertSameFailure(TinyscriptTypeError.class, "const m = new Map(); delete m.size;");
        assertSameFailure(TinyscriptTypeError.class, "const s = \"abc\"; delete s.length;");
        assertSameFailure(TinyscriptTypeError.class, "const m = Math; delete m.PI;");
        assertSameFailure(TinyscriptTypeError.class, "function f() { } delete f.prototype;");
        assertSameFailure(TinyscriptTypeError.class, "let u; delete u.x;");
        assertKnownGap(TinyscriptTypeError.class, "const n = 5; delete n.x;");
    }
}
