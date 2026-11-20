# Tinyscript Language Reference

Tinyscript is a small JavaScript-like scripting language. It is intentionally minimal — think JavaScript in strict mode, minus the historic oddities, plus a couple of deliberate design differences. If you know JavaScript, you can read Tinyscript immediately; this document covers what's the same, what's different, and all the details you need to write correct programs.

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
9. [Prototypal inheritance](#9-prototypal-inheritance)
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

Statements end with `;`. Unlike JavaScript's automatic semicolon insertion, Tinyscript follows Java-like rules: **the semicolon is required** at the end of expression statements, variable declarations, and return statements.

```
var x = 42;
print(x);
return x;
```

Blocks (`{ }`) and control structures do not take a semicolon after the closing brace.

### Identifiers

Identifiers follow the usual rules: start with a letter or `_`, followed by letters, digits, or `_`. The prefix `__ts` is reserved for the cross-compiler's internal shims and must not be used in user code.

---

## 2. Types and Values

Tinyscript has six types:

| Type | Examples | Notes |
|---|---|---|
| `number` | `0`, `3.14`, `1e6`, `-Infinity` | All numbers are 64-bit IEEE 754 doubles |
| `string` | `"hello"`, `'world'` | Single or double quotes; standard escape sequences |
| `boolean` | `true`, `false` | |
| `null` | `null` | Intentional absence of a value |
| `undefined` | (unassigned variables) | Default value of uninitialized variables |
| `object` | `{ }`, `[ ]`, functions | All objects and arrays; functions are objects |

### Conditions

A condition (`if`, `while`, `assert`, `!`, `&&`, `||`, the callback of `filter`) must be:
- a boolean,
- `null` or `undefined`, which count as `false`, or
- an object (including arrays and functions), which counts as `true`.

Numbers and strings are a `TypeError` ("Condition must be a boolean, not number"): in Javascript `0`, `""` and `NaN` are false although they are valid values, a common source of bugs. So `while (node)` and `if (o.child)` work, `if (count)` must be written `if (count > 0)`, `if (name)` as `if (name != "")`, and `if (i = 0)` (a typo for `==`) is an error.

### `typeof`

```
typeof x        // "undefined", "boolean", "number", "string", "function", or "object"
typeof null     // "object"  (matches ECMAScript)
typeof f()      // works on any operand: calls, property accesses, a[0], (1 + 2)
typeof 1 + "x"  // "numberx": typeof binds tighter than binary operators
```

`typeof` is a unary operator like `-` and `!`. Unlike Javascript, `typeof` on an undeclared name is not `"undefined"` but a compile-time error, like any other use of an undeclared name. A property access on `undefined` (`typeof o.x.y` when `o.x` is missing) is a `TypeError`, as in Javascript.

---

## 3. Variables and Scope

### Declaration

Variables must be declared before use. Tinyscript runs in strict mode — using an undeclared identifier throws a `ReferenceError`.

```
var x;          // declared, value is undefined
var y = 10;
var a = 1, b = 2, c = a + b;   // multiple declarations in one statement
```

`let` is accepted as a synonym for `var`.

### Block scope

Blocks create real scopes. A variable declared inside a block is not visible outside it:

```
{
    var inner = "only visible here";
}
// inner is not accessible here — ReferenceError
```

**No hoisting:** Unlike JavaScript's `var`, Tinyscript variables are block-scoped and are not hoisted. You cannot use a variable before the line where it is declared.

### Function scope

Each function call creates a new scope. Variables declared inside a function are not visible outside it.

### `this`

`this` at global scope refers to the global object. Inside a regular function called as a method (`obj.method()`), `this` refers to `obj`. Arrow functions inherit `this` from their enclosing lexical scope (see [Arrow functions](#arrow-functions)).

---

## 4. Operators

### Arithmetic

| Operator | Operation |
|---|---|
| `+` | Addition (numbers), concatenation (a string and any value), concatenation of two arrays |
| `-` | Subtraction |
| `*` | Multiplication |
| `/` | Division |
| `%` | Remainder |
| `-x` | Unary negation |

Operators do not convert their operands: `-`, `*`, `/`, `%` and unary `-` need numbers, and `+` adds two numbers, two arrays, or a string and a value; anything else (`1 + true`, `1 - "a"`, `undefined + 1`) is a `TypeError` instead of a silent `NaN`. `NaN` is a number, so `NaN - 1` is `NaN`.

With a string, `+` converts the other value to a string: `undefined` and `null` as `"undefined"` / `"null"`, an object with a `toString` method written in tinyscript by calling it, other values in their usual form. tinyscript does not support `valueOf` (it would convert objects implicitly in operators).

```
var arr = ["a", "b"];
arr = arr + ["c", "d"];      // ["a", "b", "c", "d"]
print("n = " + 42);          // n = 42
```

### Comparison

| Operator | Meaning |
|---|---|
| `==` | Equality (no type coercion: `1 == "1"` is `false`) |
| `!=` | Inequality |
| `<` | Less than |
| `<=` | Less than or equal |
| `>` | Greater than |
| `>=` | Greater than or equal |
| `instanceof` | Prototype chain check |

`<`, `<=`, `>`, `>=` compare two numbers or two strings (lexicographically). Comparing values of different or other types is a `TypeError`.

### Logical

| Operator | Meaning |
|---|---|
| `&&` | Logical AND |
| `\|\|` | Logical OR |
| `!` | Logical NOT |
| `??` | `a ?? b` is `b` if `a` is `null` or `undefined`, otherwise `a` |

Unlike Javascript, `&&` and `||` always give a boolean, and their operands must be conditions (see Conditions). So the Javascript idiom `x || defaultValue` does not work; use `x ?? defaultValue`, e.g. `counts.get(key) ?? 0`. As in Javascript, `b` is only evaluated if needed, and `??` cannot be combined with `&&` or `||` without parentheses: write `(a ?? b) || c`.

### Assignment

```
x = value;
x += 2;      // x = x + 2, also -=, *=, /=, %=
a[i()] += 1; // a and i() are evaluated only once
```

Assignment is an expression — it returns the assigned value — so it can appear on the right side of another assignment. Only variables and properties can be assigned to. `+=` follows the rules of `+` (numbers, strings, arrays), the others need numbers.

### Increment and decrement

```
i++;
counts[c]--;
```

`++` and `--` add or subtract 1. Unlike Javascript, they are only statements: `x = i++`, `f(i++)` and the prefix forms `++i` / `--i` are syntax errors. The value must be a number.

### Conditional operator

```
var sign = x > 0 ? 1 : x < 0 ? -1 : 0;
```

`cond ? a : b` evaluates `a` if `cond` is true, otherwise `b`; the condition follows the rules for conditions. Unlike Javascript, `b` cannot be an assignment: write `c ? a : (b = 1)`.

### Operator precedence (high to low)

1. `!`, unary `-`
2. `*`, `/`, `%`
3. `+`, `-`
4. `<`, `<=`, `>`, `>=`, `instanceof`
5. `==`, `!=`
6. `&&`
7. `||`
8. `??`
9. `? :` (right-associative)
10. `=`, `+=`, `-=`, `*=`, `/=`, `%=` (right-associative)

### `assert`

```
assert condition;
```

Throws an error if `condition` is `false` (or `null` / `undefined`; numbers and strings are a `TypeError`, see Conditions). Useful for quick sanity checks during development:

```
assert fibonacci(7) == 21;
```

---

## 5. Statements

### Variable declaration

```
var name;
var name = expression;
var a = 1, b = 2, c = 3;
```

### Expression statement

Any expression followed by `;` can be used as a statement. The most common use is function calls and assignments:

```
print("hello");
x = x + 1;
obj.method(arg);
```

### Block

A block groups statements and introduces a new scope:

```
{
    var local = 1;
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

There is no `else if` keyword; nest another `if` inside the `else` block:

```
if (x < 0) {
    print("negative");
} else {
    if (x == 0) {
        print("zero");
    } else {
        print("positive");
    }
}
```

### `for` — numeric loop

Tinyscript's numeric `for` loop is inspired by Lua. It counts an integer variable from `start` to `stop` (inclusive):

```
for (var i = start, stop) { ... }
for (var i = start, stop, step) { ... }
```

- The loop variable is declared with `var` or can be an existing variable reference.
- `start`, `stop`, and optional `step` are evaluated once before the loop begins.
- With a positive `step`, the condition is `i <= stop`. With a negative `step`, the condition is `i >= stop`.
- The loop variable is incremented by `step` (default `1`) after each iteration.

```
// Prints 0 1 2 3 4 5
for (var i = 0, 5) {
    print(i);
}

// Count down: prints 10 8 6 4 2
for (var i = 10, 1, -2) {
    print(i);
}
```

### `for` — iterable loop

Iterates over the elements of an array (values, not indices), the characters of a string, the values of a `Set` or the entries `[key, value]` of a `Map`; anything else is a `TypeError`:

```
for (var element : array) { ... }
for (var c : "abc") { ... }
```

A loop over an array sees elements that its body appends; a loop over a string, `Set` or `Map` goes through the elements it had when the loop started.

```
var fruits = ["apple", "banana", "cherry"];
for (var fruit : fruits) {
    print(fruit);   // apple, then banana, then cherry
}
```

To iterate over object keys, combine with `Object.keys()`:

```
var person = { name: "Alice", age: 30 };
for (var key : Object.keys(person)) {
    print(key + " = " + person[key]);
}
```

### `while`

Runs the block as long as the condition is `true` (see Conditions). Braces are required:

```
var i = 0;
while (i < 3) {
    print(i);
    i = i + 1;
}
```

In sandboxed mode every iteration counts against the statement limit, so even `while (true) { }` terminates with a resource limit violation.

### `break`

Exits the innermost enclosing `for` or `while` loop. `break` is only allowed inside a loop of the same function (a function defined inside a loop cannot break out of it):

```
for (var i = 0, 100) {
    if (i == 10) {
        break;
    }
    print(i);
}
```

### `continue`

Skips the rest of the current iteration of the innermost enclosing `for` or `while` loop and goes on with the next one. Like `break`, it is only allowed inside a loop of the same function:

```
for (var i = 1, 10) {
    if (i % 2 == 0) {
        continue;
    }
    print(i);   // 1, 3, 5, 7, 9
}
```

### `return`

Returns a value from a function. A `return` without an expression returns `undefined`. `return` at global (top-level) scope is a validation error.

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

Function declarations are hoisted to the top of their enclosing block, so they can be called before the declaration appears in the source.

```
function add(a, b) {
    return a + b;
}

print(add(3, 4));   // 7
```

### Function expression

Functions can be assigned to variables or passed as values. An optional name can be given for recursion:

```
var square = function(x) {
    return x * x;
};

// Named function expression (name is only in scope inside the function body)
var factorial = function fact(n) {
    if (n <= 1) { return 1; }
    return n * fact(n - 1);
};
```

### Arrow functions

Arrow functions provide a concise syntax and capture `this` from their surrounding lexical scope.

**Expression body** (implicit return):

```
var double = x => x * 2;
var add = (a, b) => a + b;
```

**Block body** (explicit `return` required; without it, the result is `undefined`):

```
var greet = name => {
    var msg = "Hello, " + name;
    return msg;
};
```

As in Javascript, a `{` after `=>` always starts a block body; to return an object, put it in parentheses: `x => ({ value: x })`. The body can be another arrow function: `var add = x => y => x + y;`.

Parentheses around parameters are optional for a single parameter, required for zero or multiple:

```
var noArgs = () => 42;
var oneArg = x => x + 1;
var twoArgs = (x, y) => x + y;
```

### Rest parameters

The last parameter can be a rest parameter, collecting all extra arguments into an array:

```
function sum(...nums) {
    var total = 0;
    for (var n : nums) {
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

### `new` — constructor calls

Any function can be used as a constructor with `new`. Inside the constructor, `this` refers to the newly created object. The constructor's return value is ignored; the new object is returned.

```
function Point(x, y) {
    this.x = x;
    this.y = y;
}

var p = new Point(3, 4);
print(p.x);   // 3
```

### `Function.prototype.call` and `.apply`

```
fn.call(thisArg, arg1, arg2)
fn.apply(thisArg, [arg1, arg2])
```

These call `fn` with an explicit `this` value. `.apply` takes the arguments as an array.

---

## 7. Objects

### Object literal

```
var person = {
    name: "Alice",
    age: 30,
    greet: function() {
        print("Hello, I'm " + this.name);
    }
};
```

Property keys can be identifiers or string literals. Values can be any expression.

### Property access

```
person.name           // dot notation
person["name"]        // bracket notation (key is a string expression)
person["na" + "me"]   // any string expression works
```

**Optional chaining:** `a?.b`, `a?.[key]` and `a?.m()` are `undefined` if `a` is `null` or `undefined`, instead of a `TypeError`. As in Javascript, the rest of the chain is skipped: `a?.b.c` is `undefined` if `a` is `null` (`.c` is not evaluated). An optional chain cannot be assigned to (`a?.b = 1` is an error).

```
var list = { value: 1, next: null };
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

Property deletion is not directly supported. Setting a property to `undefined` effectively removes its meaningful value.

---

## 8. Arrays

### Array literal

```
var empty = [];
var nums = [1, 2, 3];
var mixed = ["hello", 42, true, null];
```

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

Arrays are lists: besides their items, only `length` can be set. Unlike Javascript, writing any other key is a `TypeError`, including `nums.x = 1`, `nums[-1] = 1` and `nums[1.5] = 1`; `Object.defineProperty` on an array is a `TypeError` as well. Reading such a key gives `undefined`. An invalid `length` (negative, fractional, not a number) is a `TypeError` (Javascript: `RangeError`).

### Array methods

| Method | Description |
|---|---|
| `arr.at(i)` | Element at index `i`; a negative `i` counts from the end (`arr.at(-1)` is the last element); `undefined` if out of range |
| `arr.indexOf(x[, start])` | Index of the first element `== x` (tinyscript's `==`), or `-1`; searching from `start` (negative: counts from the end) |
| `arr.includes(x[, start])` | `true` if an element `== x`, searching from `start` (negative: counts from the end) |
| `arr.push(x)` | Appends `x` to the end; returns new element count |
| `arr.pop()` | Removes and returns the last element |
| `arr.shift()` | Removes and returns the first element |
| `arr.unshift(x)` | Prepends `x`; returns new element count |
| `arr.slice([start[, end]])` | A new array with the elements from `start` (inclusive) to `end` (exclusive); negative indices count from the end; `arr.slice()` copies the array |
| `arr.fill(x[, start[, end]])` | Sets the elements from `start` to `end` to `x`; returns the array |
| `arr.join([sep])` | Returns a string with elements joined by `sep` (default `","`) |
| `arr.forEach(fn)` | Calls `fn(element, index, array)` for each element |
| `arr.map(fn)` | Returns a new array of the results of `fn(element, index, array)` |
| `arr.filter(fn)` | Returns a new array of the elements for which `fn(element, index, array)` is true |
| `arr.some(fn)` / `arr.every(fn)` | Whether `fn` is true for some / every element |
| `arr.find(fn)` / `arr.findIndex(fn)` | The first element / its index for which `fn` is true, or `undefined` / `-1` |
| `arr.reduce(fn[, initial])` | Combines the elements with `fn(accumulator, element, index, array)`, starting with `initial` (or the first element; then an empty array is a `TypeError`) |
| `arr.sort([compare])` | Sorts the array in place (stable) and returns it; `compare(a, b)` returns a negative number if `a` comes first, a positive one if `b` does, `0` otherwise |
| `arr.toString()` | Returns a string representation |

The callbacks must be functions; the results of the callbacks of `filter`, `some`, `every`, `find` and `findIndex` are conditions (a boolean, `null` / `undefined` or an object, see [Conditions](#conditions)).

Without a comparison function, `sort` only sorts strings; unlike Javascript, which would sort numbers as strings (`[10, 9, 1].sort()` gives `[1, 10, 9]`), sorting other values without one is a `TypeError`. Sort numbers with `arr.sort((a, b) => a - b)`.

`new Array(n)` creates an array of `n` elements `undefined` (fill it with `fill`); `new Array(a, b, ...)` or `new Array("x")` create an array of these elements. Unlike Javascript, `new Array(n)` has no "holes": `map` and `forEach` see all `n` elements.

```
var numbers = [1, 5, 4, 2, 9, 7];

var squares = numbers.map(x => x * x);
print(squares);   // [1, 25, 16, 4, 81, 49]

var odds = numbers.filter(x => x % 2 == 1);
print(odds);      // [1, 5, 9, 7]

print(numbers.join(", "));   // "1, 5, 4, 2, 9, 7"
print(numbers.reduce((sum, x) => sum + x, 0));   // 28
print(numbers.slice().sort((a, b) => a - b));     // [1, 2, 4, 5, 7, 9]

var grid = new Array(3).fill(0).map(() => new Array(3).fill("."));
```

### Array concatenation with `+`

The `+` operator has array-specific semantics:

```
var a = [1, 2];
var c = a + [3, 4];    // [1, 2, 3, 4] — concatenate arrays
var s = a + "!";       // "[1, 2]!"    — with a string: string concatenation
// a + 3 is a TypeError
```

### `Array.isArray`

```
Array.isArray([1, 2, 3])   // true
Array.isArray("hello")     // false
```

### Iterating

```
for (var x : array) { ... }          // iterates values
for (var i = 0, array.length) { ... } // numeric index loop (if you track length)
```

---

## 9. Prototypal Inheritance

Tinyscript follows JavaScript's prototypal model. Every object has a prototype (accessible as `__proto__`). When you access a property that doesn't exist on an object, the lookup walks up the prototype chain.

### Constructor functions and `.prototype`

The standard JavaScript pattern works:

```
function Animal(name) {
    this.name = name;
}

Animal.prototype.speak = function() {
    print(this.name + " makes a sound.");
};

function Dog(name) {
    Animal.call(this, name);   // call parent constructor
}

Dog.prototype = Object.create(Animal.prototype);

Dog.prototype.speak = function() {
    print(this.name + " barks.");
};

var d = new Dog("Rex");
d.speak();                     // "Rex barks."
print(d instanceof Dog);       // true
print(d instanceof Animal);    // true
```

### `Object.create(proto)`

Creates a new object with `proto` as its prototype:

```
var base = { greet: function() { print("Hello!"); } };
var child = Object.create(base);
child.greet();   // "Hello!" — inherited from base
```

### Prototype-as-class pattern

Objects themselves can serve as "classes". The `oo_with_proto.tys` example demonstrates this:

```
var Proto = {
    _new: function(...args) {
        var instance = Object.create(this);
        if (instance.constructor) {
            instance.constructor.apply(instance, args);
        }
        return instance;
    },
    extend: function(subProps) {
        var subProto = Object.create(this);
        // copy subProps properties to subProto ...
        return subProto;
    }
};

var Person = Proto.extend({
    constructor: function(name) { this.name = name; },
    describe: function() { return "Person called " + this.name; }
});

var Employee = Person.extend({
    constructor: function(name, title) {
        Employee.super.constructor.call(this, name);
        this.title = title;
    },
    describe: function() {
        return Employee.super.describe.call(this) + " (" + this.title + ")";
    }
});

var jane = Employee._new("Jane", "CTO");
print(jane.describe());   // "Person called Jane (CTO)"
```

### `instanceof`

Checks whether an object has a constructor's `prototype` anywhere in its prototype chain:

```
print(d instanceof Dog);      // true
print(d instanceof Animal);   // true
print(d instanceof Object);   // true (all objects ultimately trace back)
```

### `__proto__`

The prototype of any object is accessible as `__proto__`:

```
print(child.__proto__ == base);   // true
```

---

## 10. Closures

A closure is a function that retains access to variables from the scope in which it was defined, even after that scope has exited.

```
function makeCounter() {
    var count = 0;
    return function() {
        count = count + 1;
        return count;
    };
}

var counter = makeCounter();
print(counter());   // 1
print(counter());   // 2
print(counter());   // 3
```

Each call to `makeCounter()` creates a fresh `count` variable — closures from different calls do not share state:

```
function createCounter() {
    var value = 0;
    return {
        add: function(x) { value = value + x; },
        sub: function(x) { value = value - x; },
        val: function()  { return value; }
    };
}

var c1 = createCounter();
var c2 = createCounter();

c1.add(14);
c2.add(32);
print(c1.val());   // 14
print(c2.val());   // 32
```

### Module pattern

Immediately-invoked function expressions (IIFEs) can simulate modules:

```
var weekDay;

(function(exports) {
    var names = ["Sunday", "Monday", "Tuesday", "Wednesday",
                 "Thursday", "Friday", "Saturday"];
    exports.name = function(number) { return names[number]; };
    exports.number = function(name) { return names.indexOf(name); };
})(weekDay);

print(weekDay.name(6));            // "Saturday"
print(weekDay.number("Monday"));   // 1
```

---

## 11. Built-in Objects and Functions

### `print(value)`

Writes `value` to standard output followed by a newline. The value is converted to a string.

```
print("hello");
print(42);
print([1, 2, 3]);   // [1, 2, 3]
```

### `Math`

| Member | Description |
|---|---|
| `Math.PI` | 3.141592653589793 |
| `Math.sqrt(x)` | Square root of `x` |
| `Math.round(x)` | Round to nearest integer |
| `Math.random()` | Random number in [0, 1) |
| `Math.min(a, b, ...)` / `Math.max(a, b, ...)` | Smallest / largest of the arguments |
| `Math.abs(x)` | Absolute value |
| `Math.floor(x)` / `Math.ceil(x)` | Round down / up |

```
print(Math.sqrt(2));    // 1.4142135623730951
print(Math.round(2.7)); // 3
print(Math.PI);         // 3.141592653589793
```

### `Number`, `parseInt`, `isNaN`

Conversions to numbers are explicit (no operator or builtin converts silently):

| Function | Description |
|---|---|
| `Number(x)` | A string read as a whole number literal (surrounding whitespace is ignored; decimal, `1e3`, `Infinity`, `0x1f`, `0o17`, `0b101`), otherwise `NaN`; unlike Javascript, `""` and blank strings are `NaN`, not `0`. A number stays as it is, `true` / `false` are `1` / `0`; anything else is a `TypeError` |
| `parseInt(str[, radix])` | The integer at the start of `str`, ignoring the rest (`"42abc"` → `42`), or `NaN`; leading whitespace and a sign are allowed, `0x` means radix 16. `radix` must be an integer from 2 to 36 |
| `isNaN(x)` | `true` if the number `x` is `NaN`; `x` must be a number |
| `Number.isInteger(x)` | `true` if `x` is a finite number without fraction; `false` for anything else |

```
print(Number(" 3.5 ") + 1);   // 4.5
print(isNaN(Number("12px"))); // true
print(parseInt("12px"));      // 12
print(parseInt("ff", 16));    // 255
```

### `Object`

| Method | Description |
|---|---|
| `Object.create(proto)` | New object with given prototype |
| `Object.keys(obj)` | Array of own enumerable property keys |
| `Object.getPrototypeOf(obj)` | Returns `obj`'s prototype |
| `Object.defineProperty(obj, key, desc)` | Define a property with a descriptor |
| `Object.getOwnPropertyDescriptor(obj, key)` | Returns a property descriptor |
| `Object.getOwnPropertyNames(obj)` | Array of all own property names |
| `obj.hasOwnProperty(key)` | `true` if `key` is an own (non-inherited) property |

```
var proto = { greet: function() { print("hi"); } };
var obj = Object.create(proto);
obj.name = "Alice";

print(Object.keys(obj));             // ["name"]
print(Object.getPrototypeOf(obj) == proto);  // true
obj.greet();                         // "hi" (inherited)
```

### `String`

String primitives have methods available via auto-boxing. Numbers and booleans have no wrapper objects: a property access on them (`(5).toString()`, `n.x`) is a `TypeError`; convert with `String(n)` or `"" + n`.

String methods:

| Method | Description |
|---|---|
| `str.at(i)` | Character at index `i`; a negative `i` counts from the end; `undefined` if out of range |
| `str.charAt(i)` | Character at index `i` |
| `str.charCodeAt(i)` | UTF-16 code of the character at index `i`, or `NaN` |
| `str.indexOf(sub[, pos])` | First index of `sub` at or after `pos` (negative: 0), or `-1` |
| `str.lastIndexOf(sub[, pos])` | Last index of `sub` at or before `pos`, or `-1` |
| `str.includes(sub[, pos])` / `str.startsWith(sub[, pos])` | Whether `str` contains `sub` at or after `pos` / starts with `sub` at `pos` |
| `str.endsWith(sub[, end])` | Whether `str` up to `end` ends with `sub` |
| `str.substring(start, end)` | Substring from `start` (inclusive) to `end` (exclusive) |
| `str.slice(start[, end])` | Like `substring`, but negative indices count from the end, and `start > end` gives `""` |
| `str.split(sep[, limit])` | Array of the parts between occurrences of `sep` (at most `limit` of them); `""` splits into characters. To split at any whitespace, use `words(str)` of `std:string` |
| `str.trim()` | Without leading and trailing whitespace |
| `str.replace(search, repl)` / `str.replaceAll(search, repl)` | Replaces the first / every occurrence of `search`; `repl` may contain `$&` (the match), `` $` `` / `$'` (the text before / after it) and `$$` |
| `str.repeat(n)` | `str` repeated `n` times |
| `str.padStart(len[, pad])` / `str.padEnd(len[, pad])` | Padded at the start / end with spaces or `pad` up to length `len` |
| `str.toUpperCase()` / `str.toLowerCase()` | Upper / lower case |
| `str.toString()` | The string itself |

String arguments must be strings and indices integers, otherwise the method throws a `TypeError`. `search` and `sep` are plain strings; there are no regular expressions.

All builtins (methods of strings, arrays, `Map`, `Set`, `Math`, …) understand the optional arguments of their Javascript counterparts as listed here; calling one with more arguments than it takes is a `TypeError` (`"abc".startsWith("a", 0, 1)`: "startsWith() takes at most 2 arguments, not 3"), where Javascript would ignore them. A builtin used as a callback, e.g. `lines.map(Number)`, gets `(item, index, array)` like any callback and uses what it needs. Your own functions are not checked: extra arguments are ignored, as in Javascript.

`String.fromCharCode(c1, c2, ...)` builds a string from UTF-16 codes.

```
var s = "Hello World";
print(s.substring(0, 5));   // "Hello"
print(s.indexOf("World"));  // 6
print(s.charAt(0));         // "H"
print("a,b,c".split(",").length); // 3
print("7".padStart(3, "0")); // "007"
```

### `Array`

See [Arrays](#8-arrays) for method details. Additional static:

| Method | Description |
|---|---|
| `Array.isArray(value)` | Returns `true` if `value` is an array |

### `Map` and `Set`

As in Javascript, keys of a `Map` and values of a `Set` can be of any type: numbers, strings and booleans are compared by value, objects by identity (`NaN` equals `NaN`). Both keep the insertion order.

| `Map` | Description |
|---|---|
| `new Map()` / `new Map([[k1, v1], [k2, v2]])` | An empty map / a map with these entries |
| `m.get(key)` | The value for `key`, or `undefined` |
| `m.set(key, value)` | Sets the value for `key`; returns `m`, so calls can be chained |
| `m.has(key)` | Whether `m` has an entry for `key` |
| `m.delete(key)` | Removes the entry for `key`; returns whether there was one |
| `m.clear()` | Removes all entries |
| `m.size` | The number of entries |
| `m.keys()` / `m.values()` / `m.entries()` | Arrays of the keys / values / `[key, value]` pairs |
| `m.forEach(fn)` | Calls `fn(value, key, map)` for each entry |

| `Set` | Description |
|---|---|
| `new Set()` / `new Set(values)` | An empty set / a set of the elements of an array, string, `Set` or `Map` |
| `s.add(value)` | Adds `value`; returns `s` |
| `s.has(value)` | Whether `s` contains `value` |
| `s.delete(value)` | Removes `value`; returns whether it was there |
| `s.clear()` | Removes all values |
| `s.size` | The number of values |
| `s.values()` | An array of the values |
| `s.forEach(fn)` | Calls `fn(value, value, set)` for each value |

Differences from Javascript: `keys()`, `values()` and `entries()` return arrays, not iterators, so `m.keys().length`, `m.keys().sort()` etc. work. A `Map` or `Set` has no other properties: `m[key] = value` (instead of `m.set(key, value)`) is a `TypeError`.

```
var counts = new Map();
for (var word : "a b a".split(" ")) {
    counts.set(word, (counts.has(word) ? counts.get(word) : 0) + 1);
}
print(counts.get("a"));      // 2

var visited = new Set();
visited.add(1 + "," + 2);
print(visited.has("1,2"));   // true
```

### `Function.prototype`

| Method | Description |
|---|---|
| `fn.call(thisArg, arg1, ...)` | Call `fn` with explicit `this` |
| `fn.apply(thisArg, argsArray)` | Call `fn` with explicit `this` and array of args |

### `System`

| Method | Description |
|---|---|
| `System.currentTimeMillis()` | Current wall-clock time in milliseconds (like `Date.now()`) |
| `System.args` | The arguments of the script, an array of strings: in the REPL, the arguments after `--` (`tinyscript day1.tys -- input.txt`); otherwise empty |

```
var start = System.currentTimeMillis();
// ... do work ...
print(System.currentTimeMillis() - start);   // elapsed ms

var file = System.args.length > 0 ? System.args[0] : "input.txt";
```

### Errors

Calling something that is not a function is a `TypeError` that names it: `String has no method 'splitt'`, `'x' of Object is a number, not a function`, `'f' is undefined, not a function`.

A too deep (or endless) recursion is a `TinyscriptStackOverflowError` ("Stack overflow: too many nested calls"); its stack trace shows only the innermost and outermost calls. The REPL allows a recursion depth of about 500,000 calls; in the web demo the limit is lower.

### File access

There is no global `fs` (since 0.18): file access is the module `std:fs` (see [Modules](#13-modules)), `import * as fs from "std:fs";`. It is not available in the web demo or any sandboxed execution context.

---

## 12. Differences from JavaScript

Understanding where Tinyscript diverges from JavaScript is important both for writing code and for knowing its intentional limitations.

### Things Tinyscript does differently

| Topic | JavaScript | Tinyscript |
|---|---|---|
| **Semicolons** | Optional (ASI) | Required after expression statements, `var`, `return` |
| **Block scope** | `var` is function-scoped; `let`/`const` are block-scoped | All variables are block-scoped (no hoisting for `var`) |
| **Control structure bodies** | Braces optional for single statements | Braces always required |
| **Numeric `for` loop** | `for (let i = 0; i < n; i++)` | `for (var i = 0, n)` (Lua-style) |
| **Iterable `for` loop** | `for (const x of array)` | `for (var x : array)` |
| **`for...in`** | Iterates over keys | Not supported (use `for...of` style with `:`) |
| **Array `+`** | String concatenation | Concatenation of two arrays; an array plus a number etc. is a `TypeError` |
| **`==` vs `===`** | Different (type coercion vs. strict) | Only `==` / `!=`, which never coerce types (there is no `===` / `!==`) |
| **Relational operators** | Coerce operands (`1 < "2"`) | Only number with number or string with string; otherwise a `TypeError` |
| **`let` keyword** | Block-scoped variable | Synonym for `var` |
| **`++` / `--`** | Expressions, prefix and postfix | Only the statements `i++;` / `i--;` |

### Things Tinyscript shares with JavaScript (strict mode)

- All variables must be declared before use (no implicit globals)
- Prototypal inheritance via `prototype` and `__proto__`
- First-class functions, closures, and lexical scoping
- `this` binding in constructors and methods
- `new`, `instanceof`, `typeof`
- Arrow functions with lexical `this`
- Rest parameters (`...args`)
- Immediately-invoked function expressions (IIFEs)

### Things not supported

- **Regular expressions** — no regex literals or `RegExp`
- **`try` / `catch` / `throw`** — no exception handling in user code
- **Template literals** — no backtick strings
- **Destructuring** — no `const { a, b } = obj`
- **Spread operator** — no `...array` in call sites
- **`const`** — no constant declarations
- **`delete`** operator
- **`switch` statement**
- **Getter / setter properties** (`get`/`set` in object literals)
- **`Symbol`, `WeakMap`, `Promise`**, and other ES6+ built-ins
- **`Date`** object (use `System.currentTimeMillis()` instead)
- **`console`** (use `print()` instead)

---

## 13. Modules

A file is a module. Top-level functions and variables marked with `export` can be imported by other files:

```
// geometry.tys
export function area(r) { return Math.PI * r * r; }
export var unit = "cm";
var helper = 2;                          // not exported: private to the module

// main.tys
import area, unit from "./geometry";     // named imports
import * as geo from "./geometry.tys";   // the whole module as a (read-only) object
print(area(2), unit, geo.area(1));
```

- Imports come first in a file; `export` is only allowed for top-level functions and variables.
- **Paths:** relative paths (`"./x"`, `"../lib/y"`) resolve against the importing file; bare paths (`"x"`, `"math/vector"`) are searched in the module path (REPL: `--module-path dir1:dir2` or `TINYSCRIPT_PATH`, by default the directory of the file). The extension `.tys` may be omitted.
- **Each module runs once**, on its first import, in its own scope that only sees the builtins. Every importer gets the same exports, so the same values and objects; importing a module from several places is fine, cyclic imports are an error.
- **Imported names are read-only:** `area = 1;` is a compile-time error, and the exports object cannot be changed (`geo.unit = "m";` is a `TypeError`). Objects you import stay mutable, and a module can keep state in closures.
- Importing the same name twice, or a name that is also declared in the file, is a "Duplicate identifier"; importing a name a module does not export is an error (`"geometry.tys" does not export "helper"`).
- Files do not see each other's top-level names; sharing works only through `import` / `export`.
- **Standard library:** `std:` modules come with tinyscript (written in tinyscript, in the core bundle under `/std`):
  - `std:array`: `range(n)` (`[0, …, n - 1]`), `sum(arr)`, `contains(arr, x)`, `reverse(arr)`
  - `std:string`: `words(text)` (the words separated by any whitespace: `words(" 3   4\n")` is `["3", "4"]`; Javascript: `text.trim().split(/\s+/)`), `isWhitespace(c)`, `isDigit(c)` (`false` for `undefined`, e.g. `s[s.length]`)

  ```
  import range, sum from "std:array";
  print(sum(range(10)));   // 45
  ```
  - `std:fs` (native, not available sandboxed): file access, UTF-8, relative paths against the working directory. Strict: errors end the script (tinyscript has no `try` / `catch`), so check with `exists` first.

    | Function | |
    |---|---|
    | `exists(path)` | `true` / `false`, never fails |
    | `readFile(path)` | the contents as a string |
    | `readLines(path)` | the lines as an array (split at `\n` / `\r\n`; a final line break adds no empty line) |
    | `writeFile(path, text)` | replaces the file; `text` must be a string |
    | `writeLines(path, lines)` | replaces the file; each line ends with `\n`; `readLines` gives the same array back |
    | `appendFile(path, text)` | appends, creating the file if needed |
    | `listDir(path)` | the sorted names in a directory |

    ```
    import * as fs from "std:fs";
    if (fs.exists("todo.txt")) {
        for (var line : fs.readLines("todo.txt")) { print("- " + line); }
    }
    ```
- The sandboxed web demo can import only `std:` modules, no files, and not `std:fs`. The Javascript generator bundles a program and its modules into one script: each module becomes an immediately invoked function (so it runs once and its private names stay private) that returns its frozen exports.

## Quick Reference

### Keywords

```
var  let  function  return  if  else  for  while  break
new  this  null  true  false  typeof  instanceof  assert
```

### Literals

```
42        3.14      1e6       // numbers
"hello"   'world'             // strings
true      false               // booleans
null                          // null
[1, 2, 3]                    // array
{ key: value }               // object
function(x) { ... }          // function expression
x => x * 2                   // arrow function
```

### Common patterns

```
// Named constructor
function Circle(r) {
    this.radius = r;
    this.area = function() { return Math.PI * r * r; };
}
var c = new Circle(5);
print(c.area());

// Inheritance
function Shape(color) { this.color = color; }
function Box(color, side) {
    Shape.call(this, color);
    this.side = side;
}
Box.prototype = Object.create(Shape.prototype);

// Counter via closure
function makeCounter() {
    var n = 0;
    return { inc: function() { n = n + 1; }, get: function() { return n; } };
}

// Functional array processing
var evens = [1,2,3,4,5,6].filter(x => x % 2 == 0);
var doubled = evens.map(x => x * 2);
print(doubled.join(", "));   // "4, 8, 12"
```
