package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * The generated Javascript behaves as the interpreter where they used to differ: it runs in
 * strict mode, read-only builtins and new of a non-constructor fail in both, includes finds NaN,
 * and names that only Javascript reserves work.
 */
public class BackendAgreementTest extends TinyscriptTranspilerTestBase {

    // Both fail with a TypeError: the interpreter with message, node with its own
    private void assertBothFail(String message, String code) {
        TinyscriptRuntimeException e = assertThrows(TinyscriptTypeError.class, () -> interpreterOutput(code));
        assertEquals(message, e.getMessage());
        assumeNodeAvailable();
        AssertionError nodeError = assertThrows(AssertionError.class, () -> NodeJsRunner.run(transpile(parse(code))));
        assertTrue(nodeError.getMessage().contains("TypeError"), nodeError.getMessage());
    }

    @Test
    public void testIncludesFindsNaN() {
        String code = "print([1, NaN].includes(NaN), [NaN].indexOf(NaN), [NaN, 1].includes(NaN, 1), [0].includes(-0), [1].includes(NaN));";
        assertEquals("true -1 false true false\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testThisInAPlainCallIsUndefined() {
        String code = "function f() { return this; }\nconst o = { f };\nprint(f(), o.f() == o);";
        assertEquals("undefined true\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testReadOnlyBuiltins() {
        assertBothFail("Cannot assign to the read-only property 'PI'", "Math.PI = 3;");
        assertBothFail("Cannot assign to the read-only property 'E'", "Math.E += 1;");
        assertBothFail("Cannot assign to the read-only property 'length' of a function", "function f(a) { }\nf.length = 2;");
        assertBothFail("Cannot assign to the read-only property 'name' of a function", "const f = x => x;\nf.name = \"g\";");
        // Methods can be replaced, in both
        String code = "Math.sqrt = x => x;\nprint(Math.sqrt(4));";
        assertEquals("4\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testBuiltinsCannotBeAssigned() {
        assertEquals(java.util.List.of("1:1: Cannot assign to the builtin Math"), reportedErrors("Math = 1;"));
        assertEquals(java.util.List.of("1:1: Cannot assign to the builtin Infinity"), reportedErrors("Infinity = 1;"));
        assertEquals(java.util.List.of("1:1: Cannot assign to the builtin print"), reportedErrors("print = 1;"));
    }

    @Test
    public void testNewNeedsAConstructor() {
        assertBothFail("'new' needs a class or a constructor function, not an arrow function", "const f = x => x;\nconst o = new f(1);");
        assertBothFail("'new' needs a class or a constructor function, not a method",
                "class C { m() { } }\nconst c = new C();\nconst o = new c.m();");
        assertBothFail("'new' needs a class or a constructor function, not a method",
                "const o = { m() { } };\nconst p = new o.m();");
        assertBothFail("'new' needs a class or a constructor function, not the builtin function sqrt", "const o = new Math.sqrt(4);");
        assertBothFail("'new' needs a class or a constructor function, not the builtin function print", "const o = new print(4);");
        assertBothFail("'new' needs a class or a constructor function, not the builtin function range", "const o = new range(4);");
        // Classes, functions written with function and builtin constructors
        String code = "function P(x) { this.x = x; }\nconst o = { Q: function (y) { this.y = y; } };\nclass R { }\n"
                + "print(new P(1), new o.Q(2), new R(), new Map(), new Error(\"e\").message);";
        assertEquals("{x: 1} {y: 2} R {} Map {} e\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testDeclarationsHideThePrelude() {
        String code = "const Map = 1;\nlet String = \"s\";\nconst JSON = [2];\nconst System = 3;\nprint(Map, String, JSON, System);";
        assertEquals("1 s [2] 3\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNamesThatOnlyJavascriptReserves() {
        String code = "const default = 1;\nlet yield = 2;\n"
                + "function enum(in) { const arguments = [in]; return arguments; }\n"
                + "class static { constructor(do) { this.do = do; } }\n"
                + "const { do, eval } = { do: 3, eval: 4 };\n"
                + "const o = { default, with: 5 };\n"
                + "const f = function await() { };\n"
                + "print(default, yield, enum(4), new static(6), do, eval, o, enum, static, f);";
        assertEquals("1 2 [4] static {do: 6} 3 4 {default: 1, with: 5} [Function: enum] [class static] [Function: await]\n",
                interpreterOutput(code));
        assertSameOutput(code);
    }
}
