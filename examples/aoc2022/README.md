# Advent of Code 2022 in tinyscript

Solutions of ten days of [Advent of Code 2022](https://adventofcode.com/2022),
chosen for variety, written to be read as examples of tinyscript (0.27, with
default parameters, spread and rest elements). The other years are in
[`../aoc2021`](../aoc2021), [`../aoc2023`](../aoc2023) and
[`../aoc2024`](../aoc2024).

| Day             | Puzzle                   | Shows                                                                                                           |
| --------------- | ------------------------ | --------------------------------------------------------------------------------------------------------------- |
| [7](day07.tys)  | No Space Left On Device  | a tree of directories as a class, `Map`s of children and files, a rest element for lines of different lengths   |
| [11](day11.tys) | Monkey in the Middle     | a class built from a block of lines, operations as arrow functions, worry levels modulo the `lcm` of divisors   |
| [13](day13.tys) | Distress Signal          | packets read with `JSON.parse`, a recursive comparison function for `sort`, `Array.isArray`                     |
| [15](day15.tys) | Beacon Exclusion Zone    | merging intervals, finding the one free position where the edges of the sensors' ranges cross                   |
| [16](day16.tys) | Proboscidea Volcanium    | distances by breadth first search, the best pressure for each set of opened valves as bits, the best of subsets |
| [17](day17.tys) | Pyroclastic Flow         | rocks drawn as pictures and turned into bits, a falling simulation, skipping the repeating cycle to 10^12 rocks |
| [18](day18.tys) | Boiling Boulders         | 3D cubes as numbers in a `Set`, flood fill of the space around them, spread in a call (`key(...cube)`)          |
| [20](day20.tys) | Grove Positioning System | moving numbers in a circle with `indexOf` and `splice`, a modulo for negative numbers, numbers up to 10^13      |
| [21](day21.tys) | Monkey Math              | a tree of operations as a `Map` of functions (no `switch`), solving for the unknown by undoing the operations   |
| [24](day24.tys) | Blizzard Basin           | breadth first search over the minutes, blizzards computed from where they started instead of moved              |

[aoc.tys](aoc.tys) is a module with the helpers for reading the puzzle input
(via `std:fs`), as for 2023 and 2024.

Run a day with your puzzle input (by default `input.txt` in the working
directory):

    tinyscript day07.tys -- input.txt

Day 15 takes the row of part 1 and the largest coordinate of part 2 as two more
arguments (by default 2000000 and 4000000; the example uses
`-- day15.example.txt 10 20`).

On synthetic inputs of real size (checked against reference solutions in
Python), each day takes at most about 4.5 s (day 24 4.5 s, days 11 and 16 about
2.5 s, the others 0.5–1 s, of which about 0.6 s is starting the JVM); CPython
takes 0.03–1.1 s.

The `*.example.txt` files are the examples from the puzzle texts; the tests in
`de.mkbauer.tinyscript.standalone.tests` (`AdventOfCode2022Test`) run each day
with them, in the interpreter and, transpiled to Javascript, in node. Your own
puzzle inputs are not part of the repository
([as asked by Advent of Code](https://adventofcode.com/2022/about)).
