# Advent of Code 2023 in tinyscript

Solutions of ten days of [Advent of Code 2023](https://adventofcode.com/2023),
chosen for variety, written to be read as examples of tinyscript (0.25, with
`class`). The 25 days of 2024 are in [`../aoc2024`](../aoc2024).

| Day             | Puzzle                          | Shows                                                                                                                      |
| --------------- | ------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| [3](day03.tys)  | Gear Ratios                     | a class for the numbers of a grid with their neighbors, a `Map` of arrays (`?? []` and `+ [x]`), destructuring parameters  |
| [5](day05.tys)  | If You Give A Seed A Fertilizer | a class for a map, splitting ranges at its rules, `reduce` over a chain of maps, `integers` of `std:string`                |
| [7](day07.tys)  | Camel Cards                     | a class for a hand with methods and `toString`, counting with a `Map` and `??`, sorting by a computed strength             |
| [10](day10.tys) | Pipe Maze                       | following a loop through a grid, objects as directions, counting enclosed tiles by crossings along each row                |
| [12](day12.tys) | Hot Springs                     | counting arrangements with a dynamic programming table, unfolding with `fill`, `join` and `flat`                           |
| [14](day14.tys) | Parabolic Reflector Dish        | a class for the grid, one tilt function for all four directions, finding the repeating loop with a `Map` of states         |
| [17](day17.tys) | Clumsy Crucible                 | Dijkstra's algorithm with a priority queue as a class (a binary heap), states as numbers, an array of best losses          |
| [19](day19.tys) | Aplenty                         | a class for rules, `parse` with literal braces, `find(…)?.target ?? fallback`, splitting ranges recursively                |
| [20](day20.tys) | Pulse Propagation               | classes with the same methods instead of inheritance (flip-flops, conjunctions, broadcaster), the least common multiple    |
| [24](day24.tys) | Never Tell Me The Odds          | Cramer's rule and Gaussian elimination in doubles, keeping integers below 2^53 by rounding only small values, then exactly |

[aoc.tys](aoc.tys) is a module with the helpers for reading the puzzle input
(via `std:fs`), as for 2024.

Run a day with your puzzle input (by default `input.txt` in the working
directory):

    tinyscript day03.tys -- input.txt

Day 24 takes the test area as two more arguments (by default 200000000000000 to
400000000000000; the example uses `-- day24.example1.txt 7 27`).

On synthetic inputs of real size, each day takes at most about 4.5 s (days 12,
14 and 17 about 3–4.5 s, the others 1–2 s, of which about 0.6 s is starting the
JVM); CPython takes 0.03–0.35 s.

The `*.example*.txt` files are the examples from the puzzle texts; the tests in
`de.mkbauer.tinyscript.standalone.tests` (`AdventOfCode2023Test`) run each day
with them, in the interpreter and, transpiled to Javascript, in node. Your own
puzzle inputs are not part of the repository
([as asked by Advent of Code](https://adventofcode.com/2023/about)).
