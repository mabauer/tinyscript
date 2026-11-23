# Tinyscript Language Reference

Tinyscript is a small JavaScript-like scripting language. It is intentionally
minimal — think JavaScript in strict mode, minus the historic oddities, plus a
couple of deliberate design differences. If you know JavaScript, you can read
Tinyscript immediately; this document covers what's the same, what's different,
and all the details you need to write correct programs.

---

## Table of Contents

1. [Syntax basics](#1-syntax-basics)
2. [Types and values](#2-types-and-values)
3. [Variables and scope](#3-variables-and-scope)
4. [Operators](#4-operators)
5. [Statements](#5-statements)
6. [Functions](#6-functions)
7. [Objects](#7-objects)
8. [Arrays](#8-arrays)
9. [Classes and objects](#9-classes-and-objects)
10. [Closures](#10-closures)
11. [Built-in objects and functions](#11-built-in-objects-and-functions)
12. [Differences from JavaScript](#12-differences-from-javascript)
13. [Modules](#13-modules)

---

## 1. Syntax Basics

### Comments

```
// single-line comment

/* multi-line
   comment */
```

### Statements and semicolons

Statements end with `;`. Unlike JavaScript's automatic semicolon insertion,
Tinyscript follows Java-like rules: **the semicolon is required** at the end of
expression statements, variable declarations, and return statements.

```
const x = 42;
print(x);
return x;
```

Blocks (`{ }`) and control structures do not take a semicolon after the closing
brace.

### Identifiers

Identifiers follow the usual rules: start with a letter or `_`, followed by
letters, digits, or `_`. The prefix `__ts` is reserved for the cross-compiler's
internal shims and must not be used in user code.

---

## 2. Types and Values

Tinyscript has six types:

| Type        | Examples                        | Notes                                                                          |
| ----------- | ------------------------------- | ------------------------------------------------------------------------------ |
| `number`    | `0`, `3.14`, `1e6`, `-Infinity` | All numbers are 64-bit IEEE 754 doubles                                        |
| `string`    | `"hello"`, `'world'`            | Single or double quotes; standard escape sequences                             |
| `boolean`   | `true`, `false`                 |                                                                                |
| `null`      | `null`                          | Intentional absence of a value                                                 |
| `undefined` | `undefined`                     | Value of uninitialized variables and missing properties; a literal like `null` |
| `object`    | `{ }`, `[ ]`, functions         | All objects and arrays; functions are objects                                  |

Unlike Javascript, where `undefined` is a global variable (which a function can
even redeclare), `undefined` is a literal like `null`: it cannot be declared,
assigned or shadowed (`let undefined = 1;` is an error). To check for "no
value", `x == null` is the usual test: it is also `true` for `undefined`.

### Conditions

A condition (`if`, `while`, `assert`, `!`, `&&`, `||`, the callback of `filter`)
must be:

- a boolean,
- `null` or `undefined`, which count as `false`, or
- an object (including arrays and functions), which counts as `true`.

Numbers and strings are a `TypeError` ("Condition must be a boolean, not
number"): in Javascript `0`, `""` and `NaN` are false although they are valid
values, a common source of bugs. So `while (node)` and `if (o.child)` work,
`if (count)` must be written `if (count > 0)`, `if (name)` as `if (name != "")`,
and `if (i = 0)` (a typo for `==`) is an error. `&&` and `||` give `true` or
`false`, not one of their operands (see Logical).

### `typeof`

```
typeof x        // "undefined", "boolean", "number", "string", "function", or "object"
typeof null     // "object"  (matches ECMAScript)
typeof f()      // works on any operand: calls, property accesses, a[0], (1 + 2)
typeof 1 + "x"  // "numberx": typeof binds tighter than binary operators
```

`typeof` is a unary operator like `-` and `!`. Unlike Javascript, `typeof` on an
undeclared name is not `"undefined"` but a compile-time error, like any other
use of an undeclared name. A property access on `undefined` (`typeof o.x.y` when
`o.x` is missing) is a `TypeError`, as in Javascript.

---

## 3. Variables and Scope

### Declaration

Variables must be declared before use: an undeclared name, or one that is only
declared further down in the same block (`print(x); let x = 1;`), is an error
before the script runs. A function may use a variable declared after it, since
it runs later; if it is called before the declaration has run, that is a
`ReferenceError` ("x is used before its declaration"), as in Javascript.

```
let x;          // declared, value is undefined
let y = 10;
let a = 1, b = 2, c = a + b;   // multiple declarations in one statement
const limit = 100;             // a constant (see below)
```

A variable, function, parameter or class with the name of a builtin (`range`,
`print`, `Number`, …) hides the builtin in its scope; that is allowed, as in
Javascript, but the compiler warns, since `for (const range : spans)` makes
`range(n)` fail further down.

Variables are block-scoped like Javascript's `let`. There is no `var` (in
Javascript it is function-scoped and hoisted): it is a reserved word, and
`var x = 1;` is a syntax error that says to use `let`.

`const` declares a constant: it needs a value and cannot be assigned again (with
`=`, `+=` etc., `++` / `--`, or as the existing variable of a `for` loop). As in
Javascript, the object or array a constant refers to can still be changed:

```
const limit = 100;
const seen = new Set();
seen.add(1);          // fine: seen still refers to the same Set
for (const x : [1, 2, 3]) { ... }
// limit = 200;       // error: Cannot assign to the constant limit
```

### Destructuring

A declaration can take apart an array or an object; only flat patterns of names
are supported (no nesting, defaults or renaming). An array pattern can end with
a rest element that gets the other elements as an array:

```
const [quotient, remainder] = divide(17, 5);   // the first elements of an array
const [header, ...rows] = lines;               // the first element and the others
const { row, col } = position;                 // properties of an object
for (const [key, value] : map) { ... }         // in an iterable for loop
robots.map(({ x, y }) => x * y);               // as parameters of functions and arrow functions
```

Stricter than Javascript, an array pattern needs an array with at least as many
elements as it has names before the rest (`const [a, b] = [1]` is a `TypeError`;
more elements are fine, the rest can be empty). Object patterns have no rest
element. An object pattern needs an object (or a string); like `o.x`, a missing
property gives `undefined`.

### Block scope

Blocks create real scopes. A variable declared inside a block is not visible
outside it:

```
{
    const inner = "only visible here";
}
// inner is not visible here: a compile-time error (Unknown name 'inner')
```

**No hoisting:** Unlike JavaScript's `var`, Tinyscript variables are
block-scoped and are not hoisted. You cannot use a variable before the line
where it is declared.

### Function scope

Each function call creates a new scope. Variables declared inside a function are
not visible outside it.

### `this`

Inside a regular function called as a method (`obj.method()`), `this` refers to
`obj`; called as a plain function (`f()`), `this` is `undefined`, as in
Javascript's strict mode. Arrow functions inherit `this` from their enclosing
lexical scope (see [Arrow functions](#arrow-functions)).

Outside a function, `this` is `undefined`, as in a Javascript module; so the
compiler reports `this` there as an error, also in an arrow function written
there:

```
print(this);                               // error
const counter = { n: 0, get: () => this.n }; // error: use get: function() { … }
```

Unlike in a Javascript script, there is no way to reach the global object (with
`Math`, `print`, …): builtins cannot be replaced, neither by a script nor by a
module it imports.

---

## 4. Operators

### Arithmetic

| Operator | Operation                                                                               |
| -------- | --------------------------------------------------------------------------------------- |
| `+`      | Addition (numbers), concatenation (a string and any value), concatenation of two arrays |
| `-`      | Subtraction                                                                             |
| `*`      | Multiplication                                                                          |
| `/`      | Division                                                                                |
| `%`      | Remainder                                                                               |
| `-x`     | Unary negation                                                                          |

Operators do not convert their operands: `-`, `*`, `/`, `%` and unary `-` need
numbers, and `+` adds two numbers, two arrays, or a string and a value; anything
else (`1 + true`, `1 - "a"`, `undefined + 1`) is a `TypeError` instead of a
silent `NaN`. `NaN` is a number, so `NaN - 1` is `NaN`.

With a string, `+` converts the other value to a string: `undefined` and `null`
as `"undefined"` / `"null"`, an object with a `toString` method written in
tinyscript by calling it, other values in their usual form. tinyscript does not
support `valueOf` (it would convert objects implicitly in operators).

```
let arr = ["a", "b"];
arr = arr + ["c", "d"];      // ["a", "b", "c", "d"]
print("n = " + 42);          // n = 42
```

### Bitwise

| Operator                   | Operation                                            |
| -------------------------- | ---------------------------------------------------- |
| `a & b`, `a \| b`, `a ^ b` | Bitwise and, or, exclusive or                        |
| `~a`                       | Bitwise not (`~a` is `-a - 1`)                       |
| `a << n`, `a >> n`         | Shift left / right by `n` bits (`>>` keeps the sign) |

Unlike Javascript, whose bitwise operators work on 32 bits and cut off larger
numbers silently, tinyscript's work on integers up to 2^53 (negative ones in
two's complement), so `1099511627776 ^ 1` (2^40 ^ 1) is `1099511627777`.
Anything else is a `TypeError`: a fraction, a number of 2^53 or more, a shift
count outside 0 to 53, or a `<<` whose result needs more than 53 bits. There is
no `>>>`. The compound assignments `&=`, `|=`, `^=`, `<<=` and `>>=` work as for
the other operators.

```
const flags = 4 | 1;          // 5
print(flags & 4);             // 4
print(-8 >> 1);               // -4
```

### Comparison

| Operator     | Meaning                                            |
| ------------ | -------------------------------------------------- |
| `==`         | Equality (no type coercion: `1 == "1"` is `false`) |
| `!=`         | Inequality                                         |
| `<`          | Less than                                          |
| `<=`         | Less than or equal                                 |
| `>`          | Greater than                                       |
| `>=`         | Greater than or equal                              |
| `instanceof` | Prototype chain check                              |

`<`, `<=`, `>`, `>=` compare two numbers or two strings (lexicographically).
Comparing values of different or other types is a `TypeError`.

### Logical

| Operator | Meaning                                                        |
| -------- | -------------------------------------------------------------- |
| `&&`     | Logical AND                                                    |
| `\|\|`   | Logical OR                                                     |
| `!`      | Logical NOT                                                    |
| `??`     | `a ?? b` is `b` if `a` is `null` or `undefined`, otherwise `a` |

Unlike Javascript, `&&` and `||` always give a boolean, and their operands must
be conditions (see Conditions). So the Javascript idioms that use them for a
value do not work:

```
const cfg = options || {};        // Javascript: options, or {}; tinyscript: true
const name = user && user.name;   // Javascript: the name; tinyscript: true or false
```

Use `??` for a default value (`options ?? {}`, `counts.get(key) ?? 0`) and `?.`
for a property of an object that may be `null` (`user?.name`). An operand that
can never be a condition (an object, array or function literal, `new …`, a
number or a string) is a compile-time error that says so. As in Javascript, `b`
is only evaluated if needed, and `??` cannot be combined with `&&` or `||`
without parentheses: write `(a ?? b) || c`.

### Assignment

```
x = value;
x += 2;      // x = x + 2, also -=, *=, /=, %=
a[i()] += 1; // a and i() are evaluated only once
```

Assignment is an expression — it returns the assigned value — so it can appear
on the right side of another assignment. Only variables and properties can be
assigned to. `+=` follows the rules of `+` (numbers, strings, arrays), the
others need numbers.

### Increment and decrement

```
i++;
counts[c]--;
```

`++` and `--` add or subtract 1. Unlike Javascript, they are only statements:
`x = i++`, `f(i++)` and the prefix forms `++i` / `--i` are syntax errors. The
value must be a number.

### Conditional operator

```
const sign = x > 0 ? 1 : x < 0 ? -1 : 0;
```

`cond ? a : b` evaluates `a` if `cond` is true, otherwise `b`; the condition
follows the rules for conditions. Unlike Javascript, `b` cannot be an
assignment: write `c ? a : (b = 1)`.

### Operator precedence (high to low)

1. `!`, `~`, unary `-`
2. `*`, `/`, `%`
3. `+`, `-`
4. `<<`, `>>`
5. `<`, `<=`, `>`, `>=`, `instanceof`
6. `==`, `!=`
7. `&`
8. `^`
9. `|`
10. `&&`
11. `||`
12. `??`
13. `? :` (right-associative)
14. `=`, `+=`, `-=`, `*=`, `/=`, `%=`, `&=`, `|=`, `^=`, `<<=`, `>>=`
    (right-associative)

### `assert`

```
assert condition;
```

Throws an error if `condition` is `false` (or `null` / `undefined`; numbers and
strings are a `TypeError`, see Conditions). A failed `assert` is a programming
error: `catch` does not catch it. Useful for quick sanity checks during
development:

```
assert fibonacci(7) == 21;
```

---

## 5. Statements

### Variable declaration

```
let name;
let name = expression;
let a = 1, b = 2, c = 3;
const name = expression;
```

### Expression statement

Any expression followed by `;` can be used as a statement. The most common use
is function calls and assignments:

```
print("hello");
x = x + 1;
obj.method(arg);
```

### Block

A block groups statements and introduces a new scope:

```
{
    const local = 1;
    print(local);
}
```

### `if` / `else`

Braces are **required** around both branches:

```
if (condition) {
    // then
}

if (condition) {
    // then
} else {
    // else
}
```

Several conditions are chained with `else if`; only the last `else` needs no
condition:

```
if (x < 0) {
    print("negative");
} else if (x == 0) {
    print("zero");
} else {
    print("positive");
}
```

### `for`

Iterates over the elements of an array (values, not indices), the characters of
a string, the values of a `Set`, the entries `[key, value]` of a `Map` or the
numbers of a `range`; anything else is a `TypeError`:

```
for (const element : array) { ... }
for (const c : "abc") { ... }
for (const i : range(n)) { ... }              // 0, 1, …, n - 1
for (const i : range(1, 11)) { ... }          // 1, 2, …, 10
for (const i : range(10, 0, -2)) { ... }      // 10, 8, 6, 4, 2
for (const i : range(array.length)) { ... }   // the indexes of an array
for (const i : range(array.length).reverse()) { ... }   // … backwards
```

There is no counting loop with a condition: `for (let i = 0; i < n; i++)` (and
the Lua-style `for (let i = 0, n - 1)`, which tinyscript had until 0.22.0) is a
syntax error that points to `range`. The loop variable is declared with `const`
(or `let`, if the body assigns it; it is a new variable in each iteration) or is
an existing variable: `for (i : items)`.

A loop over an array sees elements that its body appends; a loop over a string,
`Set` or `Map` goes through the elements it had when the loop started.

```
const fruits = ["apple", "banana", "cherry"];
for (const fruit : fruits) {
    print(fruit);   // apple, then banana, then cherry
}
```

To iterate over object keys, combine with `Object.keys()`:

```
const person = { name: "Alice", age: 30 };
for (const key : Object.keys(person)) {
    print(key + " = " + person[key]);
}
```

### `while`

Runs the block as long as the condition is `true` (see Conditions). Braces are
required:

```
let i = 0;
while (i < 3) {
    print(i);
    i = i + 1;
}
```

In sandboxed mode every iteration counts against the statement limit, so even
`while (true) { }` terminates with a resource limit violation.

### `break`

Exits the innermost enclosing `for` or `while` loop. `break` is only allowed
inside a loop of the same function (a function defined inside a loop cannot
break out of it):

```
for (const i : range(101)) {
    if (i == 10) {
        break;
    }
    print(i);
}
```

### `continue`

Skips the rest of the current iteration of the innermost enclosing `for` or
`while` loop and goes on with the next one. Like `break`, it is only allowed
inside a loop of the same function:

```
for (const i : range(1, 11)) {
    if (i % 2 == 0) {
        continue;
    }
    print(i);   // 1, 3, 5, 7, 9
}
```

### `throw`

Throws an error: an `Error` object (see [Errors](#errors)). Unlike Javascript,
only `Error` objects can be thrown; `throw "text";` is a `TypeError`. Unless a
`try` / `catch` catches it, the error ends the script; the REPL prints it with
the stack trace as `TinyscriptError: message`.

```
function parse(line) {
    if (!line.includes(",")) {
        throw new Error("expected two numbers: " + line);
    }
    return line.split(",").map(Number);
}
```

### `try` / `catch`

```
try {
    const point = parse(line);
    print(point);
}
catch (e) {
    print("skipped: " + e.message);
}
```

If the `try` block throws an error that can be caught, the rest of the block is
skipped and the `catch` block runs, with the error as `e`: always an `Error`
object with `e.name` and `e.message`. `e` is a variable of the `catch` block (it
can be assigned, as in Javascript). Without the variable, `catch { … }` ignores
the error. There is no `finally`.

Only the errors a script can expect are caught:

- errors thrown with `throw`;
- failed input / output of `std:fs` (a missing file, a directory that cannot be
  listed, …) and `std:io`: an `Error` named `IOError`, e.g. `e.message` is
  `Cannot read file "data.txt": not found`.

Everything else is a programming error and ends the script, also inside `try`: a
`TypeError` (wrong types, wrong arguments of a builtin, accessing a property of
`null`), a failed `assert`, a stack overflow, and a violated resource limit in
sandboxed mode. This is stricter than Javascript, where `catch` gets every
error; a broad `catch (e) { return null; }` cannot hide a typo.

`throw e;` in a `catch` block throws the caught error again, with its original
place and stack trace (unless its message was changed). `return`, `break` and
`continue` leave a `try` or `catch` block as any other block.

### Kinds of errors

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

### `return`

Returns a value from a function. A `return` without an expression returns
`undefined`. `return` at global (top-level) scope is a validation error.

```
function max(a, b) {
    if (a > b) {
        return a;
    }
    return b;
}
```

### Empty statement

A lone `;` is a valid no-op statement.

---

## 6. Functions

### Function declaration

Function declarations are hoisted to the top of their enclosing block, so they
can be called before the declaration appears in the source. The name of a
function declaration cannot be assigned (to replace a function, declare it with
`let f = …`).

```
function add(a, b) {
    return a + b;
}

print(add(3, 4));   // 7
```

### Function expression

Functions can be assigned to variables or passed as values. An optional name can
be given for recursion:

```
const square = function(x) {
    return x * x;
};

// Named function expression (name is only in scope inside the function body)
const factorial = function fact(n) {
    if (n <= 1) { return 1; }
    return n * fact(n - 1);
};
```

### Arrow functions

Arrow functions provide a concise syntax and capture `this` from their
surrounding lexical scope.

**Expression body** (implicit return):

```
const double = x => x * 2;
const add = (a, b) => a + b;
```

**Block body** (explicit `return` required; without it, the result is
`undefined`):

```
const greet = name => {
    let msg = "Hello, " + name;
    return msg;
};
```

As in Javascript, a `{` after `=>` always starts a block body; to return an
object, put it in parentheses: `x => ({ value: x })`. The body can be another
arrow function: `const add = x => y => x + y;`.

Parentheses around parameters are optional for a single parameter, required for
zero or multiple:

```
const noArgs = () => 42;
const oneArg = x => x + 1;
const twoArgs = (x, y) => x + y;
```

### Default parameters

A parameter can have a default, used when the argument is missing or `undefined`
(not `null`), as in Javascript:

```
function slice(items, start = 0, end = items.length) {
    return items.slice(start, end);
}

class Point {
    constructor(x = 0, y = 0) {
        this.x = x;
        this.y = y;
    }
}

const scale = (v, factor = 2) => v * factor;
```

The default is evaluated at each call (`function add(x, list = [])` gets a new
array every time) and can use the parameters before it, not itself or later
ones. Stricter than Javascript, parameters with defaults come last
(`function f(a = 1, b)` is an error: a caller would have to pass `undefined` to
get the default), and only a name can have a default, not a pattern. A
function's `length` counts the parameters before the first default.

### Rest parameters

The last parameter can be a rest parameter, collecting all extra arguments into
an array:

```
function sum(...nums) {
    let total = 0;
    for (const n : nums) {
        total = total + n;
    }
    return total;
}

print(sum(1, 2, 3, 4));   // 10
```

### Calling functions

```
functionName(arg1, arg2)
object.method(arg)
object["method"](arg)
```

Missing arguments are `undefined` (or get their default). Too many arguments are
an error before the script runs where the called function is known: a function
declaration, a `const` with a function (`const f = (x) => …`), and `new` of a
class or constructor function of the same file (`new Point(1, 2, 3)`: "new Point
takes 2 arguments, not 3"); not with a rest parameter. Elsewhere, extra
arguments are ignored, as in Javascript: methods (`p.len(1, 2)`; the type of `p`
is known only at run time) and functions as values, e.g. callbacks, which often
get more arguments than they use (`map` passes `(item, index, array)`).

### Spread

`...items` in a call or an array literal puts in the elements of `items` one by
one. It takes what the `for` loop goes through: an array, the characters of a
string, the values of a `Set`, the `[key, value]` entries of a `Map` or the
numbers of a range; anything else is a `TypeError`.

```
Math.max(...values)          // the largest of the values
list.push(...more)           // appends the elements of more
new Point(...coords)
const all = [...a, x, ...b]; // a new array
[...range(3)]                // [0, 1, 2]
```

Each element becomes an argument, so in the generated Javascript (node) a call
with a very large array (from about 100,000 elements, depending on the engine)
fails with "Maximum call stack size exceeded"; the interpreter has no such
limit. For the largest or smallest element of a large array, use `max` / `min`
of `std:array`. Spread in object literals (`{ ...o }`) is not supported: copy
with `Object.assign({}, o)`.

### `new` — constructor calls

Any function can be used as a constructor with `new`. Inside the constructor,
`this` refers to the newly created object. The constructor's return value is
ignored; the new object is returned.

```
function Point(x, y) {
    this.x = x;
    this.y = y;
}

const p = new Point(3, 4);
print(p.x);   // 3
```

### `Function.prototype.call` and `.apply`

```
fn.call(thisArg, arg1, arg2)
fn.apply(thisArg, [arg1, arg2])
```

These call `fn` with an explicit `this` value. `.apply` takes the arguments as
an array.

---

## 7. Objects

### Object literal

```
const person = {
    name: "Alice",
    age: 30,
    greet() {
        print("Hello, I'm " + this.name);
    }
};
```

Property keys can be identifiers or string literals. Values can be any
expression. As in Javascript, a comma after the last property is allowed
(`{ a: 1, }`), which helps in literals over several lines. There are two short
forms:

- `greet() { … }` is short for `greet: function() { … }`, with the same
  parameters (also `...rest` and patterns) and the same `this`. In a stack trace
  the function is named after its key, as is `greet: function() { … }`.
- `{ row, col }` is short for `{ row: row, col: col }` (the counterpart of the
  object pattern `const { row, col } = position;`).

The properties keep the order in which they were added (`Object.keys`, `print`),
except that integer keys come first in ascending order, as in Javascript:
`{ b: 1, "7": 2, "10": 3 }` lists `7`, `10`, `b`.

How to choose between object literals, factory functions and classes for objects
with behavior: see [Classes and objects](#9-classes-and-objects).

### Property access

```
person.name           // dot notation
person["name"]        // bracket notation (key is a string expression)
person["na" + "me"]   // any string expression works
```

**Optional chaining:** `a?.b`, `a?.[key]` and `a?.m()` are `undefined` if `a` is
`null` or `undefined`, instead of a `TypeError`. As in Javascript, the rest of
the chain is skipped: `a?.b.c` is `undefined` if `a` is `null` (`.c` is not
evaluated). An optional chain cannot be assigned to (`a?.b = 1` is an error).

```
const list = { value: 1, next: null };
print(list.next?.value ?? "end");   // end
```

### Property assignment

```
person.age = 31;
person["job"] = "Engineer";
person.newProp = "added dynamically";
```

New properties can be added to any object at any time.

### Checking property existence

```
person.hasOwnProperty("name")   // true — own property only
typeof person.missing           // "undefined" — missing property
```

### Deleting properties

`delete` removes a property of an object, as in Javascript; it gives `true`, and
deleting a missing property is fine:

```
delete person.job;
delete person[key];
```

Only properties of objects can be deleted, so `delete` is stricter than in
Javascript:

- The operand must be a property: `delete x` (a variable) or `delete o?.x` is a
  compile-time error.
- Arrays are lists without gaps: `delete a[i]` is a `TypeError`; remove an
  element with `a.splice(i, 1)`. For `Map`s and `Set`s use their method
  `delete(…)`; strings and ranges cannot be changed.
- Built-in properties (`Math.PI`, `f.prototype` …) cannot be deleted.

---

## 8. Arrays

### Array literal

```
const empty = [];
const nums = [1, 2, 3];
const mixed = ["hello", 42, true, null];
const lines = [
    "first",
    "second",     // a comma after the last element is allowed
];
```

Unlike Javascript, an array has no empty elements: `[1, , 2]` is a syntax error
(write `undefined`). A comma after the last element is only allowed in array and
object literals, not in calls, parameter lists or patterns.

### Element access and `length`

```
nums[0]                 // 1 (zero-based)
nums[nums.length - 1]   // 3, the last element
nums[10]                // undefined
nums[5] = 6;            // grows the array, the gap is filled with undefined
nums.length = 2;        // truncates the array to [1, 2]
nums.length = 4;        // grows it again: [1, 2, undefined, undefined]
```

To index from the end, use `nums.at(-1)`.

Arrays are lists: besides their items, only `length` can be set. Unlike
Javascript, writing any other key is a `TypeError`, including `nums.x = 1`,
`nums[-1] = 1` and `nums[1.5] = 1`; `Object.defineProperty` on an array is a
`TypeError` as well. Reading such a key gives `undefined`. An invalid `length`
(negative, fractional, not a number) is a `TypeError` (Javascript:
`RangeError`).

### Array methods

| Method                                       | Description                                                                                                                                                        |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `arr.at(i)`                                  | Element at index `i`; a negative `i` counts from the end (`arr.at(-1)` is the last element); `undefined` if out of range                                           |
| `arr.indexOf(x[, start])`                    | Index of the first element `== x` (tinyscript's `==`), or `-1`; searching from `start` (negative: counts from the end)                                             |
| `arr.includes(x[, start])`                   | `true` if an element `== x`, searching from `start` (negative: counts from the end)                                                                                |
| `arr.push(x)`                                | Appends `x` to the end; returns new element count                                                                                                                  |
| `arr.pop()`                                  | Removes and returns the last element                                                                                                                               |
| `arr.shift()`                                | Removes and returns the first element                                                                                                                              |
| `arr.unshift(x)`                             | Prepends `x`; returns new element count                                                                                                                            |
| `arr.slice([start[, end]])`                  | A new array with the elements from `start` (inclusive) to `end` (exclusive); negative indices count from the end; `arr.slice()` copies the array                   |
| `arr.fill(x[, start[, end]])`                | Sets the elements from `start` to `end` to `x`; returns the array                                                                                                  |
| `arr.join([sep])`                            | Returns a string with elements joined by `sep` (default `","`)                                                                                                     |
| `arr.forEach(fn)`                            | Calls `fn(element, index, array)` for each element                                                                                                                 |
| `arr.map(fn)`                                | Returns a new array of the results of `fn(element, index, array)`                                                                                                  |
| `arr.filter(fn)`                             | Returns a new array of the elements for which `fn(element, index, array)` is true                                                                                  |
| `arr.some(fn)` / `arr.every(fn)`             | Whether `fn` is true for some / every element                                                                                                                      |
| `arr.find(fn)` / `arr.findIndex(fn)`         | The first element / its index for which `fn` is true, or `undefined` / `-1`                                                                                        |
| `arr.reduce(fn[, initial])`                  | Combines the elements with `fn(accumulator, element, index, array)`, starting with `initial` (or the first element; then an empty array is a `TypeError`)          |
| `arr.sort([compare])`                        | Sorts the array in place (stable) and returns it; `compare(a, b)` returns a negative number if `a` comes first, a positive one if `b` does, `0` otherwise          |
| `arr.reverse()`                              | Reverses the array in place and returns it                                                                                                                         |
| `arr.toReversed()`                           | A reversed copy; the array stays as it is                                                                                                                          |
| `arr.splice(start[, count[, x, ...]])`       | Removes `count` elements from `start` (by default all to the end) and inserts `x, ...` there; returns the removed elements; a negative `start` counts from the end |
| `arr.lastIndexOf(x[, from])`                 | Index of the last element `== x` at or before `from` (negative: counts from the end), or `-1`                                                                      |
| `arr.findLast(fn)` / `arr.findLastIndex(fn)` | Like `find` / `findIndex`, searching from the end                                                                                                                  |
| `arr.flat([depth])`                          | A new array with the elements of nested arrays, `depth` levels deep (default `1`, `Infinity` for all)                                                              |
| `arr.flatMap(fn)`                            | Like `map`, but an array that `fn` returns adds its elements: `["a b", "c"].flatMap(s => s.split(" "))` is `["a", "b", "c"]`                                       |
| `arr.toString()`                             | The same text as `print` and `String(arr)`: `[1, "a"]` (Javascript: `1,a`)                                                                                         |

The callbacks must be functions; the results of the callbacks of `filter`,
`some`, `every`, `find`, `findIndex`, `findLast` and `findLastIndex` are
conditions (a boolean, `null` / `undefined` or an object, see
[Conditions](#conditions)).

Without a comparison function, `sort` only sorts strings; unlike Javascript,
which would sort numbers as strings (`[10, 9, 1].sort()` gives `[1, 10, 9]`),
sorting other values without one is a `TypeError`. Sort numbers with
`arr.sort((a, b) => a - b)`.

`new Array(n)` creates an array of `n` elements `undefined` (fill it with
`fill`); `new Array(a, b, ...)` or `new Array("x")` create an array of these
elements. Unlike Javascript, `new Array(n)` has no "holes": `map` and `forEach`
see all `n` elements.

```
const numbers = [1, 5, 4, 2, 9, 7];

const squares = numbers.map(x => x * x);
print(squares);   // [1, 25, 16, 4, 81, 49]

const odds = numbers.filter(x => x % 2 == 1);
print(odds);      // [1, 5, 9, 7]

print(numbers.join(", "));   // "1, 5, 4, 2, 9, 7"
print(numbers.reduce((sum, x) => sum + x, 0));   // 28
print(numbers.slice().sort((a, b) => a - b));     // [1, 2, 4, 5, 7, 9]

const grid = new Array(3).fill(0).map(() => new Array(3).fill("."));
```

### Array concatenation with `+`

The `+` operator has array-specific semantics:

```
const a = [1, 2];
const c = a + [3, 4];    // [1, 2, 3, 4] — concatenate arrays
const s = a + "!";       // "[1, 2]!"    — with a string: string concatenation
// a + 3 is a TypeError
```

### `Array.isArray`

```
Array.isArray([1, 2, 3])   // true
Array.isArray("hello")     // false
```

### Iterating

```
for (const x : array) { ... }                 // iterates values
for (const i : range(array.length)) { ... }   // iterates indexes
```

---

## 9. Classes and Objects

There are three ways to make objects with behavior. Choose by what you need:

| Form                                                 | Use it for                                          | Example                                              |
| ---------------------------------------------------- | --------------------------------------------------- | ---------------------------------------------------- |
| [Object literal](#object-literals)                   | a single object: settings, a record, a small helper | `{ x, y, length() { … } }`                           |
| [Factory function](#factory-functions-with-closures) | objects with private state, without `this`          | `counter()` returning `{ inc() { … }, get() { … } }` |
| [`class`](#classes)                                  | many objects of the same kind with shared methods   | `class Point { … }`                                  |

There is no inheritance between classes: code that several kinds of objects
share goes into a function, or one object uses another one (see
[Reuse without inheritance](#reuse-without-inheritance)). Objects of different
kinds can still be used alike, if they have the same methods (see
[Objects of different kinds used alike](#objects-of-different-kinds-used-alike)).
Under the hood, all of this is Javascript's prototypes (see
[Under the hood: prototypes](#under-the-hood-prototypes)).

### Object literals

A literal (see [Objects](#7-objects)) with methods is the simplest object with
behavior; `this` in a method is the object the method is called on:

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
methods can reach (see [Closures](#10-closures)). The methods use the variables
of the function, not `this`:

```
function counter(start) {
    let n = start;              // private: only inc and get see it
    return {
        inc() { n++; },
        get() { return n; }
    };
}

const c = counter(10);
c.inc();
print(c.get(), c.n);   // 11 undefined
```

Each call makes new state. Since the methods do not use `this`, they also work
when taken off the object (`const inc = c.inc; inc();`).

### Classes

A class describes many objects of the same kind: the `constructor` gives each
new object its fields, the methods are shared by all of them.

```
class Point {
    constructor(x, y) {
        this.x = x;
        this.y = y;
    }
    plus(other) { return new Point(this.x + other.x, this.y + other.y); }
    length() { return Math.hypot(this.x, this.y); }
}

const p = new Point(3, 4);
print(p.length(), p.plus(p));    // 5 Point {x: 6, y: 8}
print(p instanceof Point, Object.keys(p));   // true ["x", "y"]
```

- A class without `constructor` creates empty objects. `new Point(…)` always
  gives the new object, so a constructor cannot `return` a value.
- The methods are not fields of the objects: they are not in `Object.keys` and
  not printed. Parameters work as in functions (`...rest`, patterns). A field
  with the name of a method would hide it, so `this.area = …` in a class with a
  method `area()` is an error.
- `print` shows an object of a class with the class name (`Point {x: 3, y: 4}`),
  unless the class has a `toString` method; `print(Point)` shows
  `[class Point]`.
- A class can only be created with `new`: `Point(1, 2)` is a `TypeError`.
- Classes are declared at the top level of a file only, not in blocks or
  functions, and can be exported (`export class Point { … }`, see
  [Modules](#13-modules)). The name is a constant.
- Like `let` and `const`, a class can be used from its declaration on: functions
  and methods may mention a class declared further down, since they run later,
  but `new Point(1, 2)` at the top level before `class Point` is an error.
- Not supported: `extends` and `super`, `static` members (write a function or a
  constant next to the class), getters and setters, field declarations in the
  class body (fields get their values in the constructor), and interfaces
  (`interface` and `implements` are reserved words). The compiler says so with a
  hint.

### Methods and `this`

A method call `obj.method()` sets `this` to `obj`. An arrow function inside a
method takes `this` from the method, so it can use the fields:

```
class Basket {
    constructor(prices, discount) {
        this.prices = prices;
        this.discount = discount;
    }
    total() {
        return this.prices.map(price => price * (1 - this.discount)).reduce((a, b) => a + b, 0);
    }
}
print(new Basket([10, 20], 0.5).total());   // 15
```

A method taken off its object has no `this`: `const f = p.length; f();` is a
`TypeError`, since `this` is `undefined` in a plain call. To hand a method on,
wrap the call in an arrow function: `points.map(point => point.length())`. A
`function () { … }` written inside a method has its own `this` as well; use an
arrow function there.

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

### Under the hood: prototypes

Every object has a prototype: a property that the object does not have is looked
up in its prototype, then in the prototype's prototype, and so on. A class is a
constructor function whose `prototype` holds the methods:

```
class Pair {
    constructor(a, b) { this.a = a; this.b = b; }
    sum() { return this.a + this.b; }
}
const pair = new Pair(1, 2);
print(typeof Pair, Object.getPrototypeOf(pair) == Pair.prototype, pair.hasOwnProperty("sum"));
// function true false
```

`Object.create(proto)` makes a new object with `proto` as its prototype:

```
const base = { greet() { return "Hello from " + this.name; } };
const child = Object.create(base);
child.name = "child";
print(child.greet(), Object.getPrototypeOf(child) == base);   // Hello from child true
```

`x instanceof C` checks whether `C.prototype` is in the prototype chain of `x`.
`Object.getPrototypeOf(x)` gives the prototype; `x.__proto__` is the same.

Before `class`, objects of the same kind were made with a constructor function
and methods assigned to its `prototype`; older code reads like this:

```
function Car(engine) {
    this.engine = engine;
}
Car.prototype.describe = function() { return "Engine: " + this.engine; };
print(new Car("V8").describe());   // Engine: V8
```

Unlike a class, such a function can be called without `new` by mistake (then
`this` is `undefined` and the first assignment fails), and its objects print
without a name. Inheritance can be built from these parts
(`Object.create(Parent.prototype)` and `Parent.call(this, …)`, or plain objects
as classes), but it is not the way tinyscript recommends: use composition.
[`examples/oo/car.tys`](../examples/oo/car.tys) shows a constructor function
next to the same class, and
[`examples/oo/prototypes_as_classes.tys`](../examples/oo/prototypes_as_classes.tys)
inheritance with prototypes next to the same with classes and composition.

---

## 10. Closures

A closure is a function that retains access to variables from the scope in which
it was defined, even after that scope has exited.

```
function makeCounter() {
    let count = 0;
    return function() {
        count = count + 1;
        return count;
    };
}

const counter = makeCounter();
print(counter());   // 1
print(counter());   // 2
print(counter());   // 3
```

Each call to `makeCounter()` creates a fresh `count` variable, so closures from
different calls do not share state. A function that returns an object with
several such methods makes objects with private state (see
[Factory functions with closures](#factory-functions-with-closures)).

Two common uses: a function that remembers results (here in a `Map`, which only
the returned function can reach) and a function made from a parameter, e.g. a
comparison for `sort`:

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

To share functions and values between files, use modules (see
[Modules](#13-modules)).

---

## 11. Built-in Objects and Functions

### `print(value, ...)`

Writes its arguments to standard output, separated by spaces and followed by a
newline, as Javascript's `console.log` does. Strings appear as they are; other
values are converted as in `"" + value`: an object with its own `toString`
method shows what that method returns, also inside arrays and other objects;
otherwise arrays, objects, `Map`s and `Set`s show their contents. Strings inside
them appear as string literals, in double quotes and escaped as by Javascript's
`JSON.stringify`, so the output can be pasted back into a script.

```
print("hello");
print("x =", 42);           // x = 42
print([1, "a"]);            // [1, "a"]
print({ x: 1, y: [2] });    // {x: 1, y: [2]}

const p = { x: 1, toString() { return "P" + this.x; } };
print(p, [p]);              // P1 [P1]

const o = { name: "o" }; o.self = o;
print(o);                   // {name: "o", self: [Circular]}
```

`x.toString()` gives the same text as `print`, `String(x)` and `"" + x`, also
for objects and arrays without a `toString` of their own (`{x: 1}`, `[1, "a"]`;
Javascript gives `"[object Object]"` and `"1,a"`).
`Object.prototype.toString.call(x)` shows `x` without its own `toString`.

A reference back to an object that is still being printed shows as `[Circular]`;
nesting deeper than 50 levels shows as `[Object]` / `[Array]`. The generated
Javascript prints values the same way (not in `console.log`'s format).

### `range`

`range(end)` and `range(start, end[, step])` are the integers from `start`
(default `0`) up to, but not including, `end`, as in Python. A range is lazy: it
does not build an array, so `range(1000000000000)` costs nothing until a loop
goes through it.

```
for (const i : range(5)) { ... }         // 0, 1, 2, 3, 4
for (const i : range(2, 6)) { ... }      // 2, 3, 4, 5
for (const i : range(10, 0, -3)) { ... } // 10, 7, 4, 1
for (const i : range(5).reverse()) { ... } // 4, 3, 2, 1, 0
```

| Member         | Description                                                                                 |
| -------------- | ------------------------------------------------------------------------------------------- |
| `r.length`     | The number of integers                                                                      |
| `r.toArray()`  | An array of the integers: `range(3).toArray()` is `[0, 1, 2]`                               |
| `r.map(f)`     | An array of `f(i)` for each integer: `range(4).map(i => i * i)` is `[0, 1, 4, 9]`           |
| `r.forEach(f)` | Calls `f(i)` for each integer                                                               |
| `r.reverse()`  | The same integers backwards, again a lazy range: `range(5).reverse()` is `range(4, -1, -1)` |

The arguments must be integers, the step must not be `0`; anything else is a
`TypeError`. A range cannot be changed. The callbacks get `(i, index, range)`,
like those of arrays. Javascript has no `range`: the transpiler adds one.

### `Math`

| Member                                                                  | Description                                         |
| ----------------------------------------------------------------------- | --------------------------------------------------- |
| `Math.PI`                                                               | 3.141592653589793                                   |
| `Math.sqrt(x)`                                                          | Square root of `x`                                  |
| `Math.round(x)`                                                         | Round to nearest integer                            |
| `Math.random()`                                                         | Random number in [0, 1)                             |
| `Math.min(a, b, ...)` / `Math.max(a, b, ...)`                           | Smallest / largest of the arguments                 |
| `Math.abs(x)`                                                           | Absolute value                                      |
| `Math.floor(x)` / `Math.ceil(x)`                                        | Round down / up                                     |
| `Math.trunc(x)` / `Math.sign(x)`                                        | Integer part (towards zero) / sign (`-1`, `0`, `1`) |
| `Math.pow(x, y)`                                                        | `x` to the power of `y`                             |
| `Math.exp(x)`, `Math.log(x)`, `Math.log2(x)`, `Math.log10(x)`           | e to the power of `x`; logarithms                   |
| `Math.cbrt(x)`, `Math.hypot(a, b, ...)`                                 | Cube root; square root of the sum of squares        |
| `Math.sin(x)`, `cos`, `tan`, `asin`, `acos`, `atan`, `Math.atan2(y, x)` | Trigonometry (radians)                              |
| `Math.E`                                                                | 2.718281828459045                                   |

```
print(Math.sqrt(2));    // 1.4142135623730951
print(Math.round(2.7)); // 3
print(Math.PI);         // 3.141592653589793
print(Math.pow(2, 10), Math.log2(1024), Math.hypot(3, 4));   // 1024 10 5
```

The arguments must be numbers. `Math.round` rounds halves up, as in Javascript
(`Math.round(-2.5)` is `-2`). The results agree with node, the transcendental
functions (`sin`, `exp`, `pow` …) in rare cases only to the last digit but one.

### `Number`, `parseInt`, `isNaN`

Conversions to numbers are explicit (no operator or builtin converts silently):

| Function                                                 | Description                                                                                                                                                                                                                                                                                              |
| -------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Number(x)`                                              | A string read as a whole number literal (surrounding whitespace is ignored; decimal, `1e3`, `Infinity`, `0x1f`, `0o17`, `0b101`), otherwise `NaN`; unlike Javascript, `""` and blank strings are `NaN`, not `0`. A number stays as it is, `true` / `false` are `1` / `0`; anything else is a `TypeError` |
| `parseFloat(str)`                                        | The decimal number at the start of `str`, ignoring the rest (`"3.5kg"` → `3.5`), or `NaN`; `str` must be a string                                                                                                                                                                                        |
| `parseInt(str[, radix])`                                 | The integer at the start of `str`, ignoring the rest (`"42abc"` → `42`), or `NaN`; leading whitespace and a sign are allowed, `0x` means radix 16. `radix` must be an integer from 2 to 36                                                                                                               |
| `isNaN(x)`                                               | `true` if the number `x` is `NaN`; `x` must be a number                                                                                                                                                                                                                                                  |
| `Number.isInteger(x)`                                    | `true` if `x` is a finite number without fraction; `false` for anything else                                                                                                                                                                                                                             |
| `Number.isFinite(x)`                                     | `true` if `x` is a number other than `Infinity`, `-Infinity` and `NaN`; `false` for anything else                                                                                                                                                                                                        |
| `Number.MAX_SAFE_INTEGER`, `Number.MIN_SAFE_INTEGER`     | 2^53 - 1 and -(2^53 - 1), the integers that numbers represent exactly                                                                                                                                                                                                                                    |
| `Number.MAX_VALUE`, `Number.MIN_VALUE`, `Number.EPSILON` | The largest number, the smallest number above 0, and the distance from 1 to the next larger number                                                                                                                                                                                                       |
| `Infinity`, `NaN`                                        | The numbers infinity and "not a number", as in Javascript                                                                                                                                                                                                                                                |

```
print(Number(" 3.5 ") + 1);   // 4.5
print(isNaN(Number("12px"))); // true
print(parseInt("12px"));      // 12
print(parseInt("ff", 16));    // 255
```

Numbers print (and convert to strings) as in Javascript: with the shortest
digits that identify the number, written out from 10^-7 up to 10^21, otherwise
with an exponent; `-0` prints as `0`.

```
print(0.1 + 0.2);      // 0.30000000000000004
print(12345678.5);     // 12345678.5
print(1e21, 1e-7);     // 1e+21 1e-7
print(2 / 3);          // 0.6666666666666666
```

For another format, numbers have two methods, as in Javascript:

| Method                | Description                                                                                                                                                                                                                                                                                               |
| --------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `x.toFixed([digits])` | A string with `digits` decimal places (an integer from 0 to 100, default 0), rounded half away from zero on the exact binary value of `x`, as in Javascript (`(1.005).toFixed(2)` is `"1.00"`, because 1.005 is stored as 1.00499…). From 10^21 on, and for `NaN` and `Infinity`, the same as `String(x)` |
| `x.toString([radix])` | `x` in base `radix` (an integer from 2 to 36, default 10); other bases than 10 only for integers                                                                                                                                                                                                          |

```
const price = 1234.5;
print(price.toFixed(2));                  // 1234.50
print((2 / 3).toFixed(3));                // 0.667
print(price.toFixed(2).padStart(10));     // "   1234.50": columns for tables
print((255).toString(16), (5).toString(2).padStart(8, "0"));   // ff 00000101
```

A number literal needs parentheses, as in Javascript: `(5).toFixed(2)`. Wrong
arguments (`toFixed(1.5)`, `toFixed("2")`, `toString(37)`, `(1.5).toString(2)`)
are a `TypeError`. Unlike Javascript, numbers have no other properties:
`n.length` or `n.x = 1` is a `TypeError`, so that mixing up a number and an
array or object is noticed at once.

### `Object`

| Method                                       | Description                                                                       |
| -------------------------------------------- | --------------------------------------------------------------------------------- |
| `Object.create(proto)`                       | New object with given prototype                                                   |
| `Object.keys(obj)`                           | Array of own enumerable property keys                                             |
| `Object.values(obj)` / `Object.entries(obj)` | Array of their values / of `[key, value]` pairs                                   |
| `Object.fromEntries(pairs)`                  | New object from `[key, value]` pairs (an array of them or a `Map`)                |
| `Object.assign(target, source, ...)`         | Copies the own enumerable properties of the sources to `target`; returns `target` |
| `Object.getPrototypeOf(obj)`                 | Returns `obj`'s prototype                                                         |
| `Object.defineProperty(obj, key, desc)`      | Define a property with a descriptor                                               |
| `Object.getOwnPropertyDescriptor(obj, key)`  | Returns a property descriptor                                                     |
| `Object.getOwnPropertyNames(obj)`            | Array of all own property names                                                   |
| `obj.hasOwnProperty(key)`                    | `true` if `key` is an own (non-inherited) property                                |

```
const proto = { greet: function() { print("hi"); } };
const obj = Object.create(proto);
obj.name = "Alice";

print(Object.keys(obj));             // ["name"]
print(Object.getPrototypeOf(obj) == proto);  // true
obj.greet();                         // "hi" (inherited)
```

### `String`

String primitives have methods available via auto-boxing. Numbers only have
`toFixed` and `toString` (see [`Number`](#number-parseint-isnan)); booleans have
no properties at all: `flag.x` is a `TypeError`. Convert with `String(x)` or
`"" + x`.

String methods:

| Method                                                       | Description                                                                                                                                                          |
| ------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `str.at(i)`                                                  | Character at index `i`; a negative `i` counts from the end; `undefined` if out of range                                                                              |
| `str.charAt(i)`                                              | Character at index `i`                                                                                                                                               |
| `str.charCodeAt(i)`                                          | UTF-16 code of the character at index `i`, or `NaN`                                                                                                                  |
| `str.indexOf(sub[, pos])`                                    | First index of `sub` at or after `pos` (negative: 0), or `-1`                                                                                                        |
| `str.lastIndexOf(sub[, pos])`                                | Last index of `sub` at or before `pos`, or `-1`                                                                                                                      |
| `str.includes(sub[, pos])` / `str.startsWith(sub[, pos])`    | Whether `str` contains `sub` at or after `pos` / starts with `sub` at `pos`                                                                                          |
| `str.endsWith(sub[, end])`                                   | Whether `str` up to `end` ends with `sub`                                                                                                                            |
| `str.substring(start, end)`                                  | Substring from `start` (inclusive) to `end` (exclusive)                                                                                                              |
| `str.slice(start[, end])`                                    | Like `substring`, but negative indices count from the end, and `start > end` gives `""`                                                                              |
| `str.split(sep[, limit])`                                    | Array of the parts between occurrences of `sep` (at most `limit` of them); `""` splits into characters. To split at any whitespace, use `words(str)` of `std:string` |
| `str.trimStart()` / `str.trimEnd()`                          | Without leading / trailing whitespace                                                                                                                                |
| `str.codePointAt(i)`                                         | Unicode code point at index `i` (both halves of an emoji), or `undefined`                                                                                            |
| `str.trim()`                                                 | Without leading and trailing whitespace                                                                                                                              |
| `str.replace(search, repl)` / `str.replaceAll(search, repl)` | Replaces the first / every occurrence of `search`; `repl` may contain `$&` (the match), `` $` `` / `$'` (the text before / after it) and `$$`                        |
| `str.repeat(n)`                                              | `str` repeated `n` times                                                                                                                                             |
| `str.padStart(len[, pad])` / `str.padEnd(len[, pad])`        | Padded at the start / end with spaces or `pad` up to length `len`                                                                                                    |
| `str.toUpperCase()` / `str.toLowerCase()`                    | Upper / lower case                                                                                                                                                   |
| `str.toString()`                                             | The string itself                                                                                                                                                    |

String arguments must be strings and indices integers, otherwise the method
throws a `TypeError`. `search` and `sep` are plain strings; there are no regular
expressions.

All builtins (methods of strings, arrays, `Map`, `Set`, `Math`, …) understand
the optional arguments of their Javascript counterparts as listed here; calling
one with more arguments than it takes is a `TypeError`
(`"abc".startsWith("a", 0, 1)`: "startsWith() takes at most 2 arguments, not
3"), where Javascript would ignore them. A builtin used as a callback, e.g.
`lines.map(Number)`, gets `(item, index, array)` like any callback and uses what
it needs. For your own functions, see [Calling functions](#calling-functions).

`String.fromCharCode(c1, c2, ...)` builds a string from UTF-16 codes.

```
const s = "Hello World";
print(s.substring(0, 5));   // "Hello"
print(s.indexOf("World"));  // 6
print(s.charAt(0));         // "H"
print("a,b,c".split(",").length); // 3
print("7".padStart(3, "0")); // "007"
```

### `Array`

See [Arrays](#8-arrays) for method details. Additional static:

| Method                 | Description                           |
| ---------------------- | ------------------------------------- |
| `Array.isArray(value)` | Returns `true` if `value` is an array |

### `Map` and `Set`

As in Javascript, keys of a `Map` and values of a `Set` can be of any type:
numbers, strings and booleans are compared by value, objects by identity (`NaN`
equals `NaN`). Both keep the insertion order.

| `Map`                                         | Description                                                    |
| --------------------------------------------- | -------------------------------------------------------------- |
| `new Map()` / `new Map([[k1, v1], [k2, v2]])` | An empty map / a map with these entries                        |
| `m.get(key)`                                  | The value for `key`, or `undefined`                            |
| `m.set(key, value)`                           | Sets the value for `key`; returns `m`, so calls can be chained |
| `m.has(key)`                                  | Whether `m` has an entry for `key`                             |
| `m.delete(key)`                               | Removes the entry for `key`; returns whether there was one     |
| `m.clear()`                                   | Removes all entries                                            |
| `m.size`                                      | The number of entries                                          |
| `m.keys()` / `m.values()` / `m.entries()`     | Arrays of the keys / values / `[key, value]` pairs             |
| `m.forEach(fn)`                               | Calls `fn(value, key, map)` for each entry                     |

| `Set`                           | Description                                                              |
| ------------------------------- | ------------------------------------------------------------------------ |
| `new Set()` / `new Set(values)` | An empty set / a set of the elements of an array, string, `Set` or `Map` |
| `s.add(value)`                  | Adds `value`; returns `s`                                                |
| `s.has(value)`                  | Whether `s` contains `value`                                             |
| `s.delete(value)`               | Removes `value`; returns whether it was there                            |
| `s.clear()`                     | Removes all values                                                       |
| `s.size`                        | The number of values                                                     |
| `s.values()`                    | An array of the values                                                   |
| `s.forEach(fn)`                 | Calls `fn(value, value, set)` for each value                             |

Differences from Javascript: `keys()`, `values()` and `entries()` return arrays,
not iterators, so `m.keys().length`, `m.keys().sort()` etc. work. A `Map` or
`Set` has no other properties: `m[key] = value` (instead of `m.set(key, value)`)
is a `TypeError`.

```
const counts = new Map();
for (const word : "a b a".split(" ")) {
    counts.set(word, (counts.get(word) ?? 0) + 1);
}
print(counts.get("a"));      // 2

const visited = new Set();
visited.add(1 + "," + 2);
print(visited.has("1,2"));   // true
```

### `Function.prototype`

| Method                         | Description                                      |
| ------------------------------ | ------------------------------------------------ |
| `fn.call(thisArg, arg1, ...)`  | Call `fn` with explicit `this`                   |
| `fn.apply(thisArg, argsArray)` | Call `fn` with explicit `this` and array of args |

### `System`

| Method                       | Description                                                                                                                                   |
| ---------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| `System.currentTimeMillis()` | Current wall-clock time in milliseconds (like `Date.now()`)                                                                                   |
| `System.args`                | The arguments of the script, an array of strings: in the REPL, the arguments after `--` (`tinyscript day1.tys -- input.txt`); otherwise empty |

```
const start = System.currentTimeMillis();
// ... do work ...
print(System.currentTimeMillis() - start);   // elapsed ms

const file = System.args.length > 0 ? System.args[0] : "input.txt";
```

### Errors

Calling something that is not a function is a `TypeError` that names it:
`String has no method 'splitt'`, `'x' of Object is a number, not a function`,
`'f' is undefined, not a function`.

`new Error(message)` (or `Error(message)`) creates an error object for `throw`:
`e.message`, `e.name` (`"Error"`) and `e.toString()` (`"Error: message"`) as in
Javascript; the message must be a string. There are no subclasses like
`TypeError` and no stack property. `catch` gets thrown errors and failed input /
output (`e.name` is `"IOError"`), not programming errors (see
[`try` / `catch`](#try--catch)).

A too deep (or endless) recursion is a `TinyscriptStackOverflowError` ("Stack
overflow: too many nested calls"); its stack trace shows only the innermost and
outermost calls. The REPL allows a recursion depth of about 500,000 calls; in
the web demo the limit is lower.

### File access

There is no global `fs` (since 0.18): file access is the module `std:fs` (see
[Modules](#13-modules)), `import * as fs from "std:fs";`. It is not available in
the web demo or any sandboxed execution context. The standard input (keyboard or
pipe) is the module `std:io`.

---

## 12. Differences from JavaScript

Understanding where Tinyscript diverges from JavaScript is important both for
writing code and for knowing its intentional limitations.

### Things Tinyscript does differently

| Topic                        | JavaScript                                               | Tinyscript                                                                 |
| ---------------------------- | -------------------------------------------------------- | -------------------------------------------------------------------------- |
| **Semicolons**               | Optional (ASI)                                           | Required after expression statements, declarations, `return`               |
| **Block scope**              | `var` is function-scoped; `let`/`const` are block-scoped | All variables are block-scoped; there is no `var` (a reserved word)        |
| **Control structure bodies** | Braces optional for single statements                    | Braces always required                                                     |
| **Counting `for` loop**      | `for (let i = 0; i < n; i++)`                            | `for (const i : range(n))`; backwards `for (const i : range(n).reverse())` |
| **Iterable `for` loop**      | `for (const x of array)`                                 | `for (const x : array)`                                                    |
| **`for...in`**               | Iterates over keys                                       | Not supported: `for (const key : Object.keys(obj))`                        |
| **Array `+`**                | String concatenation                                     | Concatenation of two arrays; an array plus a number etc. is a `TypeError`  |
| **`==` vs `===`**            | Different (type coercion vs. strict)                     | Only `==` / `!=`, which never coerce types (there is no `===` / `!==`)     |
| **Relational operators**     | Coerce operands (`1 < "2"`)                              | Only number with number or string with string; otherwise a `TypeError`     |
| **`var` keyword**            | Function-scoped variable, hoisted                        | Reserved word; use `let`                                                   |
| **`++` / `--`**              | Expressions, prefix and postfix                          | Only the statements `i++;` / `i--;`                                        |
| **`toString()`**             | `"[object Object]"` for objects, `"1,2"` for arrays      | The same text as `print`: `{x: 1}`, `[1, 2]`                               |

### Things Tinyscript shares with JavaScript (strict mode)

- All variables must be declared before use (no implicit globals)
- Classes (without inheritance) and prototypes (`Object.create`, `__proto__`)
- First-class functions, closures, and lexical scoping
- `this` binding in constructors and methods
- `class` with a `constructor` and methods (without inheritance)
- `new`, `instanceof`, `typeof`
- Arrow functions with lexical `this`
- Rest parameters (`...args`), default parameters (`step = 1`), spread in calls
  and array literals (`f(...items)`, `[...a, x]`), rest elements in array
  patterns (`[first, ...rest]`)
- Immediately-invoked function expressions (IIFEs)

### Things not supported

- **Regular expressions** — no regex literals or `RegExp`; to read structured
  text, use `parse` / `findAll` of `std:string`
- **`finally`** — only `try` / `catch`; `catch` does not catch programming
  errors such as a `TypeError`
- **Template literals** — no backtick strings
- **Destructuring** beyond flat patterns in declarations, `for` and parameters —
  no nesting, defaults, renaming, rest elements in object patterns, or patterns
  in assignments
- **Spread in object literals** (`{ ...o }`) — use `Object.assign({}, o)`
- **Empty array elements** (`[1, , 2]`) and a comma after the last argument or
  parameter (`f(a, b,)`); in array and object literals it is allowed
- **`switch` statement**
- **Getter / setter properties** (`get`/`set` in object literals and classes)
- **Inheritance between classes** (`extends`, `super`), **`static`** members and
  field declarations in a class body
- **`Symbol`, `WeakMap`, `Promise`**, and other ES6+ built-ins
- **`Date`** object (use `System.currentTimeMillis()` instead)
- **`console`** (use `print()` instead)

---

## 13. Modules

A file is a module. Top-level functions and variables marked with `export` can
be imported by other files:

```
// geometry.tys
export function area(r) { return Math.PI * r * r; }
export const unit = "cm";
const helper = 2;                          // not exported: private to the module

// main.tys
import area, unit from "./geometry";     // named imports
import * as geo from "./geometry.tys";   // the whole module as a (read-only) object
print(area(2), unit, geo.area(1));
```

- Imports come first in a file; `export` is only allowed for top-level
  functions, variables and classes.
- **Paths:** relative paths (`"./x"`, `"../lib/y"`) resolve against the
  importing file; bare paths (`"x"`, `"math/vector"`) are searched in the module
  path (REPL: `--module-path dir1:dir2` or `TINYSCRIPT_PATH`, by default the
  directory of the file). The extension `.tys` may be omitted.
- **Each module runs once**, on its first import, in its own scope that only
  sees the builtins. Every importer gets the same exports, so the same values
  and objects; importing a module from several places is fine, cyclic imports
  are an error.
- **Imported names are read-only:** `area = 1;` is a compile-time error, and the
  exports object cannot be changed (`geo.unit = "m";` is a `TypeError`). Objects
  you import stay mutable, and a module can keep state in closures.
- Importing the same name twice, or a name that is also declared in the file, is
  a "Duplicate identifier"; importing a name a module does not export is an
  error (`"geometry.tys" does not export "helper"`).
- Files do not see each other's top-level names; sharing works only through
  `import` / `export`.
- The sandboxed web demo can import only `std:` modules, no files, and not
  `std:fs` or `std:io`.
- The Javascript generator bundles a program and its modules into one script:
  each module becomes an immediately invoked function (so it runs once and its
  private names stay private) that returns its frozen exports.

### Standard library

The `std:` modules come with tinyscript. `std:array`, `std:string` and
`std:math` are written in tinyscript (in the core bundle under `/std`); `std:fs`
and `std:io` are native. An error inside one of them, e.g. a wrong format for
`parse`, is reported at the call in your script.

```
import sum from "std:array";
import words, parse from "std:string";
import * as fs from "std:fs";
import readLine, lines from "std:io";
```

#### `std:array`

| Function             |                                            |
| -------------------- | ------------------------------------------ |
| `sum(items)`         | the sum of the numbers                     |
| `contains(items, x)` | whether an element is `== x`               |
| `reverse(items)`     | the elements in reverse order, a new array |
| `min(items)`         | the smallest element (numbers, or strings) |
| `max(items)`         | the largest element (numbers, or strings)  |

They work on anything the `for` loop goes through: arrays, ranges, strings,
`Set`s and `Map`s (`sum(range(10))` is `45`). The elements of a string are its
characters, so `contains("abc", "bc")` is `false` (use `"abc".includes("bc")`);
the elements of a `Map` are `[key, value]` pairs, which `==` never matches (use
`m.has(key)`). `min` and `max` compare as `<` does, so the elements must be all
numbers or all strings; with no elements they throw an `Error`. `max(values)` is
`Math.max(...values)`, but without a limit on the number of elements.

#### `std:string`

| Function                |                                                                                                                                             |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| `words(text)`           | the words separated by any whitespace: `words(" 3   4\n")` is `["3", "4"]` (Javascript: `text.trim().split(/\s+/)`)                         |
| `integers(text)`        | all integers in a text: `integers("p=0,4 v=3,-3")` is `[0, 4, 3, -3]`                                                                       |
| `isWhitespace(c)`       | whether `c` is a space, a tab or a line break                                                                                               |
| `isDigit(c)`            | whether `c` is a digit `0` to `9`; `false` for `undefined`, e.g. `s[s.length]`                                                              |
| `parse(format, text)`   | the values of the fields if the whole text fits the format, otherwise `null` (see below)                                                    |
| `findAll(format, text)` | the values of the fields of each match in the text (see below)                                                                              |
| `blocks(text)`          | the groups of lines separated by blank lines, each an array of its lines: `blocks("a\nb\n\nc")` is `[["a", "b"], ["c"]]` (also with `\r\n`) |

**Reading text with `parse` and `findAll`.** They read structured text with a
format, as Python's `parse` library does, without regular expressions:

```
import parse, findAll from "std:string";
const [px, py, vx, vy] = parse("p={:d},{:d} v={:d},{:d}", "p=0,4 v=3,-3");
const [a, op, b, out] = parse("{:w} {:w} {:w} -> {:w}", "x00 AND y00 -> z00");
for (const [x, y] : findAll("mul({:d},{:d})", memory)) { … }
```

| Field  | Matches                                                   | Value  |
| ------ | --------------------------------------------------------- | ------ |
| `{}`   | any text (at least one character) up to the text after it | string |
| `{:d}` | an integer, with an optional sign: `42`, `-3`, `+7`       | number |
| `{:f}` | a decimal number with a point: `3.5`, `-.25`              | number |
| `{:g}` | any number: `42`, `-2.5`, `1e3`                           | number |
| `{:w}` | a word: letters, digits and `_`                           | string |
| `{:l}` | letters                                                   | string |
| `{:S}` | anything but whitespace                                   | string |

- `parse(format, text)` gives an array with the values of the fields if the
  whole text fits the format (trailing whitespace is ignored), otherwise `null`,
  so formats can be tried one after the other.
- `findAll(format, text)` gives an array with the values of each match, from
  left to right, without overlaps (`[]` if there is none).
- Everything else in the format must appear as it is; `{{` and `}}` are literal
  braces, and a space matches one or more spaces or tabs.
- Fields take as much as their type allows, `{}` up to the first occurrence of
  the text after it, so `{}` needs text after it (except at the end of a `parse`
  format).
- Unlike Python, there is no backtracking: `parse("{:d}{:d}", "123")` is `null`,
  not `[12, 3]`. In return, the time is linear in the length of the text.
- Named fields (`{x:d}`), the other types of Python's library and its case
  insensitivity are not supported. A malformed format (`{:x}`, a single `}`) is
  an `Error`.

#### `std:math`

Integer helpers that `Math` (a global, as in Javascript) does not have:

| Function    |                                                                                           |
| ----------- | ----------------------------------------------------------------------------------------- |
| `gcd(a, b)` | the greatest common divisor of two integers: `gcd(12, 18)` is `6`                         |
| `lcm(a, b)` | the least common multiple: `lcm(4, 6)` is `12`; for several numbers `numbers.reduce(lcm)` |

Both need integers (otherwise an `Error`). Results beyond 2^53 are not exact, as
for all numbers.

#### `std:fs`

File access, UTF-8, with paths relative to the working directory. It is native
and not available sandboxed (e.g. in the web demo). A failed read or write
throws an `Error` named `IOError`, which `catch` can catch (see
[`try` / `catch`](#try--catch)); wrong arguments are a `TypeError`.

| Function                  |                                                                                       |
| ------------------------- | ------------------------------------------------------------------------------------- |
| `exists(path)`            | `true` / `false`, never fails                                                         |
| `readFile(path)`          | the contents as a string                                                              |
| `readLines(path)`         | the lines as an array (split at `\n` / `\r\n`; a final line break adds no empty line) |
| `writeFile(path, text)`   | replaces the file; `text` must be a string                                            |
| `writeLines(path, lines)` | replaces the file; each line ends with `\n`; `readLines` gives the same array back    |
| `appendFile(path, text)`  | appends, creating the file if needed                                                  |
| `listDir(path)`           | the sorted names in a directory                                                       |

```
import * as fs from "std:fs";
if (fs.exists("todo.txt")) {
    for (const line : fs.readLines("todo.txt")) { print("- " + line); }
}
```

#### `std:io`

The standard input, line by line: what the user types, or a file or pipe
(`tinyscript sum.tys < numbers.txt`). UTF-8; a line ends at `\n` or `\r\n` and
comes without it, and a last line without a line break counts. It is native and
not available sandboxed (e.g. in the web demo). A failed read throws an `Error`
named `IOError`; wrong arguments are a `TypeError`.

| Function           |                                                                                                        |
| ------------------ | ------------------------------------------------------------------------------------------------------ |
| `readLine(prompt)` | shows `prompt` (optional, a string; without a line break) and reads a line; `null` at the end of input |
| `lines()`          | the remaining lines for a `for` loop, read one by one as the loop goes on                              |

```
import readLine, lines from "std:io";

const name = readLine("Your name? ");
if (name != null) { print("Hello " + name); }

let sum = 0;
for (const line : lines()) { sum += Number(line); }
print(sum);
```

- The end of the input is `null`: Ctrl-D at the keyboard (Ctrl-Z on Windows),
  the end of a file. After that, `readLine` keeps giving `null`.
- `lines()` reads lazily, so a filter prints its first results before all input
  has arrived, and a loop that `break`s leaves the rest: `readLine` and a later
  `lines()` go on where it stopped. Lines that have been read are gone, so
  `lines()` can be read only once; to keep the lines, push them into an array in
  the loop.
- In the interactive REPL, `readLine` reads the next line of the terminal, with
  line editing; Ctrl-C stops the statement.
- The Javascript generator reads the standard input of node synchronously, so a
  generated script behaves the same.

## Quick Reference

### Keywords

```
let  const  function  class  return  if  else  for  while  break  continue
throw  try  catch  new  this  null  undefined  true  false  typeof  instanceof
delete  assert  import  export  from  as
```

Reserved (a syntax error with a hint what to write instead): `var`, `finally`,
`switch`, `case`, `interface`, `implements`.

### Literals

```
42        3.14      1e6       // numbers
"hello"   'world'             // strings
true      false               // booleans
null      undefined           // null, undefined
[1, 2, 3]                    // array
{ key: value }               // object
{ key, method() { ... } }    // object with shorthands
function(x) { ... }          // function expression
x => x * 2                   // arrow function
```

### Common patterns

```
// An object with a method
const point = { x: 1, y: 2, length() { return Math.hypot(this.x, this.y); } };

// A class
class Circle {
    constructor(r) { this.radius = r; }
    area() { return Math.PI * this.radius * this.radius; }
}
const c = new Circle(5);
print(c.area());

// Counter via closure (private state)
function makeCounter() {
    let n = 0;
    return { inc() { n++; }, get() { return n; } };
}

// Functional array processing
const evens = [1,2,3,4,5,6].filter(x => x % 2 == 0);
const doubled = evens.map(x => x * 2);
print(doubled.join(", "));   // "4, 8, 12"
```
