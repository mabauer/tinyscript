// Simple linked list implementation

function insert(element, list) {
	var result = {};
	result.next = list;
	result.value = element;
	return result;
}

function isempty(list) {
	return (!list.value && !list.next);
}

function islast(list) {
	return (!list.next || !list.next.next);
}

function head(list) {
	return list.value;
}

function tail(list) {
	return list.next;
}

function equals(list1, list2) {
	if (isempty(list1) && isempty(list2)) {
		return true;
	}
	if (isempty(list1) || isempty(list2)) {
		return false;
	}
	return (list1.value == list2.value) && equals(list1.next, list2.next);
}

function max(list) {
	var current = list.value;
	if  (islast(list)) {
		return current;
	}
	var next = max(list.next);
	if (current > next) {
		return current;
	}
	else {
		return next;
	}
}

function length(list) {
	if (isempty(list)) {
		return 0;
	}
	else {
		return length(list.next) + 1;
	}
}

function map(f, list) {
	if (isempty(list)) {
		return {};
	}
	else {
		return insert(f(list.value), map(f, list.next));
	}
}

function filter(p, list) {
	if (isempty(list)) {
		return {};
	}
	else {
		if (p(list.value)) {
			return insert(list.value, filter(p, list.next));
		}
		else {
			return filter(p, list.next);
		}
	}
}

function fromarray(a) {
	var result = {};
	for (var i = a.length, 1, -1) {
		result = insert(a[i-1], result);
	}
	return result;
}	

var list = {};
list = insert(2, list);
assert(length(list)==1);
assert(head(list)==2);
assert(isempty(tail(list)));
assert(max(list)==2);

list = insert(10, list);
list = insert(9, list);
list = insert(5, list);
list = insert(1, list);
list = insert(24, list);
list = insert(3, list);
list = insert(12, list);
assert(length(list)==8);
assert(head(list)==12);
assert(length(tail(list))==7);
assert(max(list)==24);

assert(!equals(list, tail(list)));
assert(equals(list, insert(head(list), tail(list))));

function iseven(x) {
	return (x % 2 == 0);
}

function isodd(x) {
	return !iseven(x);
}

assert(length(filter(iseven, list))==4);
assert(length(filter(isodd, list))==4);

var sequence = [2, 10, 9, 5, 1, 24, 3, 12, 
				8, 2, 10, 9, 5, 1, 24, 3, 
				12, 8, 2, 10, 9, 5, 1, 25, 
				3, 12, 50, 2, 10, 9, 5, 1, 
				23, 3, 12, 8, 4, 11, 22, 13];
assert(sequence.length==40);
var longlist = fromarray(sequence);

assert(length(longlist)==40);
assert(max(longlist)==50);

var negatives = map(function(x) {return -x;}, longlist);

assert(length(negatives)==40);
assert(max(negatives)==-1);

function square(x) {
	return x*x;
}

assert(max(map(square, longlist))==50*50);
assert(max(map(square, negatives))==50*50);

assert(equals(map(square, longlist), map(square, negatives))); 

