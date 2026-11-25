package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptArgumentError;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Characterization tests for the built-in objects and functions.
 *
 * <p>They pin down the current behavior of every builtin — including deliberate
 * deviations from JavaScript (e.g. {@code join()} without separator) — so that
 * refactorings of the runtime can be checked against them.
 */
public class BuiltinsTest extends TinyscriptTranspilerTestBase {

    // =========================================================================
    // Array
    // =========================================================================

    @Test
    public void testArrayIsArray() {
        assertSameOutput("true\nfalse\n", 
                "print(Array.isArray([1]));\n"
                + "print(Array.isArray(\"x\"));");
    }

    @Test
    public void testArrayPushPop() {
        assertSameOutput("3\n1,2,3\n3\n2\n", 
                "let a = [1];\n"
                + "print(a.push(2, 3));\n"
                + "print(a.join(\",\"));\n"
                + "print(a.pop());\n"
                + "print(a.length);");
    }

    @Test
    public void testArrayShiftUnshift() {
        String code = "let a = [2];\n"
                + "print(a.unshift(0, 1));\n"
                + "print(a.join(\",\"));\n"
                + "print(a.shift());\n"
                + "print(a.join(\",\"));";
        assertSameOutput("3\n0,1,2\n0\n1,2\n", code);
    }

    @Test
    public void testArrayMapFilter() {
        assertSameOutput("2,4,6\n2,3\n", 
                "print([1, 2, 3].map(x => x * 2).join(\",\"));\n"
                + "print([1, 2, 3].filter(x => x > 1).join(\",\"));");
    }

    @Test
    public void testArrayJoinAndToString() {
        assertSameOutput("1,2\n1-2\n[1, \"a\"]\n", 
                "print([1, 2].join());\n"
                + "print([1, 2].join(\"-\"));\n"
                + "print([1, \"a\"].toString());");
    }

    @Test
    public void testInvalidArrayLengthIsATypeError() {
        assertSameFailure(TinyscriptTypeError.class, "let a = []; a.length = -1;");
        assertSameFailure(TinyscriptTypeError.class, "let a = []; a.length = 1.5;");
        assertKnownGap(TinyscriptTypeError.class, "let a = []; a.length = \"2\";");
    }

    @Test
    public void testArraysHaveNoNamedProperties() {
        // Arrays are lists: writing anything but an integer index or length is a TypeError
        assertKnownGap(TinyscriptTypeError.class, "let a = []; a.x = 5;");
        assertKnownGap(TinyscriptTypeError.class, "let a = []; a[\"name\"] = 5;");
        assertKnownGap(TinyscriptTypeError.class, "let a = [1]; a[-1] = 5;");
        assertKnownGap(TinyscriptTypeError.class, "let a = [1]; a[1.5] = 5;");
        assertSameFailure(TinyscriptTypeError.class, "let a = []; Object.defineProperty(a, \"x\", { value: 1 });");
    }

    @Test
    public void testArrayAt() {
        String code = "let a = [1, 2, 3];\n"
                + "print(a.at(0)); print(a.at(-1)); print(a.at(-3));\n"
                + "print(a.at(3) == null); print(a.at(-4) == null); print([].at(-1) == null);";
        assertSameOutput("1\n3\n1\ntrue\ntrue\ntrue\n", code);
    }

    @Test
    public void testArrayIndexOf() {
        String code = "let o = {}; let a = [1, \"two\", o, 1, null];\n"
                + "print(a.indexOf(1)); print(a.indexOf(\"two\")); print(a.indexOf(o)); print(a.indexOf(null));\n"
                + "print(a.indexOf(\"1\")); print(a.indexOf({})); print([].indexOf(1));";
        assertSameOutput("0\n1\n2\n4\n-1\n-1\n-1\n", code);
    }

    @Test
    public void testArrayIndexOfWithStartIndex() {
        // Like Javascript: a negative start index counts from the end
        String code = "let a = [1, 2, 1, 2];\n"
                + "print(a.indexOf(1, 1)); print(a.indexOf(2, -1)); print(a.indexOf(2, -10)); print(a.indexOf(1, 4)); print(a.indexOf(1, 0));";
        assertSameOutput("2\n3\n1\n-1\n0\n", code);
    }

