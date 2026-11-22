package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * The method shorthand { greet() { ... } } for { greet: function() { ... } }, as in Javascript.
 */
public class MethodShorthandTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testMethods() {
        String code = "const person = {\n"
                + "  name: \"Alice\",\n"
                + "  greet(other) { return \"Hello \" + other + \", I'm \" + this.name; },\n"
                + "  rename(name) { this.name = name; return this; }\n"
                + "};\n"
                + "print(person.greet(\"Bob\"));\n"
                + "print(person.rename(\"Carol\").greet(\"Dave\"), Object.keys(person).join());";
        assertEquals("Hello Bob, I'm Alice\nHello Dave, I'm Carol name,greet,rename\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testParametersAndNames() {
        // Rest parameters, patterns, keywords and strings as names, mixed with the other forms
        String code = "const label = \"x\";\n"
                + "const o = {\n"
                + "  sum(...xs) { return xs.reduce((a, b) => a + b, 0); },\n"
                + "  dist([x1, y1], { x, y }) { return Math.abs(x - x1) + Math.abs(y - y1); },\n"
                + "  delete() { return \"deleted\"; },\n"
                + "  \"two words\"() { return 2; },\n"
                + "  none() { },\n"
                + "  label,\n"
                + "  arrow: () => 3\n"
                + "};\n"
                + "print(o.sum(1, 2, 3), o.dist([1, 1], { x: 4, y: 5 }), o.delete(), o[\"two words\"](), o.none(), o.label, o.arrow());";
        assertEquals("6 7 deleted 2 undefined x 3\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testFactoryWithPrivateState() {
        // The closure keeps n private; no this needed
        String code = "function counter(start) {\n"
                + "  let n = start;\n"
                + "  return {\n"
                + "    inc() { n++; return this; },\n"
                + "    get() { return n; }\n"
                + "  };\n"
                + "}\n"
                + "const a = counter(0); const b = counter(10);\n"
                + "a.inc().inc(); b.inc();\n"
                + "print(a.get(), b.get(), a.n);";
        assertEquals("2 11 undefined\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testMethodsOnAPrototype() {
        String code = "const shape = { describe() { return this.kind + \" with area \" + this.area(); } };\n"
                + "const square = Object.create(shape);\n"
                + "square.kind = \"square\"; square.side = 3;\n"
                + "square.area = function() { return this.side * this.side; };\n"
                + "print(square.describe());";
        assertEquals("square with area 9\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testTheStackTraceNamesTheMethod() {
        TinyscriptRuntimeException e = assertThrows(TinyscriptTypeError.class, () -> interpreterOutput(
                "const o = {\n  load() {\n    const z = null;\n    return z.x;\n  },\n  \"run it\": function() { return this.load(); }\n};\no[\"run it\"]();"));
        String trace = e.getTinyscriptStacktraceAsString();
        assertTrue(trace.contains("at load(") && trace.contains("at run it("), trace);
    }

    @Test
    public void testOnlyWithABody() {
        assertRejected("const o = { f() };");
        assertRejected("const o = { f() => 1 };");
        assertRejected("const o = { f(): 1 };");
    }
}
