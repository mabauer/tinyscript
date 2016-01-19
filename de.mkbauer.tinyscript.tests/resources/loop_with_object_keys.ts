// Loop over Object.keys()

var obj = {christianName: "Markus", name: "Bauer"};
var result = "";
for (var key : Object.keys(obj)) {
    result = result + (obj[key]);
}

// There is no garanteed order of keys.
assert ("MarkusBauer" == result || "BauerMarkus" == result);