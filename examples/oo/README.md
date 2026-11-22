# Objects in tinyscript: the planned class and interface

Programs written twice, to see how the planned `class` and `interface` (see
`TODO.md`, "Objects with methods are verbose") would feel:

| File                                           | Written with                                                                                        | Runs  |
| ---------------------------------------------- | --------------------------------------------------------------------------------------------------- | ----- |
| [expressions.tys](expressions.tys)             | `class` without inheritance, `interface` / `implements`                                             | later |
| [expressions_today.tys](expressions_today.tys) | constructor functions and methods on their `prototype` (today)                                      | yes   |
| [orders.tys](orders.tys)                       | in addition: several interfaces per class, class names after parameters (`listener: EventListener`) | later |
| [orders_today.tys](orders_today.tys)           | constructor functions, a hand-written check in `subscribe`                                          | yes   |

The expressions program represents expressions such as `3 * x^2 + 2 * x - 5` as
trees of objects (`Num`, `Var`, `Add`, `Sub`, `Mul`, `Pow`) that all can
evaluate, differentiate, simplify and print themselves; it uses them for partial
derivatives and Newton's method.

The orders of a small shop go through an event bus to listeners for the
inventory, accounting, shipping and an audit log; events that listeners emit are
queued so that every listener sees them in the same order. The input is read
with `parse`, and lines that cannot be read are reported with `try` / `catch`.

`OoExamplesTest` checks each pair against the same output, in the interpreter
and in node; the tests of the planned versions are disabled until `class` and
`interface` exist.