    @Test
    public void testIndexOfNeedsAnIntegerStartIndex() {
        assertSameFailure(TinyscriptTypeError.class, "[1].indexOf(1, 0.5);");
        assertSameFailure(TinyscriptTypeError.class, "\"a\".indexOf(\"a\", \"1\");");
    }

    @Test
    public void testArrayIndexOfUsesTinyscriptEquality() {
        // Unlike Javascript's ===: null == undefined, and NaN is never found
        assertSameOutput("0\n-1\n", 
                "let u; print([u].indexOf(null)); print([0 / 0].indexOf(0 / 0));");
        assertSameOutput("2 -1 2 0 true false 2 0 -1 4\n",
                "let u;\nconst a = [1, u, null, 4, u];\n"
                + "print([1, 2, null].indexOf(u), [1].indexOf(null), a.indexOf(null, 2), [null].lastIndexOf(u), a.includes(null, -1), [1].includes(u),\n"
                + "  a.lastIndexOf(null, -2), a.lastIndexOf(u, 0) + 1, a.lastIndexOf(null, -6), a.indexOf(u, -2));");
    }

    @Test
    public void testAtNeedsAnIntegerIndex() {
        assertSameFailure(TinyscriptTypeError.class, "[1].at(1.5);");
        assertSameFailure(TinyscriptTypeError.class, "[1].at(\"0\");");
        assertSameFailure(TinyscriptTypeError.class, "\"a\".at(0.5);");
        assertSameFailure(TinyscriptArgumentError.class, "const a = [1];\na.at();");
    }

    @Test
    public void testArrayMethodOnNonArrayIsATypeError() {
        assertSameFailure(TinyscriptTypeError.class,
                "let o = {};\no.push = [].push;\no.push(1);");
    }

    @Test
    public void testBuiltinCalledWithTooFewArgumentsIsAnArgumentError() {
        // Where the function is only known at run time ([1].push() is an error before the run)
        assertSameFailure(TinyscriptArgumentError.class, "const a = [1];\na.push();");
        assertSameFailure(TinyscriptArgumentError.class, "const sqrt = Math.sqrt;\nsqrt();");
    }

    // =========================================================================
    // String
    // =========================================================================

    @Test
    public void testStringCharAt() {
        assertSameOutput("b\ntrue\n", 
                "print(\"abc\".charAt(1));\n"
                + "print(\"abc\".charAt(5) == \"\");");
    }

    @Test
    public void testStringAt() {
        String code = "let s = \"abc\"; print(s.at(0)); print(s.at(-1)); print(s.at(3) == null); print(\"\".at(0) == null);";
        assertSameOutput("a\nc\ntrue\ntrue\n", code);
    }

    @Test
    public void testStringSubstring() {
        assertSameOutput("el\nel\nllo\nhe\n", 
                "print(\"hello\".substring(1, 3));\n"
                + "print(\"hello\".substring(3, 1));\n"
                + "print(\"hello\".substring(2));\n"
                + "print(\"hello\".substring(-1, 2));");
    }

    @Test
    public void testStringIndexOf() {
        assertSameOutput("2\n-1\n", 
                "print(\"hello\".indexOf(\"l\"));\n"
                + "print(\"hello\".indexOf(\"x\"));");
    }

    @Test
    public void testStringIndexOfWithStartIndex() {
        // Like Javascript: a negative position counts as 0
        String code = "let s = \"abcabc\";\n"
                + "print(s.indexOf(\"b\", 2)); print(s.indexOf(\"b\", -5)); print(s.indexOf(\"c\", 10)); print(s.indexOf(\"\", 10));";
        assertSameOutput("4\n1\n-1\n6\n", code);
    }

    @Test
    public void testStringToStringAndLength() {
        assertSameOutput("ab\n3\n", 
                "let s = new String(\"ab\");\n"
                + "print(s.toString());\n"
                + "print(\"abc\".length);");
    }

    // =========================================================================
    // Object
    // =========================================================================

