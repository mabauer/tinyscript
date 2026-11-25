package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * The members of the builtin objects, their prototypes and typical values in both backends
 * (TODO.md, phase 1): what a script can read along the prototype chain. Javascript has members
 * that tinyscript does not have (String#concat, Math.cosh …); a program that uses them runs only
 * in node. They are listed here as known, so that any other difference, in either direction,
 * fails.
 */
public class BuiltinMembersTest extends TinyscriptTranspilerTestBase {

    // The values whose members are compared, as tinyscript expressions
    private static final List<String> VALUES = List.of("Object", "Object.prototype", "Function", "Function.prototype",
            "String", "String.prototype", "Array", "Array.prototype", "Map", "Map.prototype", "Set", "Set.prototype",
            "Error", "Error.prototype", "Number", "Number.prototype", "Math", "JSON", "System", "range", "range(3)",
            "print", "parseInt", "Math.sqrt", "[].map", "f", "f.prototype", "arrow", "P", "P.prototype", "new P(1)",
            "new P(1).area", "{ a: 1 }", "[1, 2]", "new Map()", "new Set()", "new Error(\"e\")", "Object.create(null)");

    private static final String PROGRAM_START = "function f(a) { return a; }\n"
            + "const arrow = x => x;\n"
            + "class P { constructor(x) { this.x = x; } area() { return 1; } }\n"
            + "function members(value) {\n"
            + "  const names = [];\n"
            + "  let o = value;\n"
            + "  while (o != null) {\n"
            + "    for (const n : Object.getOwnPropertyNames(o)) { if (!names.includes(n)) { names.push(n); } }\n"
            + "    o = Object.getPrototypeOf(o);\n"
            + "  }\n"
            + "  names.sort();\n"
            + "  return names.join(\" \");\n"
            + "}\n";

    // Members of Javascript that tinyscript does not have, by the value (only in node)
    private static final Map<String, String> JAVASCRIPT_ONLY = new LinkedHashMap<>();

    // Members of Javascript's Object.prototype and Function.prototype, on every object / function
    private static final String OBJECT_EXTRAS = "__defineGetter__ __defineSetter__ __lookupGetter__ __lookupSetter__ __proto__ "
            + "isPrototypeOf propertyIsEnumerable toLocaleString valueOf";
    private static final String FUNCTION_EXTRAS = "arguments bind caller";

    static {
        JAVASCRIPT_ONLY.put("Object", "defineProperties freeze getOwnPropertyDescriptors getOwnPropertySymbols groupBy hasOwn is "
                + "isExtensible isFrozen isSealed preventExtensions seal setPrototypeOf");
        JAVASCRIPT_ONLY.put("String.prototype", "anchor big blink bold concat fixed fontcolor fontsize isWellFormed italics link "
                + "localeCompare match matchAll normalize search small strike sub substr sup toLocaleLowerCase toLocaleUpperCase "
                + "toWellFormed trimLeft trimRight");
        JAVASCRIPT_ONLY.put("Array", "from fromAsync of");
        JAVASCRIPT_ONLY.put("Array.prototype", "concat copyWithin keys reduceRight toSorted toSpliced values with");
        JAVASCRIPT_ONLY.put("Map", "groupBy");
        JAVASCRIPT_ONLY.put("Map.prototype", "getOrInsert getOrInsertComputed size");
        JAVASCRIPT_ONLY.put("Set.prototype", "difference entries intersection isDisjointFrom isSubsetOf isSupersetOf keys size "
                + "symmetricDifference union");
        JAVASCRIPT_ONLY.put("Number", "NEGATIVE_INFINITY NaN POSITIVE_INFINITY isNaN isSafeInteger parseFloat parseInt");
        JAVASCRIPT_ONLY.put("Number.prototype", "toExponential toPrecision");
        JAVASCRIPT_ONLY.put("Math", "LN10 LN2 LOG10E LOG2E SQRT1_2 SQRT2 acosh asinh atanh clz32 cosh expm1 f16round fround imul "
                + "log1p sinh tanh");
        // The stack of an error: the uncaught handler of the prelude needs it
        JAVASCRIPT_ONLY.put("new Error(\"e\")", "stack");
        // In Javascript, String.prototype is a string and Array.prototype an array; the length of
        // a range is a getter of its prototype in node, computed by the range in the interpreter
        JAVASCRIPT_ONLY.merge("String.prototype", "length", (a, b) -> a + " " + b);
        JAVASCRIPT_ONLY.merge("Array.prototype", "length", (a, b) -> a + " " + b);
        JAVASCRIPT_ONLY.put("range(3)", "length");
    }

    // Values that inherit the members of another (in both backends)
    private static final Map<String, String> INHERITS = Map.of("String", "Function", "Array", "Function",
            "Map", "Function", "Set", "Function", "Number", "Function", "[1, 2]", "Array.prototype",
            "new Map()", "Map.prototype", "new Set()", "Set.prototype");

    @Test
    public void testTheMembersAreTheSameButForJavascriptsOwn() {
        StringBuilder program = new StringBuilder(PROGRAM_START);
        for (String value : VALUES)
            program.append("print(members(").append(value).append("));\n");
        String code = program.toString();
        List<String> interpreter = interpreterOutput(code).lines().toList();
        assertEquals(VALUES.size(), interpreter.size(), String.join("\n", interpreter));
        if (!nodeAvailable())
            return;
        List<String> node = NodeJsRunner.run(transpile(parse(code))).lines().toList();
        assertEquals(VALUES.size(), node.size(), String.join("\n", node));
        List<String> differences = new ArrayList<>();
        for (int i = 0; i < VALUES.size(); i++) {
            String value = VALUES.get(i);
            Set<String> inInterpreter = names(interpreter.get(i));
            Set<String> inNode = names(node.get(i));
            Set<String> known = knownFor(value, inNode);
            Set<String> onlyInterpreter = new TreeSet<>(inInterpreter);
            onlyInterpreter.removeAll(inNode);
            Set<String> onlyNode = new TreeSet<>(inNode);
            onlyNode.removeAll(inInterpreter);
            Set<String> unexpected = new TreeSet<>(onlyNode);
            unexpected.removeAll(known);
            // Only its own known members: an inherited one may be in both ([1, 2].length)
            Set<String> stale = names(JAVASCRIPT_ONLY.getOrDefault(value, ""));
            stale.removeAll(onlyNode);
            if (!onlyInterpreter.isEmpty() || !unexpected.isEmpty() || !stale.isEmpty())
                differences.add(value + ":" + (onlyInterpreter.isEmpty() ? "" : " only in the interpreter " + onlyInterpreter)
                        + (unexpected.isEmpty() ? "" : " only in node " + unexpected)
                        + (stale.isEmpty() ? "" : " known, but no longer only in node " + stale));
        }
        if (!differences.isEmpty())
            fail("The members differ:\n" + String.join("\n", differences));
    }

    // The known members of Javascript a value has in node: its own, those of what it inherits,
    // and those of Object.prototype / Function.prototype if it has them
    private static Set<String> knownFor(String value, Set<String> inNode) {
        Set<String> known = new TreeSet<>();
        for (String v = value; v != null; v = INHERITS.get(v))
            known.addAll(names(JAVASCRIPT_ONLY.getOrDefault(v, "")));
        if (inNode.contains("isPrototypeOf"))
            known.addAll(names(OBJECT_EXTRAS));
        if (inNode.contains("bind"))
            known.addAll(names(FUNCTION_EXTRAS));
        return known;
    }

    private static Set<String> names(String line) {
        Set<String> result = new TreeSet<>();
        for (String name : line.trim().split(" +")) {
            if (!name.isEmpty())
                result.add(name);
        }
        return result;
    }

    @Test
    public void testFunctionNamesAsInJavascript() {
        // The name of the function, or of what it is written to
        String code = "function f(a) { }\n"
                + "const arrow = x => x;\n"
                + "let fe = function (q) { return q; };\n"
                + "const named = function inner() { };\n"
                + "const o = { m(k) { return k; }, n: x => x, p: function () { }, \"two words\": () => 1 };\n"
                + "class P { constructor(x) { this.x = x; } area() { return 1; } }\n"
                + "let later;\nlater = () => 1;\n"
                + "function g(step = () => 1) { return step; }\n"
                + "const anonymous = [x => x][0];\n"
                + "print(f.name, arrow.name, fe.name, named.name, o.m.name, o.n.name, o.p.name, o[\"two words\"].name);\n"
                + "print(P.name, new P(1).area.name, later.name, g().name, anonymous.name == \"\", Math.sqrt.name, print.name, Map.name);\n"
                + "print(arrow, fe, o.n, new P(1).area, later, anonymous, f.length, arrow.length);";
        assertSameOutput("f arrow fe inner m n p two words\n"
                + "P area later step true sqrt print Map\n"
                + "[Function: arrow] [Function: fe] [Function: n] [Function: area] [Function: later] [Function] 1 1\n", code);
        // Read-only, as before
        assertSameFailure("const h = x => x;\nh.name = \"g\";");
    }

    @Test
    public void testTheLayoutOfTheMembers() {
        // constructor comes from the prototype, arrow functions and methods have no prototype
        String code = "const a = [1];\nconst m = new Map();\nconst arrow = x => x;\nconst o = { m() { } };\n"
                + "class P { area() { } }\nfunction f() { }\n"
                + "print(a.hasOwnProperty(\"constructor\"), a.constructor == Array, m.constructor == Map, ({}).constructor == Object);\n"
                + "print(arrow.prototype, o.m.prototype, new P().area.prototype, typeof f.prototype, typeof P.prototype, Function.prototype.prototype);\n"
                + "print(Object.getOwnPropertyNames(a), Object.getOwnPropertyNames(f), Object.keys(a), Object.getOwnPropertyNames(new Error(\"e\")).includes(\"message\"));";
        assertSameOutput("false true true true\n"
                + "undefined undefined undefined object object undefined\n"
                + "[\"0\", \"length\"] [\"length\", \"name\", \"prototype\"] [\"0\"] true\n", code);
        // A range keeps its numbers to itself
        assertSameOutput("undefined undefined 3\n", "const r = range(3);\nprint(r.start, r.end, r.length);");
    }
}
