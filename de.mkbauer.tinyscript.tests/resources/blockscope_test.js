// blockscope_test.js

var i = 1;

if (i == 1) {
	var i = 2;
	console.log("inner i=" + i);
}
console.log("outer i=" + i);