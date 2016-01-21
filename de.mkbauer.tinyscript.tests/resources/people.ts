// Creates an array of "people" and to some searching on it.

// Size of the array
var N = 100000;

// Repetions
var R = 1;

function randomInt(min, max) {
    return Math.round(Math.random() * (max - min -1)) + min;
}

var CHRISTIAN_NAMES = ['Anna', 'Emilia', 'Emma', 'Hannah', 'Lea', 'Lena', 'Leonie', 
	'Marie', 'Mia', 'Sophia', 'Ben', 'Elias', 'Fynn', 'Jonas', 'Leon', 'Louis', 
	'Luca', 'Lukas', 'Noah', 'Paul'];
var NAMES = ['Mueller', 'Schmidt', 'Schneider', 'Fischer', 'Meyer', 'Weber', 'Hofmann', 
	'Wagner', 'Becker', 'Schulz', 'Schaefer', 'Koch', 'Bauer', 'Richter', 'Klein', 
	'Schroeder', 'Wolf', 'Neumann', 'Schwarz'];
var CITIES = ['Hambug', 'Karlsruhe', 'Muenchen', 'Stuttgart', 'New-York', 
	'Konstanz', 'Paris', 'Madrid', 'Rom', 'Mailand', 'Genua', 'Stockholm', 
	'Kopenhagen', 'Washington', 'Moskau', 'Schenkenzell'];

function Person() {}

Person.prototype.toString = function () {
    return this.christianName + " " + this.name + ", " + this.street + ", " + this.city;
};
    
function createPerson() {
	var result = new Person();
	result.name = NAMES[randomInt(0, NAMES.length)] + "-" + NAMES[randomInt(0, NAMES.length)];
	result.christianName = CHRISTIAN_NAMES[randomInt(0, CHRISTIAN_NAMES.length)] 
		+ "-" + CHRISTIAN_NAMES[randomInt(0, CHRISTIAN_NAMES.length)];
	result.street = CHRISTIAN_NAMES[randomInt(0, CHRISTIAN_NAMES.length)] 
		+ "-" + NAMES[randomInt(0, NAMES.length)] + "-Str. " + randomInt(1, 1000);
	result.city = randomInt(10000, 90000) + " " + CITIES[randomInt(0, CITIES.length)];
	return result;
}

function createPeople(n) {
	var people = [];
	for (var i = 0, n-1) {
		people.push(createPerson());
	}
    return people;
}

function countBauers(people) {
    var result = 0;
	for (var i = 0, people.length - 1) {
        if (people[i].name.indexOf("Bauer") >= 0) {
            result = result +1;
        }
    }
    return result;
}

function test() {         
	for (var i = 0, R-1) {
		var people = createPeople(N);
		print(countBauers(people));
	}
}

var time = System.currentTimeMillis();
test();
time = System.currentTimeMillis() - time;
print(time + "ms");