var fibonacci = function() {
	var i;
 	var fib = [];
 	fib[0] = 1;
 	fib[1] = 1;
 	for(i = 2, 100) {
  		fib[i] = fib[i-2] + fib[i-1];
 	}
 	return fib;
};

var bench = function(times) {
	var start = System.currentTimeMillis();
 	for (var n = 1, times) {
  		fibonacci();
 	}
 	var duration = times + ": " + (System.currentTimeMillis() - start);
 	print(duration);
};

bench(10000);
bench(100000);