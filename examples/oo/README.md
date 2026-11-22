# Objects in tinyscript: classes

How classes were written before `class`, each next to the same with `class`:

| File                                                   | Shows                                                                                                                            |
| ------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |
| [car.tys](car.tys)                                     | a constructor function with methods on its prototype, wrapped in an IIFE (as TypeScript compiled `class`)                        |
| [prototypes_as_classes.tys](prototypes_as_classes.tys) | inheritance with `super`, built from prototypes only (what `class` leaves out); with `class`, composition instead of inheritance |

Programs written twice, with `class` and with constructor functions and
prototypes as before `class`:

| File                                           | Written with                                     |
| ---------------------------------------------- | ------------------------------------------------ |
| [expressions.tys](expressions.tys)             | `class`                                          |
| [expressions_today.tys](expressions_today.tys) | constructor functions and methods on `prototype` |
| [orders.tys](orders.tys)                       | `class`                                          |
| [orders_today.tys](orders_today.tys)           | constructor functions and methods on `prototype` |

The expressions program represents expressions such as `3 * x^2 + 2 * x - 5` as
trees of objects (`Num`, `Var`, `Add`, `Sub`, `Mul`, `Pow`) that all can
evaluate, differentiate, simplify and print themselves; it uses them for partial
derivatives and Newton's method.

The orders of a small shop go through an event bus to listeners for the
inventory, accounting, shipping and an audit log; events that listeners emit are
queued so that every listener sees them in the same order. The input is read
with `parse`, and lines that cannot be read are reported with `try` / `catch`.

`OoExamplesTest` runs every example in the interpreter and in node and checks
that each pair prints the same.
