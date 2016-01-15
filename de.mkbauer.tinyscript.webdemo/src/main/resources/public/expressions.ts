// Basic expressions
var result;

print(result); // Undefined

result = 4 + 5;
print(result); // 9

result = "Hello" + " World";
print(result); // "Hello World";

result = "Four is " + 4;
print(result); // "Four is 4"; 

result = Math.sqrt(2);
print(result); // 1.41...

result = true;
print(result); // true

result = (4 > 10);
print(result); // false

// Arrays
var fruits = ["Banana", "Apple", "Strawberry"];
result = fruits + "Cherry";
print(result); // ["Banana", "Apple", "Strawberry", "Cherry"]

var mixed = ["Banana", 3, true, 1.5];
print(mixed);  // ["Banana", 3, true, 1.5]

// Simple Objects
var person = { name: "Markus", age: 42 };
print(person);		// { name: "Markus", age: 42}
print(person.name); // "Markus"
print(person.age);  // 42
person.age = 43;
print(person.age);  // 43


