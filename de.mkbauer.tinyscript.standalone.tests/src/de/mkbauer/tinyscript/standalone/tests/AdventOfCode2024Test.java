package de.mkbauer.tinyscript.standalone.tests;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * The Advent of Code 2024 solutions in examples/aoc2024 with the examples from the
 * puzzle texts, in the interpreter and in node.
 */
public class AdventOfCode2024Test extends TinyscriptTranspilerTestBase {

    private static final Path DIRECTORY = Path.of("../examples/aoc2024").toAbsolutePath().normalize();

    private void assertDay(String day, String input, String expected, String... moreArgs) {
        String[] args = new String[moreArgs.length + 1];
        args[0] = DIRECTORY.resolve(input).toString();
        System.arraycopy(moreArgs, 0, args, 1, moreArgs.length);
        assertProgramOutput(expected, DIRECTORY.resolve(day + ".tys"), args);
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

    @Test
    public void testDay11() {
        // Part 2 has no example answer in the puzzle; checked against a reference solution
        assertDay("day11", "day11.example.txt", "Part 1: 55312\nPart 2: 65601038650482\n");
    }

    @Test
    public void testDay12() {
        assertDay("day12", "day12.example1.txt", "Part 1: 140\nPart 2: 80\n");
        assertDay("day12", "day12.example2.txt", "Part 1: 1930\nPart 2: 1206\n");
    }

    @Test
    public void testDay13() {
        assertDay("day13", "day13.example.txt", "Part 1: 480\nPart 2: 875318608908\n");
    }

    @Test
    public void testDay14() {
        // The example room is 11 x 7; part 2 (a picture) has no example answer
        assertDay("day14", "day14.example.txt", "Part 1: 12\nPart 2: 1\n", "11", "7");
    }

    @Test
    public void testDay15() {
        assertDay("day15", "day15.example1.txt", "Part 1: 2028\nPart 2: 1751\n");
        assertDay("day15", "day15.example2.txt", "Part 1: 10092\nPart 2: 9021\n");
    }
}
