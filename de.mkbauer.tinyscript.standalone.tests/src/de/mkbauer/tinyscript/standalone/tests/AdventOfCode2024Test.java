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
    public void testDay16() {
        assertDay("day16", "day16.example1.txt", "Part 1: 7036\nPart 2: 45\n");
        assertDay("day16", "day16.example2.txt", "Part 1: 11048\nPart 2: 64\n");
    }

    @Test
    public void testDay17() {
        // The two parts have different examples; the first has no self-reproducing value of A
        assertDay("day17", "day17.example1.txt", "Part 1: 4,6,3,5,6,3,5,2,1,0\nPart 2: none\n");
        assertDay("day17", "day17.example2.txt", "Part 1: 5,7,3,0\nPart 2: 117440\n");
    }

    @Test
    public void testDay18() {
        // The example memory is 7 x 7, with 12 fallen bytes
        assertDay("day18", "day18.example.txt", "Part 1: 22\nPart 2: 6,1\n", "7", "12");
    }

    @Test
    public void testDay19() {
        assertDay("day19", "day19.example.txt", "Part 1: 6\nPart 2: 16\n");
    }

    @Test
    public void testDay20() {
        // The cheats of the example save less than 100 picoseconds; the counts are from the puzzle text
        assertDay("day20", "day20.example.txt", "Part 1: 1\nPart 2: 285\n", "50");
        assertDay("day20", "day20.example.txt", "Part 1: 5\nPart 2: 1449\n", "20");
    }

    @Test
    public void testDay21() {
        // Part 2 has no example answer in the puzzle; checked against a reference solution
        assertDay("day21", "day21.example.txt", "Part 1: 126384\nPart 2: 154115708116294\n");
    }

    @Test
    public void testDay22() {
        // The two parts have different examples
        assertDay("day22", "day22.example1.txt", "Part 1: 37327623\nPart 2: 24\n");
        assertDay("day22", "day22.example2.txt", "Part 1: 37990510\nPart 2: 23\n");
    }

    @Test
    public void testDay23() {
        assertDay("day23", "day23.example.txt", "Part 1: 7\nPart 2: co,de,ka,ta\n");
    }

    @Test
    public void testDay24() {
        // Part 2 needs an adder; the small example is none, so its result only shows the rules
        assertDay("day24", "day24.example.txt", "Part 1: 4\nPart 2: z00,z01\n");
    }

    @Test
    public void testDay25() {
        assertDay("day25", "day25.example.txt", "Part 1: 3\n");
    }

    @Test
    public void testDay15() {
        assertDay("day15", "day15.example1.txt", "Part 1: 2028\nPart 2: 1751\n");
        assertDay("day15", "day15.example2.txt", "Part 1: 10092\nPart 2: 9021\n");
    }
}
