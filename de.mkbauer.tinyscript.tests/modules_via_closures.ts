// See http://eloquentjavascript.net/10_modules.html

// var this = {}; // -> ...until this is resolvable

var weekDay;

(function(exports) {
  var names = ["Sunday", "Monday", "Tuesday", "Wednesday",
               "Thursday", "Friday", "Saturday"];

  exports.name = function(number) {
    return names[number];
  };
  exports.number = function(name) {
    return names.indexOf(name);
  };
})(weekDay);

assert(weekDay.name(weekDay.number("Saturday")) == "Saturday"); // -> "Saturday"