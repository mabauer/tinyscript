// Counter with closures
// see: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Closures

// Create a counter object
function createCounter() {
    var value = 0; // used by add, sub, val
  	return {
    	add: function(x) { value = value + x; },
    	sub: function(x) { value = value - x; },
    	val: function() { return value;}
  	};
};
 
// create two counters
var counter1 = createCounter();
var counter2 = createCounter();
 
// use the counters
counter1.add(14);
counter2.add(32);
print(counter1.val()); // 14
print(counter2.val()); // 32
counter1.sub(4);
print(counter1.val()); // 10
