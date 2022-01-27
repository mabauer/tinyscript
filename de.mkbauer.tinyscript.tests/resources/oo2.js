var util = require('util');

// Object Oriented Programming
// see: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Introduction_to_Object-Oriented_JavaScript

// Helper function, because Object.create(proto) is not yet implemented
function createObject(proto) {
    function ctor() { }
    ctor.prototype = proto;
    return new ctor();
}

var Person = (function() {
    // Constructor
    function Person(firstName) {
        this.firstName = firstName;
    }

    // Set the "constructor" property to refer to Person
    Person.prototype.constructor = Person;

    // Add a couple of methods to Person.prototype
    Person.prototype.walk = function() {
	    console.log("I am walking!");
    };

    Person.prototype.sayHello = function() {
	    console.log("Hello, I'm " + this.firstName);
    };

    return Person;
})();

var Student = (function() {

    // Define the Student constructor
    function Student(firstName, subject) {
	    // Call the parent constructor, making sure (using Function#call)
	    // that "this" is set correctly during the call
	    Person.call(this, firstName);
	    // Initialize our Student-specific properties
	    this.subject = subject;
    }

    // Create a Student.prototype object that inherits from Person.prototype.
    // Note: A common error here is to use "new Person()" to create the
    // Student.prototype. That's incorrect for several reasons, not least 
    // that we don't have anything to give Person for the "firstName" 
    // argument. The correct place to call Person is above, where we call 
    // it from Student.
    Student.prototype = Object.create(Person.prototype)

    // Set the "constructor" property to refer to Student
    Student.prototype.constructor = Student;

    // Replace the "sayHello" method
    Student.prototype.sayHello = function() {
	    console.log("Hello, I'm " + this.firstName + ". I'm studying "
			+ this.subject + ".");
    };

    // Add a "sayGoodBye" method
    Student.prototype.sayGoodBye = function() {
	    console.log("Goodbye!");
    };

    return Student;
})();

// Example usage:
var student1 = new Student("Janet", "Applied Physics");
student1.sayHello(); // "Hello, I'm Janet. I'm studying Applied Physics."
student1.walk(); // "I am walking!"
student1.sayGoodBye(); // "Goodbye!"

// Check that instanceof works correctly
console.log(student1 instanceof Person); // true 
console.log(student1 instanceof Student); // true

console.log(Person.prototype);
console.log(Student.prototype);

console.log("Prototype chain of Janet:");
var prototype_chain = [student1.__proto__, student1.__proto__.__proto__];
console.log(prototype_chain.join("->"));

console.log(util.inspect(student1.__proto__));
console.log(student1.__proto__.hasOwnProperty("sayHello")); // true
console.log(student1.__proto__.hasOwnProperty("walk")); // false
console.log(util.inspect(student1.__proto__.__proto__));