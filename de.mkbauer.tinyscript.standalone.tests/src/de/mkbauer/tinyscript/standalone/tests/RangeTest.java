package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * range(end), range(start, end[, step]): a lazy range of integers with an exclusive end, for
 * counting in the iterable for loop: for (const i : range(n)) (decided 2026-11-20).
 */
public class RangeTest extends TinyscriptTranspilerTestBase {

    @TempDir
    Path tempDir;

    @Test
    public void testCountingWithRange() {
        String code = "let s = \"\";\n"
                + "for (const i : range(5)) { s += i; } s += \" \";\n"
                + "for (const i : range(2, 6)) { s += i; } s += \" \";\n"
                + "for (const i : range(10, 0, -3)) { s += i + \",\"; } s += \" \";\n"
                + "for (const i : range(0, 10, 4)) { s += i + \",\"; } s += \" \";\n"
                + "for (const i : range(3, 3)) { s += \"never\"; } for (const i : range(5, 2)) { s += \"never\"; }\n"
                + "print(s);";
        assertEquals("01234 2345 10,7,4,1, 0,4,8, \n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testRangeOverTheIndexesOfAnArray() {
        String code = "const a = [\"x\", \"y\", \"z\"]; let s = \"\";\n"
                + "for (const i : range(a.length)) { s += i + a[i]; } print(s);\n"
                + "for (const i : range(a.length - 1, -1, -1)) { if (i == 1) { continue; } if (i == 0) { break; } print(a[i]); }";
        assertEquals("0x1y2z\nz\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testReverse() {
        String code = "let s = \"\";\n"
                + "for (const i : range(5).reverse()) { s += i; } s += \" \";\n"
                + "for (const i : range(2, 6).reverse()) { s += i; } s += \" \";\n"
                + "for (const i : range(1, 10, 3).reverse()) { s += i + \",\"; } s += \" \";\n"
                + "for (const i : range(10, 0, -3).reverse()) { s += i + \",\"; } s += \" \";\n"
                + "for (const i : range(0).reverse()) { s += \"never\"; }\n"
                + "for (const i : range(3, 3).reverse()) { s += \"never\"; }\n"
                + "print(s);\n"
                + "print(range(5).reverse(), range(1, 10, 3).reverse(), range(5).reverse().length, range(0).reverse().length);\n"
                + "print(range(4).reverse().toArray(), range(3).reverse().reverse(), range(4).reverse().map(i => i * i));\n"
                + "const a = [\"x\", \"y\", \"z\"]; const back = []; for (const i : range(a.length).reverse()) { back.push(a[i]); } print(back);";
        assertEquals("43210 5432 7,4,1, 1,4,7,10, \n"
                + "range(4, -1, -1) range(7, -2, -3) 5 0\n"
                + "[3, 2, 1, 0] range(0, 3) [9, 4, 1, 0]\n"
                + "[\"z\", \"y\", \"x\"]\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    @Timeout(5)
    public void testReverseIsLazy() {
        // A trillion integers: only lazy is this fast
        String code = "for (const i : range(1000000000000).reverse()) { print(i); break; } print(range(1000000000000).reverse().length);";
        assertEquals("999999999999\n1000000000000\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testRangeIsLazy() {
        // A range does not create its numbers: a huge one costs nothing until it is used
        String code = "const huge = range(1000000000000); print(huge.length);\n"
                + "let first = -1; for (const i : huge) { first = i; break; } print(first);";
        assertEquals("1000000000000\n0\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testRangeMethods() {
        String code = "print(range(4).toArray().join(), range(4).map(i => i * i).join(), range(1, 10, 3).length, range(5, 1).length);\n"
                + "let sum = 0; range(5).forEach(i => { sum += i; }); print(sum);\n"
                + "const grid = range(2).map(() => range(3).map(() => \".\")); print(grid.map(row => row.join(\"\")).join(\"/\"));\n"
                + "print(range(3), range(1, 9, 2));\n"
                + "print(new Set(range(3)).size, typeof range(1));";
        assertEquals("0,1,2,3 0,1,4,9 3 0\n10\n.../...\nrange(0, 3) range(1, 9, 2)\n3 object\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testClosuresSeeTheirOwnValue() {
        String code = "const fs = []; for (const i : range(3)) { fs.push(() => i); } print(fs.map(f => f()).join());";
        assertEquals("0,1,2\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testRangeIsStrict() {
        TinyscriptRuntimeException e = assertThrows(TinyscriptTypeError.class, () -> interpreterOutput("range(1.5);"));
        assertEquals("range() needs integers, not 1.5", e.getMessage());
        assertSameFailure(TinyscriptTypeError.class, "range(\"3\");");
        assertSameFailure(TinyscriptTypeError.class, "range(0, 5, 0);");
        assertEquals(java.util.List.of("1:1: range() needs at least 1 argument, not 0"), reportedErrors("range();"));
        assertEquals(java.util.List.of("1:1: range() takes at most 3 arguments, not 4"), reportedErrors("range(1, 2, 3, 4);"));
    }

    @Test
    @Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void testToArrayChecksTheObjectSizeLimit() {
        de.mkbauer.tinyscript.interpreter.ResourceLimits limits = new de.mkbauer.tinyscript.interpreter.ResourceLimits();
        limits.setMaxObjectSize(1000);
        assertThrows(de.mkbauer.tinyscript.interpreter.TinyscriptResourceLimitViolation.class,
                () -> interpreterOutput("range(1000000).toArray();", limits));
    }

    @Test
    public void testStdArrayHasNoRangeAnymore() throws IOException {
        Path main = Files.writeString(tempDir.resolve("main.tys"), "import range from \"std:array\";\nprint(1);");
        assertThrows(TinyscriptRuntimeException.class, () -> runModule(main));
    }
}
