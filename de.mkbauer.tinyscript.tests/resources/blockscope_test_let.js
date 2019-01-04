// blockscope_test_let.js
"use strict";

let i = 1;

if (i == 1) {
	let i = 2;
	console.log("inner i=" + i);
}
console.log("outer i=" + i);