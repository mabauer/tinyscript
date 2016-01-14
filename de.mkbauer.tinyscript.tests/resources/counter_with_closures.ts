// returns counter object
var createCounter = function createCounter() {
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
var val1 = counter1.val();
var val2 = counter2.val();
counter1.sub(4);
var val3 = counter1.val();

// the result: val1 is 14, val2 is 32, val3 is 10
assert (val1==14);
assert (val2==32);
assert (val3==10);
 
