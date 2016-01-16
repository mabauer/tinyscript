// Object Oriented Programming
// see: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Introduction_to_Object-Oriented_JavaScript

// Adapted to make it testable

// Helper function, because Object.create(proto) is not yet implemented
function createObject(proto) {
    function ctor() { }
    ctor.prototype = proto;
    return new ctor();
}

// Define the Person constructor
var Person = function(firstName) {
	this.firstName = firstName;
};

// Add a couple of methods to Person.prototype
Person.prototype.walk = function() {
	return "I am walking!";
};

Person.prototype.sayHello = function() {
	return "Hello, I'm " + this.firstName;
};

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
// Student.prototype.constructor = Student;

// Replace the "sayHello" method
Student.prototype.sayHello = function() {
	return "Hello, I'm " + this.firstName + ". I'm studying "
			+ this.subject + ".";
};

// Add a "sayGoodBye" method
Student.prototype.sayGoodBye = function() {
	return "Goodbye!";
};

// Example usage:
var student1 = new Student("Janet", "Applied Physics");
assert ("Hello, I'm Janet. I'm studying Applied Physics." == student1.sayHello()) ;
assert ("I am walking!" == student1.walk()); 
assert ("Goodbye!" == student1.sayGoodBye());

// Check that instanceof works correctly
assert(student1 instanceof Object); 
assert(student1 instanceof Person);  
assert(student1 instanceof Student);
