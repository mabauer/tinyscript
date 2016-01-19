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

// Iterating over objects
// Object.keys(someObject) returns they property keys of someObject as an array
var person = { name: "Markus", age: 43 };
for (var key: Object.keys(person)) {
    print(key + " is " + person[key]);
}