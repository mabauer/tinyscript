# Objects in tinyscript: the planned class and interface

A program written twice, to see how the planned `class` and `interface` (see
`TODO.md`, "Objects with methods are verbose") would feel:

| File                                           | Written with                                                         | Runs  |
| ---------------------------------------------- | -------------------------------------------------------------------- | ----- |
| [expressions.tys](expressions.tys)             | `class Name(fields)` without inheritance, `interface` / `implements` | later |
| [expressions_today.tys](expressions_today.tys) | constructor functions and methods on their `prototype` (today)       | yes   |

The program represents expressions such as `3 * x^2 + 2 * x - 5` as trees of
objects (`Num`, `Var`, `Add`, `Sub`, `Mul`, `Pow`) that all can evaluate,
differentiate, simplify and print themselves; it uses them for partial
derivatives and Newton's method.

`OoExamplesTest` checks both files against the same output, in the interpreter
and in node; the test of `expressions.tys` is disabled until `class` and
`interface` exist.
