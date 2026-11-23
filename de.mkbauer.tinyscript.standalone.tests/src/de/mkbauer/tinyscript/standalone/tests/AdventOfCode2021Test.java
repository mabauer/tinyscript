package de.mkbauer.tinyscript.standalone.tests;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Advent of Code 2021 solutions in examples/aoc2021, ported from TypeScript, with the
 * examples from the puzzle texts, in the interpreter and in node.
 */
public class AdventOfCode2021Test extends TinyscriptTranspilerTestBase {

    private static final Path DIRECTORY = Path.of("../examples/aoc2021").toAbsolutePath().normalize();

    private void assertDay(String day, String input, String expected) {
        assertProgramOutput(expected, DIRECTORY.resolve(day + ".tys"), DIRECTORY.resolve(input).toString());
    }

    @Test
    public void testDay01() {
        assertDay("day01", "day01.example.txt", "Part 1: 7\nPart 2: 5\n");
        // Part 1 once more, with a method added to Array.prototype
        assertDay("day01a", "day01.example.txt", "Part 1: 7\n");
    }

    @Test
    public void testDay02() {
        assertDay("day02", "day02.example.txt", "Part 1: 150\nPart 2: 900\n");
    }

    @Test
    public void testDay03() {
        assertDay("day03", "day03.example.txt", "Part 1: 198\nPart 2: 230\n");
    }

    @Test
    public void testDay04() {
        assertDay("day04", "day04.example.txt", "Part 1: 4512\nPart 2: 1924\n");
    }

    @Test
    public void testDay05() {
        assertDay("day05", "day05.example.txt", "Part 1: 5\nPart 2: 12\n");
    }

    @Test
    public void testDay06() {
        assertDay("day06", "day06.example.txt", "Part 1: 5934\nPart 2: 26984457539\n");
    }

    @Test
    public void testDay07() {
        assertDay("day07", "day07.example.txt", "Part 1: 37\nPart 2: 168\n");
    }

    @Test
    public void testDay08() {
        assertDay("day08", "day08.example.txt", "Part 1: 26\nPart 2: 61229\n");
    }

    @Test
    public void testDay09() {
        assertDay("day09", "day09.example.txt", "Part 1: 15\nPart 2: 1134\n");
    }

    @Test
    public void testDay10() {
        assertDay("day10", "day10.example.txt", "Part 1: 26397\nPart 2: 288957\n");
    }

    @Test
    public void testDay11() {
        assertDay("day11", "day11.example.txt", "Part 1: 1656\nPart 2: 195\n");
    }

    @Test
    public void testDay12() {
        assertDay("day12", "day12.example1.txt", "Part 1: 10\nPart 2: 36\n");
        assertDay("day12", "day12.example2.txt", "Part 1: 19\nPart 2: 103\n");
        assertDay("day12", "day12.example3.txt", "Part 1: 226\nPart 2: 3509\n");
    }

    @Test
    public void testDay13() {
        // Part 2 is the picture of the folded paper (in the example a square)
        assertDay("day13", "day13.example.txt",
                "Part 1: 17\n#####\n#   #\n#   #\n#   #\n#####\nPart 2: See above\n");
    }

    @Test
    public void testDay19() {
        assertDay("day19", "day19.example.txt", "Part 1: 79\nPart 2: 3621\n");
    }

    @Test
    public void testDay20() {
        // About 15 s in the interpreter: 50 steps, each pixel read through a string of nine
        // digits. (day20.example2.txt, from the original's tests, has a background that
        // alternates between dark and lit; it gives 30 and 2153, as the original does.)
        assertDay("day20", "day20.example1.txt", "Part 1: 35\nPart 2: 3351\n");
    }
}
