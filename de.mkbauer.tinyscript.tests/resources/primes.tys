/* function assert(condition) {
    if (!condition) {
        message = "Assertion failed";
        if (typeof Error !== "undefined") {
            throw new Error(message);
        }
        throw message; // Fallback
    }
}
*/

var n = 15000;

var primes = {};

for (var i = 1,n) {
	primes[i] = true;	
}
	
for (var i = 2,n/2) {
  for (var j = 2,n/i) { 
  	primes[(i*j)]=false;
  }
}
/*
for (var i = 0; i <= n; i++) {
	if (primes[i])
		console.log(i);	
}
*/
assert(primes[83]==true);
assert(primes[97]==true);
assert(primes[88]==false);

	