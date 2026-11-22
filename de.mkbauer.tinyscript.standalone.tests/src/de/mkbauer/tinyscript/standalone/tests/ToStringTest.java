package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * One text form per value: x.toString(), String(x), "" + x and print agree (Javascript gives
 * "[object Object]" and "1,2" for toString).
 */
public class ToStringTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testObjectsAndArraysHaveOneTextForm() {
        String code = "class Point {\n  constructor(x, y) { this.x = x; this.y = y; }\n}\n"
                + "const p = new Point(3, 4);\n"
                + "const values = [p, { a: [1, \"b\"] }, [1, [2, 3], \"c\"], new Map([[\"k\", 1]]), new Set([1]), [], {}];\n"
                + "for (const v : values) {\n"
                + "  print(v.toString() == String(v), v.toString() == \"\" + v, v.toString());\n"
                + "}\n"
                + "print(v2());\n"
                + "function v2() { return [[1, 2], { x: 1 }].join(\" | \"); }";
        assertEquals("true true Point {x: 3, y: 4}\n"
                + "true true {a: [1, \"b\"]}\n"
                + "true true [1, [2, 3], \"c\"]\n"
                + "true true Map {\"k\" => 1}\n"
                + "true true Set {1}\n"
                + "true true []\n"
                + "true true {}\n"
                + "[1, 2] | {x: 1}\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testAnOwnToStringIsUsedInsideAndCanCallTheBuiltinOne() {
        // Object.prototype.toString shows the object itself, also when the object has a toString of its own
        String code = "class Money {\n  constructor(cents) { this.cents = cents; }\n"
                + "  toString() { return (this.cents / 100).toFixed(2); }\n"
                + "  debug() { return Object.prototype.toString.call(this); }\n}\n"
                + "const m = new Money(1250);\n"
                + "print(m, [m], m.toString(), \"\" + m, m.debug(), [m].toString());";
        assertEquals("12.50 [12.50] 12.50 12.50 Money {cents: 1250} [12.50]\n", interpreterOutput(code));
        assertSameOutput(code);
    }
}
