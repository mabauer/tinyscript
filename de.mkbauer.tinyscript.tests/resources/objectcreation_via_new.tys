
// Constructor
function Person(name) {
	this.name = name;
}

// Create Object
var markus = new Person("Markus");
assert(markus.name=="Markus");
assert(Person.prototype==markus.__proto__);
assert(markus.__proto__);

// Create "methods"
Person.prototype.setName = function(name) { 
	return this.name = name; 
};

Person.prototype.getName = function() { 
	return this.name; 
};

// Test methods
assert(markus.getName()=="Markus");
markus.setName("Hugo");
assert(markus.getName()=="Hugo");

// Create some more objects...
markus = new Person("Markus");

var fritz = new Person("Fritz");
assert(fritz.getName()=="Fritz");

// ... and check that their instances are different but their prototypes are the same
assert((markus.getName()!=fritz.getName()));
assert(markus.__proto__==fritz.__proto__);

assert(markus instanceof Person);
assert(markus instanceof Object);

