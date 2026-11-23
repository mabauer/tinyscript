package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;

import de.mkbauer.tinyscript.TinyscriptDiagnostics;

/**
 * The errors reported for typical mistakes (many of them Javascript habits): only the one
 * that matters, with its position. The catalog grows with each improvement of the messages.
 */
public class ErrorMessagesTest extends TinyscriptTranspilerTestBase {

    // =========================================================================
    // Only the first syntax error, no follow-up errors
    // =========================================================================

    @Test
    public void testOnlyTheFirstSyntaxErrorIsReported() {
        // A C-style for loop used to give 2 syntax errors and 3 unresolved references
        List<String> errors = reportedErrors("let sum = 0;\nfor (let i = 0; i < 10; i++) {\n    sum += i;\n}");
        assertEquals(1, errors.size(), errors.toString());
        assertEquals("2:12: tinyscript's for loop goes through items: for (const i : range(n)) { … } counts from 0 to n - 1, range(a, b) from a to b - 1, range(n).reverse() from n - 1 down to 0", errors.get(0));
    }

    @Test
    public void testNoFollowUpErrorsOfAnUnsupportedConstruct() {
        assertEquals(1, reportedErrors("function f([a, b] = [1, 2]) { return a + b; }\nprint(f());").size());
        assertEquals(1, reportedErrors("try {\n    print(1);\n} catch (e) {\n    print(e);\n} finally {\n    print(2);\n}").size());
        assertEquals(1, reportedErrors("const r = /a+/;").size());
    }

    @Test
    public void testAllErrorsWithoutSyntaxErrors() {
        // Without syntax errors, each error stands on its own: all of them, in the order of the source
        List<String> errors = reportedErrors("print(b);\nprint(a);");
        assertEquals(List.of("1:7: Unknown name 'b'", "2:7: Unknown name 'a'"), errors);
    }

    // =========================================================================
    // Javascript habits get a hint what to write in tinyscript
    // =========================================================================

    private String firstError(String code) {
        List<String> errors = reportedErrors(code);
        assertEquals(1, errors.size(), errors.toString());
        return errors.get(0).replaceFirst("^\\d+:\\d+: ", "");
    }

    @Test
    public void testCStyleForLoop() {
        assertEquals("tinyscript's for loop goes through items: for (const i : range(n)) { … } counts from 0 to n - 1, range(a, b) from a to b - 1, range(n).reverse() from n - 1 down to 0",
                firstError("let sum = 0;\nfor (let i = 0; i < 10; i++) {\n    sum += i;\n}"));
    }

    @Test
    public void testLuaStyleForLoop() {
        // tinyscript had it until 0.22: for (let i = start, stop[, step]) with an inclusive stop
        String message = "tinyscript's for loop goes through items: for (const i : range(n)) { … } counts from 0 to n - 1, range(a, b) from a to b - 1, range(n).reverse() from n - 1 down to 0";
        assertEquals(message, firstError("for (let i = 0, 9) { print(i); }"));
        assertEquals(message, firstError("for (let i = 10, 1, -1) { print(i); }"));
        assertEquals(message, firstError("let i = 0;\nfor (i = 1, 3) { print(i); }"));
    }

    @Test
    public void testVar() {
        String message = "tinyscript has no var: use let, or const for a variable that is never assigned again";
        assertEquals(message, firstError("var x = 1;"));
        assertEquals(message, firstError("for (var x : [1]) { }"));
        assertEquals(message, firstError("function f() {\n    var total = 0;\n    return total;\n}"));
    }

    @Test
    public void testTripleEquals() {
        String message = "tinyscript has no === and !==: use == and !=, which never convert types";
        assertEquals(message, firstError("let x = 1;\nif (x === 1) { print(x); }"));
        assertEquals(message, firstError("let x = 1;\nif (x !== 1) { print(x); }"));
    }

