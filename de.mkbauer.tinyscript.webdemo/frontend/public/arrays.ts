// Working with arrays
var arr = ["Tinyscript", "is", "tiny"];

// Iterate over arr's elements and print them.
for (var str : arr) {
    print(str);
}

var x = arr.pop();
print(arr); // ["Tinyscript", "is"]
print(x); 	// "tiny"

arr.push("great");
print(arr); // ["Tinyscript", "is", "great"]

arr.shift();
print(arr); // ["is", "great"]

arr.unshift("Programming");
print(arr); // ["Programming", "is", "great"]

// Using map, filter with arrow functions
var numbers = [1, 5, 4, 2, 9, 7];
var squares = numbers.map(x => x*x); // equivalent to: var squares = arr.map(function(x) {return x*x; });
print(squares); // [1, 25, 16, 4, 81, 49]

var odds = numbers.filter(x => x % 2 == 1);
print(odds); // [1, 5, 9, 7]

// Iterating over objects
// Object.keys(someObject) returns they property keys of someObject as an array
var person = { name: "Markus", age: 43 };
for (var key: Object.keys(person)) {
    print(key + " is " + person[key]);
}