# Advent of Code 2024 in tinyscript

Solutions of all 25 days of
[Advent of Code 2024](https://adventofcode.com/2024), written to be read as
examples of tinyscript.

| Day             | Puzzle                 | Shows                                                                                                   |
| --------------- | ---------------------- | ------------------------------------------------------------------------------------------------------- |
| [1](day01.tys)  | Historian Hysteria     | destructuring, `sort` with a comparison function, `Map` for counting with `??`, `reduce`                |
| [2](day02.tys)  | Red-Nosed Reports      | `filter` and `some` with the index, `cond ? a : b`                                                      |
| [3](day03.tys)  | Mull It Over           | `findAll` of `std:string` instead of regular expressions, `split` and `join`                            |
| [4](day04.tys)  | Ceres Search           | a grid of letters, objects as directions, nested loops                                                  |
| [5](day05.tys)  | Print Queue            | a `Set` of rules, `sort` with a comparison function built from them                                     |
| [6](day06.tys)  | Guard Gallivant        | a simulation, `Set`s of numbers for visited positions and states, loop detection, `throw`               |
| [7](day07.tys)  | Bridge Repair          | recursion with pruning, reading lines with `parse` of `std:string`                                      |
| [8](day08.tys)  | Resonant Collinearity  | a `Map` of arrays (`?? []`), a `Set` of positions, `{ row, col }`                                       |
| [9](day09.tys)  | Disk Fragmenter        | two pointers, objects as files and free spans, `find`, destructuring in `for`                           |
| [10](day10.tys) | Hoof It                | depth-first search, counting paths and distinct ends at once                                            |
| [11](day11.tys) | Plutonian Pebbles      | memoization with a `Map`, numbers beyond 32 bits                                                        |
| [12](day12.tys) | Garden Groups          | flood fill, counting corners instead of sides, an arrow function as local helper                        |
| [13](day13.tys) | Claw Contraption       | reading lines with `parse` of `std:string`, Cramer's rule, `Number.isInteger`                           |
| [14](day14.tys) | Restroom Redoubt       | optional script arguments (`System.args`), patterns as parameters, a remainder that is never negative   |
| [15](day15.tys) | Warehouse Woes         | a mutable grid, recursive pushing of boxes (also two cells wide)                                        |
| [16](day16.tys) | Reindeer Maze          | Dijkstra's algorithm with a priority queue (a binary heap in a closure), searching forward and backward |
| [17](day17.tys) | Chronospatial Computer | a small virtual machine with bitwise operators; a search digit by digit                                 |
| [18](day18.tys) | RAM Run                | breadth-first search, binary search                                                                     |
| [19](day19.tys) | Linen Layout           | dynamic programming (`startsWith` with a position)                                                      |
| [20](day20.tys) | Race Condition         | distances along a path, all cells within a Manhattan distance                                           |
| [21](day21.tys) | Keypad Conundrum       | recursion over the levels of robots with memoization                                                    |
| [22](day22.tys) | Monkey Market          | bitwise operators (`^`, `<<`, `>>`, `&`), arrays as counters                                            |
| [23](day23.tys) | LAN Party              | a graph as a `Map` of `Set`s, triangles, the Bron-Kerbosch algorithm for the largest clique             |
| [24](day24.tys) | Crossed Wires          | a circuit evaluated with memoization; the rules of an adder to find the swapped wires                   |
| [25](day25.tys) | Code Chronicle         | blocks of lines, `every` with the index (only one part)                                                 |

[aoc.tys](aoc.tys) is a module with the helpers for reading the puzzle input
(via `std:fs`).

Run a day with your puzzle input (by default `input.txt` in the working
directory):

    tinyscript day01.tys -- input.txt

On inputs of real size, most days take about 2 seconds at most; day 11 about 3
s, days 7, 14 and 20 about 6–7 s, day 9 about 10 s, and day 22 about 7 s. Part 2
of day 6 simulates the guard once for each position on her route, about 20–30
million steps for a real input; that takes about a minute (tinyscript is a
tree-walking interpreter, roughly ten times slower than CPython).

The `*.example*.txt` files are the examples from the puzzle texts; the tests in
`de.mkbauer.tinyscript.standalone.tests` (`AdventOfCode2024Test`) run each day
with them, in the interpreter and, transpiled to Javascript, in node. Your own
puzzle inputs are not part of the repository
([as asked by Advent of Code](https://adventofcode.com/2024/about)).