    @Test
    public void testStatementWithoutBlock() {
        String message = "if, else, for and while need a block: if (…) { … }";
        assertEquals(message, firstError("let x = 1;\nif (x > 0) print(x);"));
        assertEquals(message, firstError("let x = 1;\nwhile (x > 0) x--;"));
    }

    @Test
    public void testForOfAndForIn() {
        String message = "A loop over the elements of an array, string, Map or Set is written for (const x : items) in tinyscript";
        assertEquals(message, firstError("const items = [1, 2];\nfor (const x of items) { print(x); }"));
        assertEquals(message, firstError("const o = { a: 1 };\nfor (const k in o) { print(k); }"));
    }

    @Test
    public void testMethodOfANumberLiteral() {
        assertEquals("A method of a number literal needs parentheses, as in Javascript: (5).toFixed(…)",
                firstError("print(5.toFixed(2));"));
        assertEquals("A method of a number literal needs parentheses, as in Javascript: (255).toString(…)",
                firstError("const hex = 255.toString(16);"));
    }

    @Test
    public void testUndefinedCannotBeDeclaredOrAssigned() {
        assertEquals("'undefined' is a reserved word in tinyscript and cannot be used as a name", firstError("let undefined = 1;"));
        assertEquals("'undefined' is a reserved word in tinyscript and cannot be used as a name", firstError("function f(undefined) { }"));
        assertEquals(1, reportedErrors("undefined = 1;").size());
    }

    @Test
    public void testDeleteNeedsAProperty() {
        String message = "delete needs a property: delete o.key or delete o[key]";
        assertEquals(message, firstError("let x = 1;\ndelete x;"));
        assertEquals(message, firstError("const o = {};\ndelete o?.x;"));
        assertEquals(message, firstError("function f() { return {}; }\ndelete f();"));
        assertEquals("'delete' is a reserved word in tinyscript and cannot be used as a name (it is the operator delete o.key; e.g. remove instead)", firstError("let delete = 1;"));
    }

    @Test
    public void testThisOutsideAFunction() {
        String message = "'this' is undefined outside a function (an arrow function takes 'this' from where it is written)";
        assertEquals(message, firstError("print(this);"));
        assertEquals(message, firstError("this.Math = null;"));
        assertEquals(message, firstError("const counter = { n: 0, get: () => this.n };"));
        // Inside a function (also an arrow function in one), this is fine
        assertEquals(List.of(), reportedErrors("function f() { return this; }\n"
                + "const o = { n: 1, get: function() { return [1].map(x => this.n); } };\nprint(o.get());"));
    }

    @Test
    public void testTemplateString() {
        assertEquals("Template strings (`…`) are not supported: build strings with +, e.g. \"Hello \" + name",
                firstError("const name = \"x\";\nprint(`Hello ${name}`);"));
    }

    @Test
    public void testDefaultParameter() {
        assertEquals("Only a name can have a default, not a pattern: give the parameter a name and take it apart in the function",
                firstError("function f(a, [b, c] = [1, 2]) { return a + b; }"));
        assertEquals("Only a name can have a default, not a pattern: give the parameter a name and take it apart in the function",
                firstError("const f = ({ x } = {}) => x;"));
        assertEquals("A parameter without a default cannot follow one with a default (a): put the parameters with defaults last",
                firstError("function f(a = 1, b) { return a + b; }"));
        assertEquals("b is used before its declaration: the default of a can only use the parameters before it",
                firstError("function f(a = b, b = 1) { return a; }"));
        assertEquals("a is used before its declaration: the default of a can only use the parameters before it",
                firstError("const a = 1;\nfunction f(a = a) { return a; }"));
    }

    @Test
    public void testIncrementInsideAnExpression() {
        assertEquals("++ and -- are statements, not expressions: write i++; on its own",
                firstError("let i = 0;\nconst j = i++;"));
    }

