package de.mkbauer.tinyscript.standalone.tests;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Ten Advent of Code 2022 solutions in examples/aoc2022 with the examples from the puzzle
 * texts, in the interpreter and in node.
 */
public class AdventOfCode2022Test extends TinyscriptTranspilerTestBase {

    private static final Path DIRECTORY = Path.of("../examples/aoc2022").toAbsolutePath().normalize();

    private void assertDay(String day, String input, String expected, String... moreArgs) {
        String[] args = new String[moreArgs.length + 1];
        args[0] = DIRECTORY.resolve(input).toString();
        System.arraycopy(moreArgs, 0, args, 1, moreArgs.length);
        assertProgramOutput(expected, DIRECTORY.resolve(day + ".tys"), args);
    }

    @Test
    public void testDay07() {
        assertDay("day07", "day07.example.txt", "Part 1: 95437\nPart 2: 24933642\n");
    }

    @Test
    public void testDay11() {
        assertDay("day11", "day11.example.txt", "Part 1: 10605\nPart 2: 2713310158\n");
    }

    @Test
    public void testDay13() {
        assertDay("day13", "day13.example.txt", "Part 1: 13\nPart 2: 140\n");
    }

    @Test
    public void testDay15() {
        // The example uses row 10 and coordinates 0 to 20
        assertDay("day15", "day15.example.txt", "Part 1: 26\nPart 2: 56000011\n", "10", "20");
    }

    @Test
    public void testDay16() {
        assertDay("day16", "day16.example.txt", "Part 1: 1651\nPart 2: 1707\n");
    }

    @Test
    public void testDay17() {
        assertDay("day17", "day17.example.txt", "Part 1: 3068\nPart 2: 1514285714288\n");
    }

    @Test
    public void testDay18() {
        assertDay("day18", "day18.example.txt", "Part 1: 64\nPart 2: 58\n");
    }

    @Test
    public void testDay20() {
        assertDay("day20", "day20.example.txt", "Part 1: 3\nPart 2: 1623178306\n");
    }

    @Test
    public void testDay21() {
        assertDay("day21", "day21.example.txt", "Part 1: 152\nPart 2: 301\n");
    }

    @Test
    public void testDay24() {
        assertDay("day24", "day24.example.txt", "Part 1: 18\nPart 2: 54\n");
    }
}
