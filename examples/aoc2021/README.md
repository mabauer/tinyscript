# Advent of Code 2021 in tinyscript

Solutions of fifteen days of
[Advent of Code 2021](https://adventofcode.com/2021), ported as literally as
possible from TypeScript ([mabauer/aoc2021](https://github.com/mabauer/aoc2021),
written to learn TypeScript): the same classes, functions and algorithms, so
that they show how code written with Javascript habits looks in tinyscript. The
days of 2023 and 2024, written for tinyscript, are in [`../aoc2023`](../aoc2023)
and [`../aoc2024`](../aoc2024).

| Day                              | Puzzle                  | Shows                                                                                               |
| -------------------------------- | ----------------------- | --------------------------------------------------------------------------------------------------- |
| [1](day01.tys), [1a](day01a.tys) | Sonar Sweep             | comparing neighbors; in 1a a method `reduce_pairs` added to `Array.prototype`                       |
| [2](day02.tys)                   | Dive!                   | two classes with the same methods (TypeScript: `extends`), default values with `??`                 |
| [3](day03.tys)                   | Binary Diagnostic       | columns of bits, `parseInt(s, 2)`, an object instead of an `enum`                                   |
| [4](day04.tys)                   | Giant Squid             | a class for a bingo board, `Set`s of drawn numbers, `words` of `std:string`                         |
| [5](day05.tys)                   | Hydrothermal Venture    | drawing lines on a 1000×1000 canvas, `parse` of `std:string` instead of a regular expression        |
| [6](day06.tys)                   | Lanternfish             | counting fish per age instead of simulating each one                                                |
| [7](day07.tys)                   | The Treachery of Whales | cost functions as arrow functions, `f.apply(null, array)` instead of spread                         |
| [8](day08.tys)                   | Seven Segment Search    | backtracking over the digits, an object as a mapping                                                |
| [9](day09.tys)                   | Smoke Basin             | a grid class, recursive flood fill                                                                  |
| [10](day10.tys)                  | Syntax Scoring          | a recursive descent parser for brackets                                                             |
| [11](day11.tys)                  | Dumbo Octopus           | a cellular automaton with a work list                                                               |
| [12](day12.tys)                  | Passage Pathing         | depth first search over all paths, copying a `Set` per step                                         |
| [13](day13.tys)                  | Transparent Origami     | folding points, printing the result as a picture                                                    |
| [19](day19.tys)                  | Beacon Scanner          | 3D points, 24 rotation matrices (a module of its own, [`day19/rotations.tys`](day19/rotations.tys)) |
| [20](day20.tys)                  | Trench Map              | an image of infinite size: the cells, and the value of everything around them                       |

[aoc.tys](aoc.tys) reads the puzzle input as the TypeScript originals did (all
lines, also the empty last one); [utils.tys](utils.tys) has the helpers of
`utils.ts` that the days use, [point.tys](point.tys) the class `Point` that days
5, 9, 11 and 13 share (each TypeScript day had its own copy).

Run a day with your puzzle input (by default `input.txt` in the working
directory):

    tinyscript day05.tys -- input.txt

## Notes on the port

- Comments starting with "TypeScript:" mark where the original is written
  differently and why: no default parameters (`x ?? default` instead), no spread
  in calls (`f.apply(null, array)`), no rest elements in patterns
  (`items.slice(1)`), no `extends`, getters, field declarations or `enum`s, no
  `in` operator, no regular expressions (`parse` and `words` of `std:string`),
  no trailing comma in array literals, `from` is a reserved word.
- Javascript forgives what tinyscript does not: a comparison with `undefined` or
  `NaN` is `false` in Javascript but a `TypeError` in tinyscript (day 1), as is
  a string as a condition (day 4) or destructuring too short an array (day 2).
- The originals print a lot of debug output; it is left out.
- One change for speed only: `listIntersect` in `utils.tys` uses a `Set` of the
  hash codes instead of comparing each element with each other one. The result
  is the same, but day 19 takes 53 s instead of about 50 minutes.
- Bugs of the originals are fixed, marked with "Fixed:": points were compared by
  a hash code that is not unique (`31 * (31 * 7 + x) + y`; days 9, 11, 13, 19),
  so in day 13 a few dots got lost when folding (part 1: 712 instead of 720);
  day 4 added an empty board and relied on a blank line at the end of the input;
  day 20 only worked for square images. Otherwise, the answers on real inputs
  are the same as those of the originals.

On real inputs, days 1–13 take 0.5–5.4 s each (day 5 the most, for its large
canvas), day 20 45 s and day 19 53 s; node takes at most 0.25 s for the
originals, except 11 s for day 19.

The `*.example*.txt` files are the examples from the puzzle texts (for day 20
also one from the original's tests); the tests in
`de.mkbauer.tinyscript.standalone.tests` (`AdventOfCode2021Test`) run each day
with them, in the interpreter and, transpiled to Javascript, in node. Your own
puzzle inputs are not part of the repository
([as asked by Advent of Code](https://adventofcode.com/2021/about)).