    @Test
    public void testSpreadOutsideOfCallsAndArrays() {
        assertEquals("... is only allowed in calls (f(...items)), array literals ([...a, x]), and as the last parameter or the last element of an array pattern ([first, ...rest])",
                firstError("const a = [1, 2];\nconst b = ...a;"));
    }

    @Test
    public void testRestMustBeLast() {
        assertEquals("The rest element (...rest) must be the last one", firstError("const [...rest, last] = [1, 2];"));
        assertEquals("The rest parameter (...args) must be the last one", firstError("function f(...args, last) { return last; }"));
    }

    @Test
    public void testRegularExpression() {
        assertEquals("Regular expressions are not supported (std:string has parse, findAll, words and integers)",
                firstError("const r = /a+/;"));
    }

    @Test
    public void testAndOrAsDefaultValue() {
        String or = "'||' gives true or false, not one of its operands: for a default value use ?? (e.g. options ?? {})";
        String and = "'&&' gives true or false, not one of its operands: to read a property of an object that may be null use ?. (e.g. user?.name)";
        assertEquals(or, firstError("function f(options) {\n    const cfg = options || {};\n    return cfg;\n}"));
        assertEquals(or, firstError("const list = null;\nconst items = list || [];"));
        assertEquals(or, firstError("const m = null;\nconst cache = m || new Map();"));
        assertEquals(or, firstError("const n = null;\nconst count = n || 0;"));
        assertEquals(or, firstError("const s = null;\nconst name = s || (\"anonymous\");"));
        assertEquals(or, firstError("const x = null;\nconst y = x || -1;"));
        assertEquals(or, firstError("const f = null;\nconst g = f || (x => x);"));
        assertEquals(and, firstError("const user = null;\nconst label = user && \"name\";"));
        assertEquals(and, firstError("const ok = {} && true;"));
        // Conditions and booleans are fine
        assertEquals(0, reportedErrors("const a = null; const b = true;\nif (a || b) { print(1); }\nconst c = a && b || !b;\nconst d = (a ?? {}) || b;").size());
    }

    @Test
    public void testUnsupportedKeywords() {
        assertEquals("finally is not supported in tinyscript: there is only try { ... } catch (e) { ... }",
                firstError("try {\n    print(1);\n} catch (e) {\n    print(e);\n} finally {\n    print(2);\n}"));
        assertEquals("Duplicate identifier", firstError("try { print(1); } catch (e) { let e = 2; }"));
        assertEquals("switch is not supported in tinyscript: use if / else if",
                firstError("const x = 1;\nswitch (x) { case 1: print(1); }"));
    }

    @Test
    public void testPatternsAndKeysThatTinyscriptLeavesOut() {
        assertEquals("Computed keys ({ [key]: value }) are not supported: create the object first, then o[key] = value;",
                firstError("const k = \"x\";\nconst o = { [k]: 1 };"));
        assertEquals("Computed keys ({ [key]: value }) are not supported: create the object first, then o[key] = value;",
                firstError("const k = \"x\";\nconst o = { a: 1, [k + \"y\"]: 2 };"));
        assertEquals("Renaming in a pattern ({ a: … }) is not supported: use the property's name ({ a }), or read it with o.a",
                firstError("const o = { a: 1 };\nconst { a: b } = o;"));
        assertEquals("Renaming in a pattern ({ value: … }) is not supported: use the property's name ({ value }), or read it with o.value",
                firstError("function f({ value: v }) { return v; }"));
        String noPatternAssignment = "Patterns can only declare variables (const [a, b] = pair;), not assign to existing ones: "
                + "assign one by one, with a temporary variable to swap";
        assertEquals(noPatternAssignment, firstError("let a = 1; let b = 2;\n[a, b] = [b, a];"));
        assertEquals(noPatternAssignment, firstError("let x = 0;\n({ x } = { x: 1 });"));
        // Object literals and arrays elsewhere are fine
        assertEquals(List.of(), reportedErrors("const k = \"x\";\nconst o = { a: [1, 2], b: { c: 1 } };\nprint([o, { k }], f({ a: 1 }));\nfunction f({ a }) { return a; }"));
    }

