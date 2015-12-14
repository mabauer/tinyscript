// Simple assignments

var x = 2;
var y;
y = x * x;
y = -y;

var z1, z2;
z1 = z2 = y;

// Locic

var cond1;
var cond2;
var cond3;

cond1 == cond2;
cond1 === cond2;
cond1 && cond2;
cond1 || cond2;
(cond1 && cond2) || cond3;
(x <= y) || (x > y);
!cond1;
//!(cond1);
//!(cond1 && cond2);

// Initializers
 
var banana = "banana";
var obj = {};
var coordinates = { x : 0.0, y : 0.0 };
var arr = [];
var fruit = ["orange", "apple", "banana"]; 

// Accessing properties

coordinates.x; 
coordinates["y"];
x = "x";
coordinates[x];

banana = fruit[2];

coordinates.move = function (dx, dy) { 
	this.x = this.x + dx;
	this.y = this.y +dy;
};

coordinates.move(1.0, 1.0); 

function hello (str) {
	return "hello";
}



