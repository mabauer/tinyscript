function assert(condition) {
	if (!condition) {
		message = "Assertion failed";
		if (typeof Error !== "undefined") {
			throw new Error(message);
		}
		throw message; // Fallback
	}
}

Array.prototype.equals = function(array) {
	// if the other array is a falsy value, return
	if (!array || !Array.isArray(array))
		return false;

	// compare lengths - can save a lot of time
	if (this.length != array.length)
		return false;

	for (var i = 0, l = this.length; i < l; i++) {
		if (this[i] != array[i]) {
			// Warning - two different object instances will never be equal:
			// {x:20} != {x:20}
			return false;
		}
	}
	return true;
}
// Hide method from for-in loops
Object.defineProperty(Array.prototype, "equals", {
	enumerable : false
});

function __ts_add(obj1, obj2) {
	var result;
	if (Array.isArray(obj1)) {
		if (Array.isArray(obj2)) {
			return obj1.concat(obj2);
		} else {
			result = obj1.slice(0);
			result.push(obj2);
			return result;
		}
	}
	if (Array.isArray(obj2)) {
		result = obj2.slice();
		result.unshift(obj1);
		return result;
	}
	return obj1 + obj2;
}

var str = "test";
var arr1 = [ "a", "b", "c" ];
var arr2 = [ 1, 2, 3 ];

assert([ "a", "b", "c", 1, 2, 3 ].equals(__ts_add(arr1, arr2)));
assert([ "test", "a", "b", "c" ].equals(__ts_add(str, arr1)));
assert([ "a", "b", "c", "test" ].equals(__ts_add(arr1, str)));
assert("testhallo" == __ts_add(str, "hallo"));
assert("1hallo" == __ts_add(1, "hallo"));
assert(5, __ts_add(1, 4));
