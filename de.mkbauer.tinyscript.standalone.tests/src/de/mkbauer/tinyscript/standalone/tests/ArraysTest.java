package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import de.mkbauer.tinyscript.interpreter.ResourceLimits;
import de.mkbauer.tinyscript.interpreter.TinyscriptResourceLimitViolation;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * Array methods, as in Javascript.
 */
public class ArraysTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testSortWithAComparisonFunction() {
        String code = "let a = [10, 9, 1, 100]; let b = a.sort((x, y) => x - y); print(a.join(\",\")); print(a == b);\n"
                + "print([3, 1, 2].sort((x, y) => y - x).join(\",\"));\n"
                + "let people = [{ n: \"b\", age: 2 }, { n: \"a\", age: 1 }, { n: \"c\", age: 1 }];\n"
                + "print(people.sort((p, q) => p.age - q.age).map(p => p.n).join(\"\"));";
        assertEquals("1,9,10,100\ntrue\n3,2,1\nacb\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testSortWithoutAComparisonFunctionOnlySortsStrings() {
        String code = "print([\"b\", \"c\", \"a\"].sort().join(\",\")); print([].sort().length);";
        assertEquals("a,b,c\n0\n", interpreterOutput(code));
        assertSameOutput(code);
        // Javascript sorts numbers as strings ([1, 10, 9]), a trap
        assertSameFailure(TinyscriptTypeError.class, "[10, 9, 1].sort();");
        assertSameFailure(TinyscriptTypeError.class, "[3, 1].sort((x, y) => x < y);");
        assertSameFailure(TinyscriptTypeError.class, "[3, 1].sort(1);");
    }

    @Test
    public void testSlice() {
        String code = "let a = [1, 2, 3, 4]; print(a.slice(1, 3).join(\",\")); print(a.slice(-2).join(\",\")); print(a.slice().join(\",\"));\n"
                + "print(a.slice(3, 1).length); print(a.slice(1, -1).join(\",\")); let c = a.slice(); c.push(5); print(a.length);";
        assertEquals("2,3\n3,4\n1,2,3,4\n0\n2,3\n4\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testReduce() {
        String code = "print([1, 2, 3].reduce((sum, x) => sum + x, 0)); print([1, 2, 3].reduce((sum, x) => sum + x));\n"
                + "print([\"a\", \"b\"].reduce((s, x, i) => s + x + i, \"\")); print([].reduce((s, x) => s + x, 42));";
        assertEquals("6\n6\na0b1\n42\n", interpreterOutput(code));
        assertSameOutput(code);
        assertSameFailure(TinyscriptTypeError.class, "[].reduce((s, x) => s + x);");
    }

    @Test
    public void testForEachAndIndexes() {
        String code = "let sum = 0; [1, 2, 3].forEach(x => { sum = sum + x; }); print(sum);\n"
                + "[\"a\", \"b\"].forEach((x, i) => { print(i + x); });\n"
                + "print([5, 6].map((x, i) => x * i).join(\",\")); print([5, 6, 7].filter((x, i) => i != 1).join(\",\"));\n"
                + "let n = 0; [1, 2].forEach(() => { n = n + 1; }); print(n); print([1, 2].map(() => 0).join(\",\"));";
        assertEquals("6\n0a\n1b\n0,6\n5,7\n2\n0,0\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testSomeEveryFindFindIndex() {
        String code = "let a = [1, 5, 8];\n"
                + "print(a.some(x => x > 6), a.some(x => x > 9), a.every(x => x > 0), a.every(x => x > 1));\n"
                + "print([].some(x => true), [].every(x => false));\n"
                + "print(a.find(x => x > 2)); print(a.find(x => x > 9)); print(a.findIndex(x => x > 2)); print(a.findIndex(x => x > 9));";
        assertEquals("true false true false\nfalse true\n5\nundefined\n1\n-1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testCallbacksMustReturnConditions() {
        assertSameFailure(TinyscriptTypeError.class, "[1, 2].some(x => x);");
        assertSameFailure(TinyscriptTypeError.class, "[1, 2].find(x => x - 1);");
    }

    @Test
    public void testCallbacksMustBeFunctions() {
        assertSameFailure(TinyscriptTypeError.class, "[1].forEach(1);");
        assertSameFailure(TinyscriptTypeError.class, "[1].map(\"x\");");
        assertSameFailure(TinyscriptTypeError.class, "[1].filter(null);");
        assertSameFailure(TinyscriptTypeError.class, "[1].reduce({}, 0);");
    }

    @Test
    public void testIncludes() {
        String code = "print([1, 2, 3].includes(2), [1, 2].includes(\"2\"), [\"a\"].includes(\"a\"), [null].includes(null));";
        assertEquals("true false true true\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testFill() {
        String code = "print([1, 2, 3].fill(0).join(\",\")); print([1, 2, 3, 4].fill(9, 1, 3).join(\",\")); print([1, 2, 3].fill(7, -1).join(\",\"));";
        assertEquals("0,0,0\n1,9,9,4\n1,2,7\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testNewArrayWithALength() {
        String code = "let a = new Array(3); print(a.length); print(a.fill(0).join(\",\"));\n"
                + "let grid = new Array(2).fill(0).map(() => new Array(3).fill(\".\")); grid[1][2] = \"#\"; print(grid.map(row => row.join(\"\")).join(\"/\"));\n"
                + "print(new Array().length); print(new Array(1, 2).join(\",\")); print(new Array(\"a\").join(\",\"));";
        assertEquals("3\n0,0,0\n.../..#\n0\n1,2\na\n", interpreterOutput(code));
        assertSameOutput(code);
        assertSameFailure(TinyscriptTypeError.class, "new Array(-1);");
        assertSameFailure(TinyscriptTypeError.class, "new Array(1.5);");
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testNewArrayChecksTheObjectSizeLimit() {
        ResourceLimits limits = new ResourceLimits();
        limits.setMaxObjectSize(1000);
        assertThrows(TinyscriptResourceLimitViolation.class, () -> interpreterOutput("new Array(1000000000);", limits));
    }

    @Test
    public void testJoinUsesACommaByDefault() {
        String code = "print([1, 2, 3].join()); print([1, 2].join(\"\")); print([].join());";
        assertEquals("1,2,3\n12\n\n", interpreterOutput(code));
        assertSameOutput(code);
        assertSameFailure(TinyscriptTypeError.class, "[1, 2].join(1);");
    }
}
