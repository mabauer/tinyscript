function Person(name) {
	this.name = name;
}

var markus = new Person("Markus");
assert(markus.name=="Markus");
assert(Person.prototype==markus.__proto__);
assert(markus.__proto__);

Person.prototype.setName() = function(name) { 
	return this.name = name; 
};


Person.prototype.getName() = function() { 
	return this.name; 
};

assert(markus.getName()=="Markus");
markus.setName("Hugo");
assert(markus.getName()=="Hugo");

markus = new Person("Markus");

var fritz = new Person("Fritz");
assert(fritz.getName()=="Fritz");

assert((markus.getName()!=fritz.getName()));
assert(markus.__proto__==fritz.__proto__);
