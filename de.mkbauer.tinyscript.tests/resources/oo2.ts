// code: language=javascript insertSpaces=true tabSize=4

// Object Oriented Programming
// see: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Introduction_to_Object-Oriented_JavaScript

// Helper function, because Object.create(proto) is not yet implemented
function createObject(proto) {
    function ctor() { }
    ctor.prototype = proto;
    return new ctor();
}

// Helper function because console.log(s) is not yet implemented
var console = {};
console.log = function(s) {
    print(s);
};

var Person = (function() {
    // Constructor
    function Person(firstName) {
        this.firstName = firstName;
    }

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
    Student.prototype = createObject(Person.prototype); // Object.create(Person.prototype)

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
assert((student1 instanceof Person) == true); // true 
assert((student1 instanceof Student) == true); // true

// Inspect Janet and her prototype chain
console.log("Janet:");
console.log(student1);
console.log("Prototype chain of Janet:");
var prototype_chain = [student1.__proto__, student1.__proto__.__proto__];
for (var p: prototype_chain) {
    console.log(p);
};

assert(student1.__proto__ == Student.prototype);
assert(student1.__proto__.hasOwnProperty("sayHello")==true); 
assert(student1.__proto__.hasOwnProperty("sayGoodBye")==true);
assert(student1.__proto__.hasOwnProperty("walk")==false); // This is inherited, so it's not in Student.prototype!

assert(student1.__proto__.__proto__ == Person.prototype);
assert(student1.__proto__.__proto__.hasOwnProperty("sayHello")==true);
assert(student1.__proto__.__proto__.hasOwnProperty("walk")==true);
assert(student1.__proto__.__proto__.hasOwnProperty("sayGoodBye")==false);

// Another way of doing the above checks
function includes(arr, obj) {
    return (arr.filter(elem => obj == elem).length > 0);
}
assert(includes(Object.keys(student1.__proto__), "constructor"));
assert(includes(Object.keys(student1.__proto__), "sayHello"));
assert(includes(Object.keys(student1.__proto__), "sayGoodBye"));
assert(!includes(Object.keys(student1.__proto__), "walk"));

// A standardized replacement for __proto__
assert(Object.getPrototypeOf(student1) == Student.prototype);
assert(Object.getPrototypeOf(Object.getPrototypeOf(student1)) == Person.prototype);


