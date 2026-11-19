package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.mkbauer.tinyscript.interpreter.TinyscriptArgumentError;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TinyscriptUnsupportedOperationError;

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
                "var a = [1];\n"
                + "print(a.push(2, 3));\n"
                + "print(a.join(\",\"));\n"
                + "print(a.pop());\n"
                + "print(a.length);"));
    }

    @Test
    public void testArrayShiftUnshift() {
        assertEquals("3\n1,0,2\n1\n0,2\n", interpreterOutput(
                "var a = [2];\n"
                + "print(a.unshift(0, 1));\n"
                + "print(a.join(\",\"));\n"
                + "print(a.shift());\n"
                + "print(a.join(\",\"));"));
    }

    @Test
    public void testArrayMapFilter() {
        assertEquals("2,4,6\n2,3\n", interpreterOutput(
                "print([1, 2, 3].map(x => x * 2).join(\",\"));\n"
                + "print([1, 2, 3].filter(x => x > 1).join(\",\"));"));
    }

    @Test
    public void testArrayJoinAndToString() {
        assertEquals("12\n1-2\n1, a\n", interpreterOutput(
                "print([1, 2].join());\n"
                + "print([1, 2].join(\"-\"));\n"
                + "print([1, \"a\"].toString());"));
    }

    @Test
    public void testInvalidArrayLengthIsATypeError() {
        assertInterpreterThrows(TinyscriptTypeError.class, "var a = []; a.length = -1;");
        assertInterpreterThrows(TinyscriptTypeError.class, "var a = []; a.length = 1.5;");
        assertInterpreterThrows(TinyscriptTypeError.class, "var a = []; a.length = \"2\";");
    }

    @Test
    public void testArraysHaveNoNamedProperties() {
        // Arrays are lists: writing anything but an integer index or length is a TypeError
        assertInterpreterThrows(TinyscriptTypeError.class, "var a = []; a.x = 5;");
        assertInterpreterThrows(TinyscriptTypeError.class, "var a = []; a[\"name\"] = 5;");
        assertInterpreterThrows(TinyscriptTypeError.class, "var a = [1]; a[-1] = 5;");
        assertInterpreterThrows(TinyscriptTypeError.class, "var a = [1]; a[1.5] = 5;");
        assertInterpreterThrows(TinyscriptTypeError.class, "var a = []; Object.defineProperty(a, \"x\", { value: 1 });");
    }

    @Test
    public void testArrayAt() {
        String code = "var a = [1, 2, 3];\n"
                + "print(a.at(0)); print(a.at(-1)); print(a.at(-3));\n"
                + "print(a.at(3) == null); print(a.at(-4) == null); print([].at(-1) == null);";
        assertEquals("1\n3\n1\ntrue\ntrue\ntrue\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testAtNeedsAnIntegerIndex() {
        assertInterpreterThrows(TinyscriptTypeError.class, "[1].at(1.5);");
        assertInterpreterThrows(TinyscriptTypeError.class, "[1].at(\"0\");");
        assertInterpreterThrows(TinyscriptTypeError.class, "\"a\".at(0.5);");
        assertInterpreterThrows(TinyscriptArgumentError.class, "[1].at();");
    }

    @Test
    public void testArrayMethodOnNonArrayIsATypeError() {
        assertInterpreterThrows(TinyscriptTypeError.class,
                "var o = {};\no.push = [].push;\no.push(1);");
    }

    @Test
    public void testBuiltinCalledWithTooFewArgumentsIsAnArgumentError() {
        assertInterpreterThrows(TinyscriptArgumentError.class, "[1].push();");
        assertInterpreterThrows(TinyscriptArgumentError.class, "Math.sqrt();");
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
        String code = "var s = \"abc\"; print(s.at(0)); print(s.at(-1)); print(s.at(3) == null); print(\"\".at(0) == null);";
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
    public void testStringToStringAndLength() {
        assertEquals("ab\n3\n", interpreterOutput(
                "var s = new String(\"ab\");\n"
                + "print(s.toString());\n"
                + "print(\"abc\".length);"));
    }

    // =========================================================================
    // Object
    // =========================================================================

    @Test
    public void testObjectCreateAndGetPrototypeOf() {
        assertEquals("1\ntrue\ntrue\n", interpreterOutput(
                "var p = { x: 1 };\n"
                + "var o = Object.create(p);\n"
                + "print(o.x);\n"
                + "print(Object.getPrototypeOf(o) == p);\n"
                + "print(Object.getPrototypeOf(Object.create(null)) == null);"));
    }

    @Test
    public void testObjectCreateWithNonObjectIsATypeError() {
        assertInterpreterThrows(TinyscriptTypeError.class, "Object.create(5);");
    }

    @Test
    public void testObjectDefinePropertyKeysAndPropertyNames() {
        assertEquals("2\na\na,b\n", interpreterOutput(
                "var o = { a: 1 };\n"
                + "Object.defineProperty(o, \"b\", { value: 2, enumerable: false });\n"
                + "print(o.b);\n"
                + "print(Object.keys(o).join(\",\"));\n"
                + "print(Object.getOwnPropertyNames(o).join(\",\"));"));
    }

    @Test
    public void testObjectDefinePropertyOnNonObjectIsATypeError() {
        assertInterpreterThrows(TinyscriptTypeError.class, "Object.defineProperty(1, \"a\", {});");
    }

    @Test
    public void testObjectGetOwnPropertyDescriptor() {
        assertEquals("1\ntrue\ntrue\ntrue\n", interpreterOutput(
                "var d = Object.getOwnPropertyDescriptor({ a: 1 }, \"a\");\n"
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
                + "var o = new F();\n"
                + "print(o.toString != null);"));
    }

    @Test
    public void testObjectDefinePropertyReadOnly() {
        assertEquals("1\nfalse\n", interpreterOutput(
                "var o = {};\n"
                + "Object.defineProperty(o, \"a\", { value: 1, writable: false });\n"
                + "o.a = 2;\n"
                + "print(o.a);\n"
                + "print(Object.getOwnPropertyDescriptor(o, \"a\").writable);"));
    }

    @Test
    public void testObjectHasOwnPropertyAndToString() {
        assertEquals("true\nfalse\n[object Object]\n", interpreterOutput(
                "var p = { x: 1 };\n"
                + "var o = Object.create(p);\n"
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
                + "var o = { x: 1 };\n"
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
                + "var r = Math.random();\n"
                + "print(r >= 0 && r < 1);\n"
                + "print(Math.PI);"));
    }

    @Test
    public void testSystemCurrentTimeMillis() {
        assertEquals("true\n", interpreterOutput("print(System.currentTimeMillis() > 0);"));
    }

    @Test
    public void testPrintConcatenatesItsArguments() {
        assertEquals("a1true\n", interpreterOutput("print(\"a\", 1, true);"));
    }

    // =========================================================================
    // fs: only available when not sandboxed
    // =========================================================================

    @Test
    public void testFileSystemIsNotAvailableWhenSandboxed() {
        assertInterpreterThrows(TinyscriptUnsupportedOperationError.class, "fs.readFile(\"x\");");
        assertInterpreterThrows(TinyscriptUnsupportedOperationError.class, "fs.writeFile(\"x\", \"y\");");
    }

    @Test
    public void testFileSystemWriteAndRead(@TempDir File tempDir) throws IOException {
        String path = new File(tempDir, "test.txt").getAbsolutePath().replace("\\", "\\\\");
        assertEquals("5\nhello\nundefined\n", interpreterOutputUnsandboxed(
                "print(fs.writeFile(\"" + path + "\", \"hello\"));\n"
                + "print(fs.readFile(\"" + path + "\"));\n"
                + "var missing = fs.readFile(\"" + path + ".missing\");\n"
                + "print(typeof missing);"));
    }
}
