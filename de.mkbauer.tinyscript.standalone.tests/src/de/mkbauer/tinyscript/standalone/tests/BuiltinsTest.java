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
        assertEquals("true\nfalse\n", interpreterOutput(
                "print(Array.isArray([1]));\n"
                + "print(Array.isArray(\"x\"));"));
    }

    @Test
    public void testArrayPushPop() {
        assertEquals("3\n1,2,3\n3\n2\n", interpreterOutput(
                "let a = [1];\n"
                + "print(a.push(2, 3));\n"
                + "print(a.join(\",\"));\n"
                + "print(a.pop());\n"
                + "print(a.length);"));
    }

    @Test
    public void testArrayShiftUnshift() {
        String code = "let a = [2];\n"
                + "print(a.unshift(0, 1));\n"
                + "print(a.join(\",\"));\n"
                + "print(a.shift());\n"
                + "print(a.join(\",\"));";
        assertEquals("3\n0,1,2\n0\n1,2\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testArrayMapFilter() {
        assertEquals("2,4,6\n2,3\n", interpreterOutput(
                "print([1, 2, 3].map(x => x * 2).join(\",\"));\n"
                + "print([1, 2, 3].filter(x => x > 1).join(\",\"));"));
    }

    @Test
    public void testArrayJoinAndToString() {
        assertEquals("1,2\n1-2\n[1, \"a\"]\n", interpreterOutput(
                "print([1, 2].join());\n"
                + "print([1, 2].join(\"-\"));\n"
                + "print([1, \"a\"].toString());"));
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
        assertKnownGap(TinyscriptTypeError.class, "let a = []; Object.defineProperty(a, \"x\", { value: 1 });");
    }

    @Test
    public void testArrayAt() {
        String code = "let a = [1, 2, 3];\n"
                + "print(a.at(0)); print(a.at(-1)); print(a.at(-3));\n"
                + "print(a.at(3) == null); print(a.at(-4) == null); print([].at(-1) == null);";
        assertEquals("1\n3\n1\ntrue\ntrue\ntrue\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testArrayIndexOf() {
        String code = "let o = {}; let a = [1, \"two\", o, 1, null];\n"
                + "print(a.indexOf(1)); print(a.indexOf(\"two\")); print(a.indexOf(o)); print(a.indexOf(null));\n"
                + "print(a.indexOf(\"1\")); print(a.indexOf({})); print([].indexOf(1));";
        assertEquals("0\n1\n2\n4\n-1\n-1\n-1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testArrayIndexOfWithStartIndex() {
        // Like Javascript: a negative start index counts from the end
        String code = "let a = [1, 2, 1, 2];\n"
                + "print(a.indexOf(1, 1)); print(a.indexOf(2, -1)); print(a.indexOf(2, -10)); print(a.indexOf(1, 4)); print(a.indexOf(1, 0));";
        assertEquals("2\n3\n1\n-1\n0\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testIndexOfNeedsAnIntegerStartIndex() {
        assertSameFailure(TinyscriptTypeError.class, "[1].indexOf(1, 0.5);");
        assertSameFailure(TinyscriptTypeError.class, "\"a\".indexOf(\"a\", \"1\");");
    }

    @Test
    public void testArrayIndexOfUsesTinyscriptEquality() {
        // Unlike Javascript's ===: null == undefined, and NaN is never found
        assertEquals("0\n-1\n", interpreterOutput(
                "let u; print([u].indexOf(null)); print([0 / 0].indexOf(0 / 0));"));
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
        assertEquals("b\ntrue\n", interpreterOutput(
                "print(\"abc\".charAt(1));\n"
                + "print(\"abc\".charAt(5) == \"\");"));
    }

    @Test
    public void testStringAt() {
        String code = "let s = \"abc\"; print(s.at(0)); print(s.at(-1)); print(s.at(3) == null); print(\"\".at(0) == null);";
        assertEquals("a\nc\ntrue\ntrue\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testStringSubstring() {
        assertEquals("el\nel\nllo\nhe\n", interpreterOutput(
                "print(\"hello\".substring(1, 3));\n"
                + "print(\"hello\".substring(3, 1));\n"
                + "print(\"hello\".substring(2));\n"
                + "print(\"hello\".substring(-1, 2));"));
    }

    @Test
    public void testStringIndexOf() {
        assertEquals("2\n-1\n", interpreterOutput(
                "print(\"hello\".indexOf(\"l\"));\n"
                + "print(\"hello\".indexOf(\"x\"));"));
    }

    @Test
    public void testStringIndexOfWithStartIndex() {
        // Like Javascript: a negative position counts as 0
        String code = "let s = \"abcabc\";\n"
                + "print(s.indexOf(\"b\", 2)); print(s.indexOf(\"b\", -5)); print(s.indexOf(\"c\", 10)); print(s.indexOf(\"\", 10));";
        assertEquals("4\n1\n-1\n6\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testStringToStringAndLength() {
        assertEquals("ab\n3\n", interpreterOutput(
                "let s = new String(\"ab\");\n"
                + "print(s.toString());\n"
                + "print(\"abc\".length);"));
    }

    // =========================================================================
    // Object
    // =========================================================================

    @Test
    public void testObjectCreateAndGetPrototypeOf() {
        assertEquals("1\ntrue\ntrue\n", interpreterOutput(
                "let p = { x: 1 };\n"
                + "let o = Object.create(p);\n"
                + "print(o.x);\n"
                + "print(Object.getPrototypeOf(o) == p);\n"
                + "print(Object.getPrototypeOf(Object.create(null)) == null);"));
    }

    @Test
    public void testObjectCreateWithNonObjectIsATypeError() {
        assertSameFailure(TinyscriptTypeError.class, "Object.create(5);");
    }

    @Test
    public void testObjectDefinePropertyKeysAndPropertyNames() {
        assertEquals("2\na\na,b\n", interpreterOutput(
                "let o = { a: 1 };\n"
                + "Object.defineProperty(o, \"b\", { value: 2, enumerable: false });\n"
                + "print(o.b);\n"
                + "print(Object.keys(o).join(\",\"));\n"
                + "print(Object.getOwnPropertyNames(o).join(\",\"));"));
    }

    @Test
    public void testObjectDefinePropertyOnNonObjectIsATypeError() {
        assertSameFailure(TinyscriptTypeError.class, "Object.defineProperty(1, \"a\", {});");
    }

    @Test
    public void testObjectGetOwnPropertyDescriptor() {
        assertEquals("1\ntrue\ntrue\ntrue\n", interpreterOutput(
                "let d = Object.getOwnPropertyDescriptor({ a: 1 }, \"a\");\n"
                + "print(d.value);\n"
                + "print(d.enumerable);\n"
                + "print(d.writable);\n"
                + "print(d.configurable);"));
    }

    @Test
    public void testObjectGetOwnPropertyDescriptorOfMissingPropertyIsUndefined() {
        assertEquals("true\ntrue\n", interpreterOutput(
                "print(Object.getOwnPropertyDescriptor({ a: 1 }, \"x\") == null);\n"
                + "print(Object.getOwnPropertyDescriptor(5, \"a\") == null);"));
    }

    @Test
    public void testNewWithNonObjectPrototypeUsesObjectPrototype() {
        assertEquals("true\n", interpreterOutput(
                "function F() { }\n"
                + "F.prototype = 5;\n"
                + "let o = new F();\n"
                + "print(o.toString != null);"));
    }

    @Test
    public void testRedefiningANonConfigurablePropertyIsATypeError() {
        assertSameFailure(TinyscriptTypeError.class,
                "let o = {};\n"
                + "Object.defineProperty(o, \"a\", { value: 1, configurable: false });\n"
                + "Object.defineProperty(o, \"a\", { value: 2 });");
        assertEquals("2\n", interpreterOutput(
                "let o = {};\n"
                + "Object.defineProperty(o, \"a\", { value: 1, configurable: true });\n"
                + "Object.defineProperty(o, \"a\", { value: 2 });\n"
                + "print(o.a);"));
    }

    @Test
    public void testObjectDefinePropertyReadOnly() {
        String code = "let o = {};\n"
                + "Object.defineProperty(o, \"a\", { value: 1, writable: false });\n"
                + "print(o.a, Object.getOwnPropertyDescriptor(o, \"a\").writable);\n";
        assertEquals("1 false\n", interpreterOutput(code));
        // As in Javascript's strict mode, assigning to it is an error
        assertSameFailure(TinyscriptTypeError.class, code + "o.a = 2;");
    }

    @Test
    public void testObjectHasOwnPropertyAndToString() {
        assertEquals("true\nfalse\n{a: 1}\n", interpreterOutput(
                "let p = { x: 1 };\n"
                + "let o = Object.create(p);\n"
                + "o.y = 2;\n"
                + "print(o.hasOwnProperty(\"y\"));\n"
                + "print(o.hasOwnProperty(\"x\"));\n"
                + "print({ a: 1 }.toString());"));
    }

    // =========================================================================
    // Function
    // =========================================================================

    @Test
    public void testFunctionCallAndApply() {
        assertEquals("6\n6\n", interpreterOutput(
                "function f(a, b) { return this.x + a + b; }\n"
                + "let o = { x: 1 };\n"
                + "print(f.call(o, 2, 3));\n"
                + "print(f.apply(o, [2, 3]));"));
    }

    @Test
    public void testFunctionToString() {
        assertEquals("function add(a, b) {...}\nfunction print() { // builtin }\n", interpreterOutput(
                "function add(a, b) { return a + b; }\n"
                + "print(add.toString());\n"
                + "print(print.toString());"));
    }

    // =========================================================================
    // Math, System, print
    // =========================================================================

    @Test
    public void testMath() {
        assertEquals("4\n3\n-2\ntrue\n3.141592653589793\n", interpreterOutput(
                "print(Math.sqrt(16));\n"
                + "print(Math.round(2.5));\n"
                + "print(Math.round(-2.5));\n"
                + "let r = Math.random();\n"
                + "print(r >= 0 && r < 1);\n"
                + "print(Math.PI);"));
    }

    @Test
    public void testMathMinMaxAbsFloorCeil() {
        String code = "print(Math.min(3, 4)); print(Math.min(5, -1, 2)); print(Math.max(3, 4)); print(Math.max(7));\n"
                + "print(Math.abs(-2.5)); print(Math.abs(3)); print(Math.floor(2.7)); print(Math.floor(-2.5));\n"
                + "print(Math.ceil(2.1)); print(Math.ceil(-2.5));";
        assertEquals("3\n-1\n4\n7\n2.5\n3\n2\n-3\n3\n-2\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testSystemCurrentTimeMillis() {
        assertEquals("true\n", interpreterOutput("print(System.currentTimeMillis() > 0);"));
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
        assertEquals("a 1 true\n", interpreterOutput("print(\"a\", 1, true);"));
    }

    @Test
    public void testPrintShowsCyclesAsCircular() {
        String code = "const o = { a: 1 }; o.self = o; print(o);\n"
                + "const a = [1]; a.push(a); print(a);\n"
                + "const m = new Map(); m.set(\"me\", m); print(m);\n"
                + "const s = new Set(); s.add(s); print(s);\n"
                + "print(\"o = \" + o);";
        assertEquals("{a: 1, self: [Circular]}\n[1, [Circular]]\nMap {\"me\" => [Circular]}\nSet {[Circular]}\no = {a: 1, self: [Circular]}\n",
                interpreterOutput(code));
        assertSameOutput(code);
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
        assertEquals("[\"Banana\", 3, true, 1.5, null, [], {}] {name: \"Markus\", tags: [\"a\", \"b\"], nested: {x: 1}}\n"
                + "[undefined, [undefined, undefined], 0.30000000000000004, 0, 1e+21]\n"
                + "[\"say \\\"hi\\\"\", \"it's\", \"a\\nb\", \"tab\\t\", \"back\\\\slash\", \"\\u0001\", \"äöü €\"]\n"
                + "Map {\"k\" => [1], 2 => {v: \"x\"}} Set {1, \"1\"} Map {} Set {}\n"
                + "range(0, 3) [range(1, 9, 2)] Error: boom [Error]\n"
                + "[Function: named] [[Function: max]]\n"
                + "[1, \"a\"] x{a: [2]} [null] {} s 7 \n"
                + "top [\"inner\"] string 7\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testPrintShowsSharedObjectsEachTime() {
        assertEquals("[{x: 1}, {x: 1}]\n", interpreterOutput("const s = { x: 1 }; print([s, s]);"));
    }

    @Test
    public void testPrintLimitsTheNestingDepth() {
        String out = interpreterOutput("let l = null; for (const i : range(1, 1001)) { l = { next: l }; } print(l); print([[[1]]]);");
        assertTrue(out.contains("{next: [Object]}"), out);
        assertTrue(out.endsWith("[[[1]]]\n"), out);
    }

    @Test
    public void testPrintUsesAUserDefinedToString() {
        String code = "const Point = { toString: function() { return \"(\" + this.x + \",\" + this.y + \")\"; } };\n"
                + "const p = Object.create(Point); p.x = 1; p.y = 2;\n"
                + "print(p); print(\"p = \" + p); print([p, \"s\"]); print({ at: p }); print(String(p), p);";
        assertEquals("(1,2)\np = (1,2)\n[(1,2), \"s\"]\n{at: (1,2)}\n(1,2) (1,2)\n", interpreterOutput(code));
        assertSameOutput(code);
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
