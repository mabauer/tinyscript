# Advent of Code 2024 in tinyscript

Solutions of the first days of [Advent of Code 2024](https://adventofcode.com/2024), written to be read as examples of tinyscript.

| Day | Puzzle | Shows |
|---|---|---|
| [1](day01.tys) | Historian Hysteria | destructuring, `sort` with a comparison function, `Map` for counting with `??`, `reduce` |
| [2](day02.tys) | Red-Nosed Reports | `filter` and `some` with the index, `cond ? a : b` |
| [3](day03.tys) | Mull It Over | a small scanner instead of regular expressions, `slice`, `while` |
| [4](day04.tys) | Ceres Search | a grid of letters, objects as directions, nested loops |
| [5](day05.tys) | Print Queue | a `Set` of rules, `sort` with a comparison function built from them |
| [6](day06.tys) | Guard Gallivant | a simulation, `Set`s of numbers for visited positions and states, loop detection, `throw` |
| [7](day07.tys) | Bridge Repair | recursion with pruning, destructuring of `split` |
| [8](day08.tys) | Resonant Collinearity | a `Map` of arrays (`?? []`), a `Set` of positions, `{ row, col }` |
| [9](day09.tys) | Disk Fragmenter | two pointers, objects as files and free spans, `find`, destructuring in `for` |
| [10](day10.tys) | Hoof It | depth-first search, counting paths and distinct ends at once |
| [11](day11.tys) | Plutonian Pebbles | memoization with a `Map`, numbers beyond 32 bits |
| [12](day12.tys) | Garden Groups | flood fill, counting corners instead of sides, an arrow function as local helper |
| [13](day13.tys) | Claw Contraption | reading numbers from text (`integers` of `std:string`), Cramer's rule, `Number.isInteger` |
| [14](day14.tys) | Restroom Redoubt | optional script arguments (`System.args`), patterns as parameters, a remainder that is never negative |
| [15](day15.tys) | Warehouse Woes | a mutable grid, recursive pushing of boxes (also two cells wide) |

[aoc.tys](aoc.tys) is a module with the helpers for reading the puzzle input (via `std:fs`).

Run a day with your puzzle input (by default `input.txt` in the working directory):

    tinyscript day01.tys -- input.txt

Days 1–5, 8, 10, 12, 13 and 15 take about a second at most, day 11 about 3 s, day 7 and day 14 about 6–7 s and day 9 about 10 s on inputs of real size. Part 2 of day 6 simulates the guard once for each position on her route, about 20–30 million steps for a real input; that takes about a minute (tinyscript is a tree-walking interpreter, roughly ten times slower than CPython).

The `*.example*.txt` files are the examples from the puzzle texts; the tests in `de.mkbauer.tinyscript.standalone.tests` (`AdventOfCode2024Test`) run each day with them, in the interpreter and, transpiled to Javascript, in node. Your own puzzle inputs are not part of the repository ([as asked by Advent of Code](https://adventofcode.com/2024/about)).
