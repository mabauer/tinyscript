# Objects in tinyscript: classes

Two programs written with `class`, each next to the same program written as
before `class`, with constructor functions and methods on their `prototype`
(`_today`). Both versions print the same.

| Program                            | Before `class`                                 | Shows                                                                                                                                 |
| ---------------------------------- | ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| [expressions.tys](expressions.tys) | [expressions_today.tys](expressions_today.tys) | objects of different classes used alike (they implement the same interface), code that several classes share in a function            |
| [orders.tys](orders.tys)           | [orders_today.tys](orders_today.tys)           | classes for data and for listeners, an event bus that tests for the method `onEvent`, so that an object literal can be a listener too |

The expressions program represents expressions such as `3 * x^2 + 2 * x - 5` as
trees of objects (`Num`, `Var`, `Add`, `Sub`, `Mul`, `Pow`) that all can
evaluate, differentiate, simplify and print themselves; it uses them for partial
derivatives and Newton's method.

The orders of a small shop go through an event bus to listeners for the
inventory, accounting, shipping and an audit log; events that listeners emit are
queued so that every listener sees them in the same order. The input is read
with `parse`, and lines that cannot be read are reported with `try` / `catch`.

How classes were written before `class`, each next to the same with `class`:

| File                                                   | Shows                                                                                                                                       |
| ------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------- |
| [car.tys](car.tys)                                     | a constructor function with methods on its prototype, wrapped in an IIFE (as TypeScript compiled `class`)                                   |
| [prototypes_as_classes.tys](prototypes_as_classes.tys) | inheritance with `super`, built from prototypes only (what `class` leaves out on purpose); with `class`, composition instead of inheritance |

See also the language reference,
[Classes and objects](../../docs/language-reference.md#9-classes-and-objects),
and the programming guide,
[Objects with behavior](../../docs/programming-guide.md#1-objects-with-behavior).

`OoExamplesTest` runs every example in the interpreter and in node and checks
that each pair prints the same.
