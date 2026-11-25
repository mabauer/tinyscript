# Tinyscript Programming Guide

The [language reference](language-reference.md) says what each construct does.
This guide shows how to put them together: which kind of object to use when, how
to share code without inheritance, what closures are good for, and how to tell
your own errors apart.

1. [Objects with behavior](#1-objects-with-behavior)
2. [Closures in practice](#2-closures-in-practice)
3. [Your own kinds of errors](#3-your-own-kinds-of-errors)

---

## 1. Objects with behavior

There are three ways to make objects with behavior. Choose by what you need:

| Form                                                 | Use it for                                          | Example                                              |
| ---------------------------------------------------- | --------------------------------------------------- | ---------------------------------------------------- |
| [Object literal](#object-literals)                   | a single object: settings, a record, a small helper | `{ x, y, length() { … } }`                           |
| [Factory function](#factory-functions-with-closures) | objects with private state, without `this`          | `stack()` returning `{ push(x) { … }, pop() { … } }` |
| [`class`](#classes-and-interfaces)                   | many objects of the same kind with shared methods   | `class Point { … }`                                  |

There is no inheritance between classes: code that several kinds of objects
share goes into a function, or one object uses another one (see
[Reuse without inheritance](#reuse-without-inheritance)). Objects of different
kinds can still be used alike, if they have the same methods (see
[Objects of different kinds used alike](#objects-of-different-kinds-used-alike)).
Under the hood, all of this is Javascript's prototypes (see
[Under the hood: prototypes](language-reference.md#under-the-hood-prototypes)).

### Object literals

A literal (see [Objects](language-reference.md#7-objects)) with methods is the
simplest object with behavior; `this` in a method is the object the method is
called on:

```
const origin = {
    x: 0,
    y: 0,
    distanceTo(other) { return Math.hypot(other.x - this.x, other.y - this.y); }
};
print(origin.distanceTo({ x: 3, y: 4 }));   // 5
```

### Factory functions with closures

A function that returns an object literal makes objects whose state only their
methods can reach (see [Closures](language-reference.md#10-closures)). The
methods use the variables of the function, not `this`:

```
function stack() {
    const items = [];           // private: only the methods see it
    return {
        push(x) { items.push(x); },
        pop() { return items.pop(); },
        size() { return items.length; }
    };
}

const s = stack();
s.push(1);
s.push(2);
print(s.pop(), s.size(), s.items);   // 2 1 undefined
```

Each call makes new state. Since the methods do not use `this`, they also work
when taken off the object (`const push = s.push; push(3);`).

### Classes and interfaces

A class describes many objects of the same kind: the `constructor` gives each
new object its fields, the methods are shared by all of them. An interface names
the methods that several classes must have. Both are described in the language
reference: [Classes](language-reference.md#classes) and
[Interfaces](language-reference.md#interfaces).

### Objects of different kinds used alike

Objects that have the same methods can be used alike, whatever made them:
objects of different classes, and object literals too.

```
class Circle {
    constructor(radius) { this.radius = radius; }
    area() { return Math.PI * this.radius * this.radius; }
    toString() { return "circle " + this.radius; }
}

class Square {
    constructor(side) { this.side = side; }
    area() { return this.side * this.side; }
    toString() { return "square " + this.side; }
}

const shapes = [new Circle(1), new Square(2), { area() { return 1.5; }, toString() { return "patch"; } }];
for (const shape : shapes) {
    print(shape + ": " + shape.area().toFixed(2));
}
```

Describe the methods such objects need in a comment. Where a missing method
would only show up much later, a function can check for it right away:

```
function addListener(listeners, listener) {
    if (listener == null || typeof listener.onEvent != "function") {
        throw new Error("a listener needs a method onEvent(event)");
    }
    listeners.push(listener);
}
```

Otherwise, calling a method that is not there is a clear error anyway:
`Square has no method 'perimeter'`. The examples
[`expressions.tys`](../examples/oo/expressions.tys) (kinds of expressions that
can all evaluate, derive and simplify themselves) and
[`orders.tys`](../examples/oo/orders.tys) (listeners on an event bus) are built
this way.

### Reuse without inheritance

Instead of a class that extends another one, an object can have another one and
pass work on to it (composition):

```
class Person {
    constructor(name) { this.name = name; }
    describe() { return "Person called " + this.name; }
}

class Employee {
    constructor(name, title) {
        this.person = new Person(name);
        this.title = title;
    }
    describe() { return this.person.describe() + " (" + this.title + ")"; }
}

print(new Employee("Jane", "CTO").describe());   // Person called Jane (CTO)
```

Code that several classes share, but that does not belong to any of them, goes
into a function next to them, e.g. `binary(left, op, right, precedence)` in
[`expressions.tys`](../examples/oo/expressions.tys), which the classes `Add`,
`Sub` and `Mul` use for `toString`.

---

## 2. Closures in practice

A closure is a function that keeps access to the variables of the scope it was
defined in (see [Closures](language-reference.md#10-closures)). Two common uses:
a function that remembers results (here in a `Map`, which only the returned
function can reach) and a function made from a parameter, e.g. a comparison for
`sort`:

```
function memoize(f) {
    const cache = new Map();
    return function(n) {
        if (!cache.has(n)) {
            cache.set(n, f(n));
        }
        return cache.get(n);
    };
}

// Without the cache, fib(70) would take years
const fib = memoize(n => n < 2 ? n : fib(n - 1) + fib(n - 2));
print(fib(70));   // 190392490709135

function by(key) {
    return (a, b) => a[key] - b[key];
}

const people = [{ name: "Ann", age: 41 }, { name: "Bob", age: 23 }, { name: "Cy", age: 35 }];
people.sort(by("age"));
print(people.map(p => p.name).join(", "));   // Bob, Cy, Ann
```

---

## 3. Your own kinds of errors

`catch` gets the errors thrown with `throw`, failed input / output and invalid
JSON (see [Errors](language-reference.md#12-errors)).

To tell your own errors apart, give them a name. `throw` takes only `Error`
objects, and a class cannot extend `Error`, so the name is a property: a small
function makes such errors, and a `catch` that handles only them throws the
others on.

```
function parseError(message) {
    const e = new Error(message);
    e.name = "ParseError";
    return e;
}

function parsePoint(text) {
    const parts = text.split(",");
    if (parts.length != 2) {
        throw parseError("expected x,y but got \"" + text + "\"");
    }
    return { x: Number(parts[0]), y: Number(parts[1]) };
}

for (const line : ["1,2", "oops"]) {
    try {
        print(parsePoint(line));
    }
    catch (e) {
        if (e.name != "ParseError") {
            throw e;            // not ours: pass it on
        }
        print(e);               // ParseError: expected x,y but got "oops"
    }
}
```
