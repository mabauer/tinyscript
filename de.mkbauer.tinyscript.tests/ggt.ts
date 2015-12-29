
function ggt(p, q) {
	if (p > q) {
		return ggt(p-q, q);
	}
	else { 
		if (p < q) {
			return ggt(p, q-p);
		}
		else {
			return p;
		}
	}
}

assert(ggt(8, 12)==4);
assert(ggt(10710000, 10290)==210); 