    @Test
    public void testObjectCreateAndGetPrototypeOf() {
        assertSameOutput("1\ntrue\ntrue\n", 
                "let p = { x: 1 };\n"
                + "let o = Object.create(p);\n"
                + "print(o.x);\n"
                + "print(Object.getPrototypeOf(o) == p);\n"
                + "print(Object.getPrototypeOf(Object.create(null)) == null);");
    }

    @Test
    public void testObjectCreateWithNonObjectIsATypeError() {
        assertSameFailure(TinyscriptTypeError.class, "Object.create(5);");
    }

    @Test
    public void testObjectDefinePropertyKeysAndPropertyNames() {
        assertSameOutput("2\na\na,b\n", 
                "let o = { a: 1 };\n"
                + "Object.defineProperty(o, \"b\", { value: 2, enumerable: false });\n"
                + "print(o.b);\n"
                + "print(Object.keys(o).join(\",\"));\n"
                + "print(Object.getOwnPropertyNames(o).join(\",\"));");
    }

    @Test
    public void testObjectDefinePropertyOnNonObjectIsATypeError() {
        assertSameFailure(TinyscriptTypeError.class, "Object.defineProperty(1, \"a\", {});");
    }

    @Test
    public void testObjectGetOwnPropertyDescriptor() {
        assertSameOutput("1\ntrue\ntrue\ntrue\n", 
                "let d = Object.getOwnPropertyDescriptor({ a: 1 }, \"a\");\n"
                + "print(d.value);\n"
                + "print(d.enumerable);\n"
                + "print(d.writable);\n"
                + "print(d.configurable);");
    }

    @Test
    public void testObjectGetOwnPropertyDescriptorOfMissingPropertyIsUndefined() {
        assertSameOutput("true\n", "print(Object.getOwnPropertyDescriptor({ a: 1 }, \"x\") == null);");
    }

    @Test
    public void testTheFunctionsOfObjectNeedObjects() {
        // Unlike Javascript (the indexes of a string, Number.prototype, a wrapper for a number)
        for (String call : java.util.List.of("Object.keys(v)", "Object.getOwnPropertyNames(v)", "Object.getPrototypeOf(v)",
                "Object.getOwnPropertyDescriptor(v, \"a\")", "Object.values(v)", "Object(v)", "new Object(v)"))
            for (String value : java.util.List.of("5", "\"ab\"", "true", "null"))
                assertSameFailure(TinyscriptTypeError.class, "const v = " + value + ";\nprint(" + call + ");");
        assertSameFailure(TinyscriptTypeError.class, "const o = {};\nconst d = 1;\nObject.defineProperty(o, \"x\", d);");
        assertSameFailure(TinyscriptTypeError.class, "const o = {};\nconst k = [];\nObject.defineProperty(o, k, { value: 1 });");
        // Object() and new Object() make an empty object, Object(o) is o
        assertSameOutput("{} {} true true {a: 1}\n",
                "const o = { a: 1 };\nprint(Object(), new Object(), Object(o) == o, new Object(o) == o, Object.getOwnPropertyDescriptor(o, \"a\").value == 1 ? o : null);");
        assertSameOutput("true true\n", "const o = {};\nprint(o.constructor == Object, o instanceof Object);");
    }

    @Test
    public void testNewWithNonObjectPrototypeUsesObjectPrototype() {
        assertSameOutput("true\n", 
                "function F() { }\n"
                + "F.prototype = 5;\n"
                + "let o = new F();\n"
                + "print(o.toString != null);");
    }

    @Test
    public void testRedefiningANonConfigurablePropertyIsATypeError() {
        assertSameFailure(TinyscriptTypeError.class,
                "let o = {};\n"
                + "Object.defineProperty(o, \"a\", { value: 1, configurable: false });\n"
                + "Object.defineProperty(o, \"a\", { value: 2 });");
        assertSameOutput("2\n", 
                "let o = {};\n"
                + "Object.defineProperty(o, \"a\", { value: 1, configurable: true });\n"
                + "Object.defineProperty(o, \"a\", { value: 2 });\n"
                + "print(o.a);");
    }

