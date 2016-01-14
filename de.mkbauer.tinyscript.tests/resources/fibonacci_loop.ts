function fibonacci(n) {
    var h = 1, g = 1, f = 1;
    for (var i = 2, n) {
        h = g;
        g = f;
        f = g + h;
    }
    return f;
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
assert (fibonacci(25) == 121393); 
assert (fibonacci(32) == 3524578);
assert (fibonacci(42) == 433494437);
assert (fibonacci(50) == 20365011074);