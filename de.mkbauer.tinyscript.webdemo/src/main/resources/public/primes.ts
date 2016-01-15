// Compute prime numbers

var n = 15000;

var primes = [];

for (var i = 1,n) {
	primes[i] = true;	
}
	
for (var i = 2,n/2) {
  for (var j = 2,n/i) { 
  	primes[(i*j)]=false;
  }
}

for (var i = 0,n) {
	if (primes[i]) {
		print(i);	
	}
}