    @Test
    public void testObjectDefinePropertyReadOnly() {
        String code = "let o = {};\n"
                + "Object.defineProperty(o, \"a\", { value: 1, writable: false });\n"
                + "print(o.a, Object.getOwnPropertyDescriptor(o, \"a\").writable);\n";
        assertSameOutput("1 false\n", code);
        // As in Javascript's strict mode, assigning to it is an error
        assertSameFailure(TinyscriptTypeError.class, code + "o.a = 2;");
    }

    @Test
    public void testObjectHasOwnPropertyAndToString() {
        assertSameOutput("true\nfalse\n{a: 1}\n", 
                "let p = { x: 1 };\n"
                + "let o = Object.create(p);\n"
                + "o.y = 2;\n"
                + "print(o.hasOwnProperty(\"y\"));\n"
                + "print(o.hasOwnProperty(\"x\"));\n"
                + "print({ a: 1 }.toString());");
    }

    // =========================================================================
    // Function
    // =========================================================================

    @Test
    public void testFunctionCallAndApply() {
        assertSameOutput("6\n6\n", 
                "function f(a, b) { return this.x + a + b; }\n"
                + "let o = { x: 1 };\n"
                + "print(f.call(o, 2, 3));\n"
                + "print(f.apply(o, [2, 3]));");
    }

    @Test
    public void testFunctionToString() {
        // The same text as print (Javascript: the source)
        // (names of functions in const, of anonymous functions and methods of classes still differ
        // between the backends: TODO.md, phase 1)
        assertSameOutput("[Function: add] [Function: print] [Function: sqrt] [Function: map]\n"
                + "[Function: inner] [Function: m] [Function: n] [class P]\n"
                + "[Function: add],[class P] [Function: add] true\n",
                "function add(a, b) { return a + b; }\n"
                + "const f = function inner(x) { };\nconst o = { m(k) { return k; }, n: x => x };\n"
                + "class P { constructor(x) { this.x = x; } area() { return 1; } }\n"
                + "print(add.toString(), print.toString(), Math.sqrt.toString(), [].map.toString());\n"
                + "print(f.toString(), o.m.toString(), o.n.toString(), P.toString());\n"
                + "print([add, P].join(), \"\" + add, add.toString() == String(add));");
    }

    // =========================================================================
    // Math, System, print
    // =========================================================================

    @Test
    public void testMath() {
        assertSameOutput("4\n3\n-2\ntrue\n3.141592653589793\n", 
                "print(Math.sqrt(16));\n"
                + "print(Math.round(2.5));\n"
                + "print(Math.round(-2.5));\n"
                + "let r = Math.random();\n"
                + "print(r >= 0 && r < 1);\n"
                + "print(Math.PI);");
    }

    @Test
    public void testMathMinMaxAbsFloorCeil() {
        String code = "print(Math.min(3, 4)); print(Math.min(5, -1, 2)); print(Math.max(3, 4)); print(Math.max(7));\n"
                + "print(Math.abs(-2.5)); print(Math.abs(3)); print(Math.floor(2.7)); print(Math.floor(-2.5));\n"
                + "print(Math.ceil(2.1)); print(Math.ceil(-2.5));";
        assertSameOutput("3\n-1\n4\n7\n2.5\n3\n2\n-3\n3\n-2\n", code);
    }

    @Test
    public void testSystemCurrentTimeMillis() {
        assertSameOutput("true\n", "print(System.currentTimeMillis() > 0);");
    }

