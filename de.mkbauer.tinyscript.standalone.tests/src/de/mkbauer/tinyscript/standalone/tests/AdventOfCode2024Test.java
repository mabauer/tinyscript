package de.mkbauer.tinyscript.standalone.tests;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * The Advent of Code 2024 solutions in examples/aoc2024 with the examples from the
 * puzzle texts, in the interpreter and in node.
 */
public class AdventOfCode2024Test extends TinyscriptTranspilerTestBase {

    private static final Path DIRECTORY = Path.of("../examples/aoc2024").toAbsolutePath().normalize();

    private void assertDay(String day, String input, String expected) {
        assertProgramOutput(expected, DIRECTORY.resolve(day + ".tys"), DIRECTORY.resolve(input).toString());
    }

    @Test
    public void testDay01() {
        assertDay("day01", "day01.example.txt", "Part 1: 11\nPart 2: 31\n");
    }

    @Test
    public void testDay02() {
        assertDay("day02", "day02.example.txt", "Part 1: 2\nPart 2: 4\n");
    }

    @Test
    public void testDay03() {
        // The two parts have different examples
        assertDay("day03", "day03.example1.txt", "Part 1: 161\nPart 2: 161\n");
        assertDay("day03", "day03.example2.txt", "Part 1: 161\nPart 2: 48\n");
    }

    @Test
    public void testDay04() {
        assertDay("day04", "day04.example.txt", "Part 1: 18\nPart 2: 9\n");
    }

    @Test
    public void testDay05() {
        assertDay("day05", "day05.example.txt", "Part 1: 143\nPart 2: 123\n");
    }

    @Test
    public void testDay06() {
        assertDay("day06", "day06.example.txt", "Part 1: 41\nPart 2: 6\n");
    }

    @Test
    public void testDay07() {
        assertDay("day07", "day07.example.txt", "Part 1: 3749\nPart 2: 11387\n");
    }

    @Test
    public void testDay08() {
        assertDay("day08", "day08.example.txt", "Part 1: 14\nPart 2: 34\n");
    }

    @Test
    public void testDay09() {
        assertDay("day09", "day09.example.txt", "Part 1: 1928\nPart 2: 2858\n");
    }

    @Test
    public void testDay10() {
        assertDay("day10", "day10.example.txt", "Part 1: 36\nPart 2: 81\n");
    }
}
