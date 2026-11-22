package de.mkbauer.tinyscript.standalone.tests;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Ten Advent of Code 2023 solutions in examples/aoc2023 with the examples from the puzzle
 * texts, in the interpreter and in node.
 */
public class AdventOfCode2023Test extends TinyscriptTranspilerTestBase {

    private static final Path DIRECTORY = Path.of("../examples/aoc2023").toAbsolutePath().normalize();

    private void assertDay(String day, String input, String expected, String... moreArgs) {
        String[] args = new String[moreArgs.length + 1];
        args[0] = DIRECTORY.resolve(input).toString();
        System.arraycopy(moreArgs, 0, args, 1, moreArgs.length);
        assertProgramOutput(expected, DIRECTORY.resolve(day + ".tys"), args);
    }

    @Test
    public void testDay03() {
        assertDay("day03", "day03.example.txt", "Part 1: 4361\nPart 2: 467835\n");
    }

    @Test
    public void testDay05() {
        assertDay("day05", "day05.example.txt", "Part 1: 35\nPart 2: 46\n");
    }

    @Test
    public void testDay07() {
        assertDay("day07", "day07.example.txt", "Part 1: 6440\nPart 2: 5905\n");
    }

    @Test
    public void testDay10() {
        // The puzzle gives part 1 for the first two examples and part 2 for the other three;
        // the other answers were checked against a reference solution
        assertDay("day10", "day10.example1.txt", "Part 1: 4\nPart 2: 1\n");
        assertDay("day10", "day10.example2.txt", "Part 1: 8\nPart 2: 1\n");
        assertDay("day10", "day10.example3.txt", "Part 1: 22\nPart 2: 4\n");
        assertDay("day10", "day10.example4.txt", "Part 1: 70\nPart 2: 8\n");
        assertDay("day10", "day10.example5.txt", "Part 1: 80\nPart 2: 10\n");
    }

    @Test
    public void testDay12() {
        assertDay("day12", "day12.example.txt", "Part 1: 21\nPart 2: 525152\n");
    }

    @Test
    public void testDay14() {
        assertDay("day14", "day14.example.txt", "Part 1: 136\nPart 2: 64\n");
    }

    @Test
    public void testDay17() {
        // The second example is only given for part 2
        assertDay("day17", "day17.example1.txt", "Part 1: 102\nPart 2: 94\n");
        assertDay("day17", "day17.example2.txt", "Part 1: 59\nPart 2: 71\n");
    }

    @Test
    public void testDay19() {
        assertDay("day19", "day19.example.txt", "Part 1: 19114\nPart 2: 167409079868000\n");
    }

    @Test
    public void testDay20() {
        // Part 2 needs a module rx, which the examples do not have
        assertDay("day20", "day20.example1.txt", "Part 1: 32000000\nPart 2: no rx module\n");
        assertDay("day20", "day20.example2.txt", "Part 1: 11687500\nPart 2: no rx module\n");
    }

    @Test
    public void testDay24() {
        // The example uses the test area 7 to 27
        assertDay("day24", "day24.example1.txt", "Part 1: 2\nPart 2: 47\n", "7", "27");
    }
}
