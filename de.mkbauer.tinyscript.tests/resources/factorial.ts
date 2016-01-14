
function factorial(n) {
    if (n == 1) {
        return 1;
    }
    else {
        return n * factorial(n-1);
    }
}

assert(factorial(3) == 6);
assert(factorial(20) == 2432902008176640000);