    @Test
    public void testAWarningForANameThatHidesABuiltin() {
        assertEquals(List.of("2:12: 'range' hides the builtin range here: choose another name"),
                reportedWarnings("const spans = [];\nfor (const range : spans) { print(range); }"));
        assertEquals(List.of("1:7: 'Number' hides the builtin Number here: choose another name",
                "2:10: 'print' hides the builtin print here: choose another name"),
                reportedWarnings("class Number { }\nfunction print(x) { }"));
        assertEquals(List.of(), reportedWarnings("const numbers = [1];\nfunction show(value) { print(value); }"));
    }

    @Test
    public void testReservedWordsAsNames() {
        assertEquals("'from' is a reserved word in tinyscript and cannot be used as a name (it belongs to import … from …; e.g. source, start or origin instead)", firstError("const from = 1;"));
        assertEquals("'as' is a reserved word in tinyscript and cannot be used as a name (it belongs to import * as …; e.g. alias instead)", firstError("function f(as) { return 1; }"));
        assertEquals("'import' is a reserved word in tinyscript and cannot be used as a name", firstError("let import = 2;"));
        assertEquals("'function' is a reserved word in tinyscript and cannot be used as a name", firstError("const function = 1;"));
        assertEquals("'from' is a reserved word in tinyscript and cannot be used as a name (it belongs to import … from …; e.g. source, start or origin instead)", firstError("function f(a, from) { return a; }"));
    }

    @Test
    public void testSpreadAndRestInObjects() {
        assertEquals("Rest elements (...) in object patterns are not supported: take the properties you need, e.g. const { x, y } = o;",
                firstError("const o = { x: 1, y: 2 };\nconst { x, ...others } = o;"));
        assertEquals("Spread (...) in object literals is not supported: copy the properties with Object.assign, e.g. Object.assign({}, o, { y: 2 })",
                firstError("const o = { x: 1 };\nconst p = { ...o, y: 2 };"));
    }