    @Test
    public void testPrintReportsOutputErrors() {
        OutputStream failing = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("disk full");
            }
        };
        TinyscriptRuntimeException e = assertThrows(TinyscriptRuntimeException.class,
                () -> runInterpreter("print(\"x\");", failing));
        assertEquals("Cannot write output: disk full", e.getMessage());
    }

    @Test
    public void testPrintSeparatesItsArgumentsWithSpaces() {
        assertSameOutput("a 1 true\n", "print(\"a\", 1, true);");
    }

    @Test
    public void testPrintShowsCyclesAsCircular() {
        String code = "const o = { a: 1 }; o.self = o; print(o);\n"
                + "const a = [1]; a.push(a); print(a);\n"
                + "const m = new Map(); m.set(\"me\", m); print(m);\n"
                + "const s = new Set(); s.add(s); print(s);\n"
                + "print(\"o = \" + o);";
        assertSameOutput("{a: 1, self: [Circular]}\n[1, [Circular]]\nMap {\"me\" => [Circular]}\nSet {[Circular]}\no = {a: 1, self: [Circular]}\n",
                code);
    }

    @Test
    public void testPrintShowsValuesLikeNode() {
        // The generated Javascript prints values as the interpreter does (strings quoted as by JSON.stringify)
        String code = "print([\"Banana\", 3, true, 1.5, null, [], {}], { name: \"Markus\", tags: [\"a\", \"b\"], nested: { x: 1 } });\n"
                + "let u; print([u, new Array(2), 0.1 + 0.2, -0, 1e21]);\n"
                + "print([\"say \\\"hi\\\"\", \"it's\", \"a\\nb\", \"tab\\t\", \"back\\\\slash\", \"\\u0001\", \"äöü €\"]);\n"
                + "const m = new Map(); m.set(\"k\", [1]); m.set(2, { v: \"x\" }); print(m, new Set([1, \"1\"]), new Map(), new Set());\n"
                + "print(range(3), [range(1, 9, 2)], new Error(\"boom\"), [new Error(\"\")]);\n"
                + "function named() { } const f = function() { }; print(named, [Math.max]);\n"
                + "print(\"\" + [1, \"a\"], \"x\" + { a: [2] }, String([null]), String({}), String(\"s\"), String(7), String());\n"
                + "print(\"top\", [\"inner\"], typeof String(1), new String(\"wrapped\").length);";
        assertSameOutput("[\"Banana\", 3, true, 1.5, null, [], {}] {name: \"Markus\", tags: [\"a\", \"b\"], nested: {x: 1}}\n"
                + "[undefined, [undefined, undefined], 0.30000000000000004, 0, 1e+21]\n"
                + "[\"say \\\"hi\\\"\", \"it's\", \"a\\nb\", \"tab\\t\", \"back\\\\slash\", \"\\u0001\", \"äöü €\"]\n"
                + "Map {\"k\" => [1], 2 => {v: \"x\"}} Set {1, \"1\"} Map {} Set {}\n"
                + "range(0, 3) [range(1, 9, 2)] Error: boom [Error]\n"
                + "[Function: named] [[Function: max]]\n"
                + "[1, \"a\"] x{a: [2]} [null] {} s 7 \n"
                + "top [\"inner\"] string 7\n", code);
    }

    @Test
    public void testPrintShowsSharedObjectsEachTime() {
        assertSameOutput("[{x: 1}, {x: 1}]\n", "const s = { x: 1 }; print([s, s]);");
    }

    @Test
    public void testPrintLimitsTheNestingDepth() {
        String code = "let l = null; for (const i : range(1, 1001)) { l = { next: l }; } print(l); print([[[1]]]);";
        String out = interpreterOutput(code);
        assertSameOutput(out, code);
        assertTrue(out.contains("{next: [Object]}"), out);
        assertTrue(out.endsWith("[[[1]]]\n"), out);
    }

    @Test
    public void testPrintUsesAUserDefinedToString() {
        String code = "const Point = { toString: function() { return \"(\" + this.x + \",\" + this.y + \")\"; } };\n"
                + "const p = Object.create(Point); p.x = 1; p.y = 2;\n"
                + "print(p); print(\"p = \" + p); print([p, \"s\"]); print({ at: p }); print(String(p), p);";
        assertSameOutput("(1,2)\np = (1,2)\n[(1,2), \"s\"]\n{at: (1,2)}\n(1,2) (1,2)\n", code);
    }

    @Test
    public void testPrintWithAUserDefinedToStringLikeNode() {
        assertSameOutput("const p = { x: 1, toString: function() { return \"P\" + this.x; } };\n"
                + "print(p); print(p, \"and\", 2); print(\"\" + p);");
    }

    // =========================================================================
    // fs: only as the module std:fs (see StdFsTest)
    // =========================================================================

    @Test
    public void testThereIsNoGlobalFs() {
        assertRejected("fs.readFile(\"x\");");
        assertRejected("print(typeof fs);");
    }
}
