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

### Truthiness

The following values are falsy (coerce to `false` in a condition):
- `false`
- `0` and `NaN`
- `""` (empty string)
- `null`
- `undefined`

Everything else is truthy, including `[]` and `{}`.

### `typeof`

```
typeof x        // "undefined", "boolean", "number", "string", "function", or "object"
typeof null     // "object"  (matches ECMAScript)
```

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
| `+` | Addition (numbers), concatenation (strings), array append (see below) |
| `-` | Subtraction |
| `*` | Multiplication |
| `/` | Division |
| `%` | Remainder |
| `-x` | Unary negation |

The `+` operator has extended semantics for arrays: `array + element` appends the element; `array + array` concatenates both arrays into a new array.

```
var arr = ["a", "b"];
arr = arr + "c";             // ["a", "b", "c"]
arr = arr + ["d", "e"];      // ["a", "b", "c", "d", "e"]
```

### Comparison

| Operator | Meaning |
|---|---|
| `==` / `===` | Equality (both behave the same — no type coercion distinction) |
| `!=` / `!==` | Inequality |
| `<` | Less than |
| `<=` | Less than or equal |
| `>` | Greater than |
| `>=` | Greater than or equal |
| `instanceof` | Prototype chain check |

### Logical

| Operator | Meaning |
|---|---|
| `&&` | Logical AND |
| `\|\|` | Logical OR |
| `!` | Logical NOT |

### Assignment

```
x = value;
```

Assignment is an expression — it returns the assigned value — so it can appear on the right side of another assignment.

### Operator precedence (high to low)

1. `!`, unary `-`
2. `*`, `/`, `%`
3. `+`, `-`
4. `<`, `<=`, `>`, `>=`, `instanceof`
5. `==`, `===`, `!=`, `!==`
6. `&&`
7. `||`
8. `=` (right-associative)

### `assert`

```
assert condition;
```

Throws an error if `condition` is falsy. Useful for quick sanity checks during development:

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

Iterates over the elements of an array (values, not indices):

```
for (var element : array) { ... }
```

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

```
while (condition) {
    // body
}
```

### `break`

Exits the immediately enclosing `for` or `while` loop:

```
for (var i = 0, 100) {
    if (i == 10) {
        break;
    }
    print(i);
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

**Block body** (explicit `return` required):

```
var greet = name => {
    var msg = "Hello, " + name;
    return msg;
};
```

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

### Element access

```
nums[0]       // 1 (zero-based)
nums[nums.length - 1]   // last element... wait — .length is not yet implemented
```

> Note: `.length` is not a built-in property. Use index counting or `pop`/`push` patterns instead.

### Array methods

| Method | Description |
|---|---|
| `arr.push(x)` | Appends `x` to the end; returns new element count |
| `arr.pop()` | Removes and returns the last element |
| `arr.shift()` | Removes and returns the first element |
| `arr.unshift(x)` | Prepends `x`; returns new element count |
| `arr.join(sep)` | Returns a string with elements joined by `sep` |
| `arr.concat(other)` | Returns a new array concatenating `arr` and `other` |
| `arr.map(fn)` | Returns a new array by applying `fn(element)` to each element |
| `arr.filter(fn)` | Returns a new array of elements where `fn(element)` is truthy |
| `arr.indexOf(x)` | Returns the index of `x`, or `-1` if not found |
| `arr.toString()` | Returns a string representation |

```
var numbers = [1, 5, 4, 2, 9, 7];

var squares = numbers.map(x => x * x);
print(squares);   // [1, 25, 16, 4, 81, 49]

var odds = numbers.filter(x => x % 2 == 1);
print(odds);      // [1, 5, 9, 7]

print(numbers.join(", "));   // "1, 5, 4, 2, 9, 7"
```

### Array concatenation with `+`

The `+` operator has array-specific semantics:

```
var a = [1, 2];
var b = a + 3;         // [1, 2, 3]   — append element
var c = a + [3, 4];    // [1, 2, 3, 4] — concatenate arrays
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

Objects themselves can serve as "classes". The `oo_with_proto.ts` example demonstrates this:

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
print(child.__proto__ === base);   // true
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

```
print(Math.sqrt(2));    // 1.4142135623730951
print(Math.round(2.7)); // 3
print(Math.PI);         // 3.141592653589793
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
print(Object.getPrototypeOf(obj) === proto);  // true
obj.greet();                         // "hi" (inherited)
```

### `String`

String primitives have methods available via auto-boxing:

| Method | Description |
|---|---|
| `str.charAt(i)` | Character at index `i` |
| `str.indexOf(sub)` | First index of `sub`, or `-1` |
| `str.substring(start, end)` | Substring from `start` (inclusive) to `end` (exclusive) |
| `str.toString()` | The string itself |

```
var s = "Hello World";
print(s.substring(0, 5));   // "Hello"
print(s.indexOf("World"));  // 6
print(s.charAt(0));         // "H"
```

### `Array`

See [Arrays](#8-arrays) for method details. Additional static:

| Method | Description |
|---|---|
| `Array.isArray(value)` | Returns `true` if `value` is an array |

### `Function.prototype`

| Method | Description |
|---|---|
| `fn.call(thisArg, arg1, ...)` | Call `fn` with explicit `this` |
| `fn.apply(thisArg, argsArray)` | Call `fn` with explicit `this` and array of args |

### `System`

| Method | Description |
|---|---|
| `System.currentTimeMillis()` | Current wall-clock time in milliseconds (like `Date.now()`) |

```
var start = System.currentTimeMillis();
// ... do work ...
print(System.currentTimeMillis() - start);   // elapsed ms
```

### `fs` (REPL only, disabled in sandboxed mode)

| Method | Description |
|---|---|
| `fs.readFile(path)` | Returns file contents as a string |
| `fs.writeFile(path, content)` | Writes `content` to a file |

The `fs` object is not available in the web demo or any sandboxed execution context.

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
| **Array `+`** | String concatenation | Array append / concatenation |
| **`==` vs `===`** | Different (type coercion vs. strict) | Both behave the same (no coercion distinction) |
| **`let` keyword** | Block-scoped variable | Synonym for `var` |

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
- **Ternary operator** (`? :`)
- **Getter / setter properties** (`get`/`set` in object literals)
- **`Symbol`, `Map`, `Set`, `Promise`**, and other ES6+ built-ins
- **`Date`** object (use `System.currentTimeMillis()` instead)
- **`console`** (use `print()` instead)
- **Modules** (`import`/`export`) — use the IIFE/closure pattern instead

---

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
