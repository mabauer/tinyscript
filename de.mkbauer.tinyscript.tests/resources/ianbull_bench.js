var fibonacci = function() {
	var i;
	var fib = [];
	fib[0] = 1;
	fib[1] = 1;
	for(i = 2; i <= 100; i++) {
		fib[i] = fib[i-2] + fib[i-1];
	}
	return fib;
};

var bench = function(times) {
	var start = new Date().getTime();
	for (n = 0; n < times; n++) {
		fibonacci();
	}
	var duration = times + ": " + (new Date().getTime() - start);
	console.log(duration);
};

bench(10000);
bench(100000);