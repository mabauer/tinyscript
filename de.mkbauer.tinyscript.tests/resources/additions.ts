// Helper function to check if arrays are element-wise equal
Array.prototype.equals = function(array) {
	// if the other array is a falsy value, return
	if (!array || !Array.isArray(array)) {
		return false;
	}

	// compare lengths - can save a lot of time
	if (this.length != array.length) {
		return false;
	}

	for (var i = 0, this.length-1) {
		if (this[i] != array[i]) {
			// Warning - two different object instances will never be equal:
			// {x:20} != {x:20}
			return false;
		}
	}
	return true;
};

// Test cases
var str = "test";
var arr1 = [ "a", "b", "c" ];
var arr2 = [ 1, 2, 3 ];

assert ([ "a", "b", "c", 1, 2, 3 ].equals(arr1 + arr2)) ;
assert ([ "test", "a", "b", "c" ].equals(str + arr1));
assert ([ "a", "b", "c", "test" ].equals(arr1 + str));
assert ("testhallo" == (str + "hallo"));
assert ("1hallo" == (1 + "hallo"));
assert (5 == (1 + 4));