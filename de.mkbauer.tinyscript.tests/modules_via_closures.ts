// See http://eloquentjavascript.net/10_modules.html

var this = {}; // -> ...until this is resolvable

(function(exports) {
  var names = ["Sunday", "Monday", "Tuesday", "Wednesday",
               "Thursday", "Friday", "Saturday"];

  exports.name = function(number) {
    return names[number];
  };
  exports.number = function(name) {
    return names.indexOf(name);
  };
})(this.weekDay);

assert(this.weekDay.name(this.weekDay.number("Saturday")) == "Saturday"); // -> "Saturday"