    @Test
    public void testKeywordsAreFineAsPropertyNames() {
        // Like Javascript: only names of variables are reserved, not names of properties
        String code = "const range = { from: 1, to: 5, as: \"x\", import: true, if: 2 };\n"
                + "print(range.from + range.to, range.as, range.import, range.if);";
        assertEquals("6 x true 2\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testReservedWordsAreFineAsPropertyNames() {
        String code = "const o = { class: \"a\", case: 1 }; print(o.class + o.case);";
        assertEquals("a1\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    // =========================================================================
    // The other syntax errors in plain words
    // =========================================================================

    @Test
    public void testGeneralSyntaxErrors() {
        assertEquals("Missing ';' at the end of line 1", firstError("let x = 1\nprint(x);"));
        assertEquals("Missing ';' before 'print'", firstError("let x = 1 print(x);"));
        assertEquals("Unexpected end of input, expected '}'", firstError("function f(a) {\n    return a;\n"));
        assertEquals("Unexpected ')', expected an expression", firstError("print(1 + );"));
    }

    // =========================================================================
    // Unknown names, with a suggestion
    // =========================================================================

    @Test
    public void testUnknownNameWithASuggestion() {
        assertEquals("Unknown name 'valeus' (did you mean 'values'?)", firstError("const values = [1, 2];\nprint(valeus.length);"));
        assertEquals("Unknown name 'Print' (did you mean 'print'?)", firstError("Print(1);"));
        assertEquals("Unknown name 'Mtah' (did you mean 'Math'?)", firstError("print(Mtah.sqrt(4));"));
        // Local names of functions are visible inside them
        assertEquals("Unknown name 'totl' (did you mean 'total'?)", firstError("function sum(numbers) { let total = 0; for (const n : numbers) { totl += n; } return total; }"));
    }

    @Test
    public void testUnknownNameWithoutASuggestion() {
        assertEquals("Unknown name 'xyz'", firstError("const values = [1];\nprint(xyz);"));
        // Names declared later or in another function are not visible, so they are no suggestion
        assertEquals("Unknown name 'countr'", firstError("print(countr);\nconst counter = 1;"));
        assertEquals("Unknown name 'inner'", firstError("function f() { const inner = 1; }\nprint(inner);"));
    }

    // =========================================================================
    // Commas: after the last element only in literals, no empty elements
    // =========================================================================

    @Test
    public void testACommaAfterTheLastElementOfALiteral() {
        // As in Javascript, multi-line literals can have a comma after each element
        String code = "const digits = [\n    \"abc\",\n    \"cf\",\n];\n"
                + "const point = { x: 1, y: 2, };\n"
                + "print(digits.length, digits, point, [], {});";
        assertEquals("2 [\"abc\", \"cf\"] {x: 1, y: 2} [] {}\n", interpreterOutput(code));
        assertSameOutput(code);
    }

    @Test
    public void testACommaAfterTheLastElementOnlyInLiterals() {
        String message = "A comma after the last element is only allowed in array and object literals";
        assertEquals(message, firstError("print(Math.max(1, 2,));"));
        assertEquals(message, firstError("function f(a, b,) { return a; }"));
        assertEquals(message, firstError("const f = (a, b,) => a;"));
        assertEquals(message, firstError("const f = (a = [1, 2,], b,) => a;"));
        assertEquals(message, firstError("print([1].map((x,) => x));"));
        assertEquals(message, firstError("const [a, b,] = [1, 2];"));
        assertEquals(message, firstError("const { a, } = { a: 1 };"));
    }

    @Test
    public void testNoEmptyElements() {
        // Javascript's empty elements (holes): [1, , 2] has length 3, [, 1] length 2
        assertEquals("Arrays have no empty elements in tinyscript: write undefined, e.g. [1, undefined, 2]",
                firstError("print([1, , 2]);"));
        assertEquals("Arrays have no empty elements in tinyscript: write undefined, e.g. [1, undefined, 2]",
                firstError("print([, 1]);"));
        assertEquals("A pattern cannot skip elements: give each element a name", firstError("const [, b] = [1, 2];"));
        // These were accepted silently: the comma before the first element was ignored
        assertEquals("Unexpected ',': an element is missing before it", firstError("print(Math.max(, 3, 4));"));
        assertEquals("Unexpected ',': an element is missing before it", firstError("function f(, a) { return a; }"));
        assertEquals("Unexpected ',': an element is missing before it", firstError("print({ , a: 1 });"));
    }

    // =========================================================================
    // Runtime errors name the expression
    // =========================================================================

    private void assertRuntimeError(String expectedMessage, String code) {
        TinyscriptRuntimeException e = assertThrows(TinyscriptRuntimeException.class, () -> interpreterOutput(code));
        assertEquals(expectedMessage, e.getMessage());
    }

    @Test
    public void testPropertyOfUndefinedOrNull() {
        assertRuntimeError("Cannot read 'length' of o.lst: it is undefined", "const o = { list: [1, 2] };\nconst total = o.lst.length;");
        assertRuntimeError("Cannot read [0] of grid[5]: it is undefined", "const grid = [[1, 2], [3, 4]];\nprint(grid[5][0]);");
        assertRuntimeError("Cannot read 'c' of o.a.b: it is undefined", "function f(o) { return o.a.b.c; }\nprint(f({ a: {} }));");
        assertRuntimeError("Cannot read 'push' of list: it is null", "const list = null;\nlist.push(1);");
        assertRuntimeError("Cannot read ['key'] of m: it is undefined", "let m;\nprint(m[\"key\"]);");
        assertRuntimeError("Cannot set 'debug' of o.config: it is undefined", "const o = {};\no.config.debug = true;");
    }

    @Test
    public void testPropertyOfANumberOrBoolean() {
        assertRuntimeError("Cannot read 'length' of n: numbers only have the methods toFixed and toString", "const n = 5;\nprint(n.length);");
        assertRuntimeError("Cannot set 'x' of n: numbers cannot be changed", "const n = 5;\nn.x = 1;");
        assertRuntimeError("Cannot read 'x' of flag: booleans have no properties", "const flag = true;\nprint(flag.x);");
    }

    @Test
    public void testOperatorsNameTheWrongOperand() {
        assertRuntimeError("'*' needs two numbers, but shape.height is undefined",
                "function area(shape) { return shape.width * shape.height; }\nprint(area({ width: 2 }));");
        assertRuntimeError("'*' needs two numbers, but count is a string", "const count = \"3\";\nprint(count * 2);");
        assertRuntimeError("'-' needs two numbers, but a is a string and b is an array", "const a = \"x\"; const b = [];\nprint(a - b);");
        assertRuntimeError("'+' adds two numbers, two arrays, or a string and a value, but x is a number and true is a boolean",
                "const x = 1;\nprint(x + true);");
        assertRuntimeError("'+' can only add an array to another array or to a string, but 1 is a number", "print([1] + 1);");
        assertRuntimeError("'<' compares two numbers or two strings, but 1 is a number and \"2\" is a string", "print(1 < \"2\");");
        // NaN is a number for typeof, but cannot be compared (found with parseInt of an empty line)
        assertRuntimeError("'<' cannot compare NaN, but ints[1] is NaN (e.g. from parseInt or Number of a text that is no number)",
                "const ints = [1, parseInt(\"\")];\nprint(ints[0] < ints[1]);");
        assertRuntimeError("'<' compares two numbers or two strings, but ints[0] is a number and ints[1] is undefined",
                "const ints = [1];\nprint(ints[0] < ints[1]);");
        assertRuntimeError("'-' needs a number, but s is a string", "const s = \"a\";\nprint(-s);");
        assertRuntimeError("'+=' adds two numbers, two arrays, or a string and a value, but total is undefined and 1 is a number", "let total;\ntotal += 1;");
    }

    @Test
    public void testConditionsNameTheExpression() {
        assertRuntimeError("The condition items.length is a number, not a boolean: compare it, e.g. items.length > 0",
                "const items = [];\nif (items.length) { print(\"some\"); }");
        assertRuntimeError("The condition name is a string, not a boolean: compare it, e.g. name != \"\"",
                "const name = \"x\";\nwhile (name) { break; }");
        assertRuntimeError("The condition count is a number, not a boolean: compare it, e.g. count > 0",
                "const count = 1;\nprint(!count);");
    }

    @Test
    public void testLongExpressionsAreShortened() {
        assertRuntimeError("Cannot read 'x' of compute(someLongArgumentName, anotherLongArgumen…: it is null",
                "function compute(a, b) { return null; } const someLongArgumentName = 1; const anotherLongArgumentName = 2;\n"
                + "print(compute(someLongArgumentName, anotherLongArgumentName).x);");
    }

    // =========================================================================
    // Excerpt of the source
    // =========================================================================

    @Test
    public void testExcerptMarksTheColumn() {
        String source = "let x = 1;\nif (x === 1) { print(x); }";
        assertEquals("    if (x === 1) { print(x); }\n          ^\n", TinyscriptDiagnostics.excerpt(source, 2, 7));
        assertEquals("", TinyscriptDiagnostics.excerpt(source, 9, 1));
    }

    @Test
    public void testExcerptKeepsTabs() {
        // Tabs before the column stay tabs, so that the marker lines up in a terminal
        assertEquals("    \tx = ;\n    \t    ^\n", TinyscriptDiagnostics.excerpt("\tx = ;", 1, 6));
    }
}
