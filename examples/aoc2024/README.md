# Advent of Code 2024 in tinyscript

Solutions of the first days of [Advent of Code 2024](https://adventofcode.com/2024), written to be read as examples of tinyscript.

| Day | Puzzle | Shows |
|---|---|---|
| [1](day01.tys) | Historian Hysteria | `sort` with a comparison function, `Map` for counting, `reduce` |
| [2](day02.tys) | Red-Nosed Reports | `filter` and `some` with the index, `cond ? a : b` |
| [3](day03.tys) | Mull It Over | a small scanner instead of regular expressions, `slice`, `while` |
| [4](day04.tys) | Ceres Search | a grid of letters, objects as directions, nested loops |

[aoc.tys](aoc.tys) is a module with the helpers for reading the puzzle input (via `std:fs`).

Run a day with your puzzle input (by default `input.txt` in the working directory):

    tinyscript day01.tys -- input.txt

The `*.example*.txt` files are the examples from the puzzle texts; the tests in `de.mkbauer.tinyscript.standalone.tests` (`AdventOfCode2024Test`) run each day with them, in the interpreter and, transpiled to Javascript, in node. Your own puzzle inputs are not part of the repository ([as asked by Advent of Code](https://adventofcode.com/2024/about)).
