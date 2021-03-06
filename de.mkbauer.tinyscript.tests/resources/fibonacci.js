function assert(condition) {
    if (!condition) {
        message = "Assertion failed";
        if (typeof Error !== "undefined") {
            throw new Error(message);
        }
        throw message; // Fallback
    }
}

// Recursive implementation of the fibonacci numbers.
/* function fibonacci(n) {
	if (n == 0) {
    	return 1;
    }
    if (n == 1) {
    	return 1;
    }
    return fibonacci(n-2) + fibonacci(n-1);
}
*/
function fibonacci(n) {
	if (n == 0 || n == 1) {
    	return 1;
    }
    else {
    	return fibonacci(n-2) + fibonacci(n-1);
	}
}

assert (fibonacci(0) == 1);
assert (fibonacci(1) == 1);
assert (fibonacci(2) == 2);
assert (fibonacci(3) == 3);
assert (fibonacci(4) == 5);
assert (fibonacci(5) == 8);
assert (fibonacci(6) == 13);
assert (fibonacci(7) == 21); 
assert (fibonacci(21) == 17711);
assert (fibonacci(32) == 3524578);
// assert (fibonacci(42) == 433494437);
// assert (fibonacci(50) == 20365011074);

