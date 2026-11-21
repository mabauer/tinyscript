package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import de.mkbauer.tinyscript.interpreter.ResourceLimits;
import de.mkbauer.tinyscript.interpreter.TinyscriptResourceLimitViolation;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Map and Set (step 4 of the Advent of Code plan in TODO.md).
 */
public class MapSetTest extends TinyscriptTranspilerTestBase {

    // =========================================================================
    // Map
    // =========================================================================

    @Test
    public void testMapBasics() {
        String code = "let m = new Map(); m.set(\"a\", 1).set(\"b\", 2); m.set(\"a\", 3);\n"
                + "print(m.get(\"a\"), m.get(\"x\"), m.has(\"b\"), m.has(\"x\"), m.size);\n"
                + "print(m.delete(\"b\"), m.delete(\"b\"), m.size); m.clear(); print(m.size);";
        assertEquals("3 undefined true false 2\ntrue false 1\n0\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testMapKeysOfAnyType() {
        String code = "let m = new Map(); let o = {}; let p = {};\n"
                + "m.set(1, \"number\"); m.set(\"1\", \"string\"); m.set(true, \"boolean\"); m.set(null, \"null\"); m.set(o, \"o\");\n"
                + "print(m.get(1), m.get(\"1\"), m.get(true), m.get(null), m.get(o), m.get(p), m.size);\n"
                + "m.set(0 / 0, \"nan\"); print(m.get(0 / 0)); m.set(-0, \"zero\"); print(m.get(0));";
        assertEquals("number string boolean null o undefined 5\nnan\nzero\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testMapKeysValuesEntriesAreArraysInInsertionOrder() {
        String code = "let m = new Map([[\"b\", 2], [\"a\", 1]]); m.set(\"c\", 3); m.set(\"b\", 20);\n"
                + "print(m.keys().join(\",\")); print(m.values().join(\",\")); print(m.entries().map(e => e[0] + \"=\" + e[1]).join(\",\"));\n"
                + "print(m.keys().length, m.values().reduce((s, x) => s + x, 0));";
        assertEquals("b,a,c\n20,1,3\nb=20,a=1,c=3\n3 24\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testMapIteration() {
        String code = "let m = new Map(); m.set(\"x\", 1); m.set(\"y\", 2);\n"
                + "for (let e : m) { print(e[0] + e[1]); }\n"
                + "m.forEach((value, key) => { print(key + \"->\" + value); });\n"
                + "for (let k : m.keys()) { m.set(k + k, 0); } print(m.size);";
        assertEquals("x1\ny2\nx->1\ny->2\n4\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testCountingWithAMap() {
        String code = "let counts = new Map();\n"
                + "for (let w : \"a b a c a b\".split(\" \")) { counts.set(w, (counts.has(w) ? counts.get(w) : 0) + 1); }\n"
                + "print(counts.keys().sort().map(k => k + counts.get(k)).join(\" \"));";
        assertEquals("a3 b2 c1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    // =========================================================================
    // Set
    // =========================================================================

    @Test
    public void testSetBasics() {
        String code = "let s = new Set([1, 2, 2, 3]); s.add(4).add(1);\n"
                + "print(s.size, s.has(2), s.has(\"2\"), s.delete(2), s.has(2), s.size);\n"
                + "print(s.values().join(\",\")); s.clear(); print(s.size); print(new Set().size);";
        assertEquals("4 true false true false 3\n1,3,4\n0\n0\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testSetIteration() {
        String code = "let s = new Set([\"b\", \"a\", \"b\"]);\n"
                + "for (let x : s) { print(x); }\n"
                + "s.forEach(x => { print(x + x); });\n"
                + "let seen = new Set(); function seenBefore(x) { let d = seen.has(x); seen.add(x); return d; }\n"
                + "print([3, 1, 3, 2, 1].filter(seenBefore).join(\",\"));";
        assertEquals("b\na\nbb\naa\n3,1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testSetOfStringKeysForGrids() {
        // A common pattern: a set of visited positions as "x,y" strings
        String code = "let visited = new Set(); let x = 0; let y = 0;\n"
                + "for (let d : \"RRUL\".split(\"\")) { if (d == \"R\") { x++; } else if (d == \"L\") { x--; } else { y++; } visited.add(x + \",\" + y); }\n"
                + "print(visited.size, visited.has(\"1,1\"));";
        assertEquals("4 true\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    // =========================================================================
    // Iterable for loop and strictness
    // =========================================================================

    @Test
    public void testForLoopOverAString() {
        String code = "let s = \"\"; for (let c : \"abc\") { s = c + s; } print(s);";
        assertEquals("cba\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testForLoopNeedsSomethingIterable() {
        assertInterpreterThrows(TinyscriptTypeError.class, "for (let k : { a: 1 }) { }");
        assertInterpreterThrows(TinyscriptTypeError.class, "for (let k : 42) { }");
        assertInterpreterThrows(TinyscriptTypeError.class, "let u; for (let k : u) { }");
    }

    @Test
    public void testMapAndSetHaveNoOtherProperties() {
        // m[k] = v instead of m.set(k, v) is a common mistake
        assertInterpreterThrows(TinyscriptTypeError.class, "let m = new Map(); m[\"a\"] = 1;");
        assertInterpreterThrows(TinyscriptTypeError.class, "let m = new Map(); m.size = 3;");
        assertInterpreterThrows(TinyscriptTypeError.class, "let s = new Set(); s.x = 1;");
    }

    @Test
    public void testMethodsOnlyWorkOnMapsAndSets() {
        assertInterpreterThrows(TinyscriptTypeError.class, "let get = new Map().get; get(1);");
        assertInterpreterThrows(TinyscriptTypeError.class, "new Map([1, 2]);");
        assertInterpreterThrows(TinyscriptTypeError.class, "new Set(42);");
        assertInterpreterThrows(TinyscriptTypeError.class, "new Map().forEach(1);");
    }

    @Test
    public void testTypeofAndInstanceof() {
        String code = "let m = new Map(); let s = new Set();\n"
                + "print(typeof m, m instanceof Map, s instanceof Set, s instanceof Map, Array.isArray(m.keys()));";
        assertEquals("object true true false true\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testSizeLimit() {
        ResourceLimits limits = new ResourceLimits();
        limits.setMaxObjectSize(100);
        assertThrows(TinyscriptResourceLimitViolation.class,
                () -> interpreterOutput("let s = new Set(); for (const i : range(1, 1001)) { s.add(i); }", limits));
        assertThrows(TinyscriptResourceLimitViolation.class,
                () -> interpreterOutput("let m = new Map(); for (const i : range(1, 1001)) { m.set(i, i); }", limits));
    }
}
