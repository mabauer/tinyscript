package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.StringInputStream;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.Issue;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.google.inject.Injector;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.Function;
import de.mkbauer.tinyscript.interpreter.ResourceLimits;
import de.mkbauer.tinyscript.interpreter.ResourceMonitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.ts.Tinyscript;

/**
 * Every builtin function with edge values, in both backends: the same value, or an error in
 * the same category for catch (TODO.md, phase 1). The builtins are read from the builtin objects
 * of the interpreter (the globals, their static members and the methods of their prototypes),
 * not from a list of their own, so that a new builtin is tested as well.
 *
 * <p>Each case calls the builtin with 0 … its maximum number of arguments (not more: that is a
 * known gap, see TODO.md), each argument an edge value, all held in const variables so that the
 * checks before the run do not see them; a case they reject anyway (Math.sqrt() with too few
 * arguments) is left out. The interpreter runs each case by itself, node all cases in one program
 * (each in a try / catch of Javascript). Differences are listed in target/builtin-matrix.txt.
 */
public class BuiltinMatrixTest extends TinyscriptTranspilerTestBase {

    // Edge values, as tinyscript expressions
    private static final List<String> VALUES = List.of("NaN", "-0", "0", "1", "-1", "1.5", "Infinity", "\"\"", "\"a\"",
            "\" 12 \"", "null", "undefined", "true", "[]", "{}", "id");

    // Receivers of the methods of each prototype, by the name of the constructor
    private static final Map<String, List<String>> RECEIVERS = Map.of(
            "Object", List.of("{ a: 1 }"),
            "Function", List.of("(x, y) => x"),
            "String", List.of("\"abc\""),
            "Array", List.of("[3, 1, 2]"),
            "Map", List.of("new Map([[1, \"a\"]])"),
            "Set", List.of("new Set([1, 2])"),
            "Error", List.of("new Error(\"e\")"),
            "Number", List.of("5", "2.5"),
            "range", List.of("range(4)"));

    // Not called: print writes (its text is tested elsewhere), Function makes code from a
    // string in Javascript, random and the time are not the same twice
    private static final Set<String> LEFT_OUT = Set.of("print", "Function", "Math.random", "System.currentTimeMillis");

    private static final Path REPORT = Path.of("target", "builtin-matrix.txt");

    // The function among the edge values: declared, so that both backends know its name (the
    // names of functions in const still differ, TODO.md)
    private static final String IDENTITY = "function id(x) { return x; }\n";

    // Known differences, by the declarations and the call of a case, with the reason (TODO.md);
    // a case listed here that no longer differs fails, so that the list stays true
    private static final Map<String, String> KNOWN = Map.of(
            "const a0 = 1;\nMath.exp(a0)", "exp of StrictMath differs from V8 in the last digit for some inputs",
            "const a0 = [];\nObject.getPrototypeOf(a0)", "Array.prototype is an array in Javascript (see BuiltinMembersTest)");

    /** A call of a builtin: the declarations of the receiver and arguments, and the call. */
    record Case(String builtin, String declarations, String call) {
        String program() {
            return IDENTITY + declarations + "print(" + call + ");";
        }
    }

    /** What a backend gave: the text of the value, or the error. */
    record Outcome(String value, TinyscriptTranspilerTestBase.Failure error) {
        boolean sameAs(Outcome other) {
            if (value != null || other.value != null)
                return value != null && value.equals(other.value);
            return error.catchable() == other.error.catchable();
        }

        @Override
        public String toString() {
            return value != null ? value : error.name() + ": " + error.message() + (error.catchable() ? " (catchable)" : "");
        }
    }

    private final Injector matrixInjector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
    private int nextUri;

    @TestFactory
    Stream<DynamicTest> everyBuiltinWithEdgeValues() throws IOException {
        Map<String, List<Case>> cases = cases();
        // The interpreter first: it also finds the cases rejected before the run
        Map<Case, Outcome> interpreter = new LinkedHashMap<>();
        int rejected = 0;
        for (List<Case> list : cases.values()) {
            for (Case c : list) {
                Outcome outcome = runInterpreter(c);
                if (outcome == null)
                    rejected++;
                else
                    interpreter.put(c, outcome);
            }
        }
        Map<Case, Outcome> node = nodeAvailable() ? runNode(new ArrayList<>(interpreter.keySet())) : Map.of();
        List<String> report = new ArrayList<>();
        Map<String, List<String>> differences = new LinkedHashMap<>();
        for (Map.Entry<Case, Outcome> entry : interpreter.entrySet()) {
            Case c = entry.getKey();
            Outcome inNode = node.get(c);
            if (inNode == null)
                continue;
            String known = KNOWN.get(c.declarations() + c.call());
            boolean same = entry.getValue().sameAs(inNode);
            if (same && known == null)
                continue;
            String text = c.call() + "   [" + c.declarations().replace("\n", " ") + "]\n    interpreter: " + entry.getValue()
                    + "\n    node:        " + inNode + (known != null ? "\n    known:       " + known : "");
            report.add(text);
            if (same)
                differences.computeIfAbsent(c.builtin(), k -> new ArrayList<>()).add("no longer differs, remove from KNOWN: " + text);
            else if (known == null)
                differences.computeIfAbsent(c.builtin(), k -> new ArrayList<>()).add(text);
        }
        Files.createDirectories(REPORT.getParent());
        Files.writeString(REPORT, interpreter.size() + " cases (" + rejected + " rejected before the run), "
                + report.size() + " differ\n\n" + String.join("\n", report) + "\n", StandardCharsets.UTF_8);
        return cases.keySet().stream().map(builtin -> DynamicTest.dynamicTest(builtin, () -> {
            List<String> found = differences.getOrDefault(builtin, List.of());
            if (!found.isEmpty())
                fail(found.size() + " cases differ:\n" + found.stream().limit(10).collect(Collectors.joining("\n")));
        }));
    }

    // The cases of every builtin function, by its name (Math.sqrt, String#padStart, new Map)
    private Map<String, List<Case>> cases() {
        TinyscriptEngine engine = new TinyscriptEngine(matrixInjector.getInstance(XtextResourceSet.class));
        TSObject global = engine.getGlobalContext().getGlobalObject();
        Map<String, List<Case>> cases = new LinkedHashMap<>();
        for (String name : global.getOwnPropertyNames()) {
            TSValue value = global.get(name);
            if (LEFT_OUT.contains(name))
                continue;
            if (value instanceof BuiltinFunction function) {
                cases.put(name, calls(name, "", name, function));
                if (function instanceof BuiltinConstructor)
                    cases.put("new " + name, calls("new " + name, "", "new " + name, function));
            }
            if (value instanceof TSObject object) {
                for (String member : object.getOwnPropertyNames()) {
                    if (object.get(member) instanceof BuiltinFunction function && !member.equals("prototype")
                            && !LEFT_OUT.contains(name + "." + member))
                        cases.put(name + "." + member, calls(name + "." + member, "", name + "." + member, function));
                }
            }
            TSObject prototype = name.equals("range")
                    ? ((TSObject) ((Function) value).apply(null, new TSValue[] { TSValue.of(4) })).getPrototype()
                    : value instanceof TSObject object && object.get("prototype") instanceof TSObject p && RECEIVERS.containsKey(name) ? p : null;
            if (prototype == null)
                continue;
            for (String method : prototype.getOwnPropertyNames()) {
                if (!(prototype.get(method) instanceof BuiltinFunction function) || method.equals("constructor") || method.equals("prototype"))
                    continue;
                List<Case> list = new ArrayList<>();
                for (String receiver : RECEIVERS.get(name))
                    list.addAll(calls(name + "#" + method, "const r = " + receiver + ";\n", "r." + method, function));
                cases.put(name + "#" + method, list);
            }
        }
        return cases;
    }

    // Calls with too few arguments (one less than the minimum) up to one more than the minimum
    // (at most the maximum): with no arguments once; otherwise each edge value in all arguments,
    // and with two or more arguments each edge value in one of them (the others 1)
    private static List<Case> calls(String builtin, String receiver, String callee, BuiltinFunction function) {
        int min = function.getMinArgs();
        int max = function.getMaxArgs();
        List<Case> result = new ArrayList<>();
        for (int n = Math.max(0, min - 1); n <= Math.min(max, Math.max(min, 1) + (min == 0 ? 0 : 1)); n++) {
            if (n == 0) {
                result.add(new Case(builtin, receiver, callee + "()"));
                continue;
            }
            for (String value : VALUES) {
                result.add(call(builtin, receiver, callee, n, -1, value));
                if (n > 1) {
                    for (int position = 0; position < n; position++)
                        result.add(call(builtin, receiver, callee, n, position, value));
                }
            }
        }
        return result;
    }

    // n arguments: value in position (all if position is -1), 1 in the others
    private static Case call(String builtin, String receiver, String callee, int n, int position, String value) {
        StringBuilder declarations = new StringBuilder(receiver);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            declarations.append("const a").append(i).append(" = ").append(position < 0 || position == i ? value : "1").append(";\n");
            names.add("a" + i);
        }
        return new Case(builtin, declarations.toString(), callee + "(" + String.join(", ", names) + ")");
    }

    // The outcome of a case in the interpreter, null if it is rejected before the run
    private Outcome runInterpreter(Case c) {
        XtextResourceSet resourceSet = matrixInjector.getInstance(XtextResourceSet.class);
        Resource resource = resourceSet.createResource(URI.createURI("matrix" + (nextUri++) + ".tys"));
        try {
            resource.load(new StringInputStream(c.program()), null);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        List<Issue> issues = ((XtextResource) resource).getResourceServiceProvider().getResourceValidator()
                .validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == org.eclipse.xtext.diagnostics.Severity.ERROR))
            return null;
        // Limits only so that a mistake cannot hang the test; node has none
        ResourceLimits limits = new ResourceLimits();
        limits.setMaxStatements(1_000_000);
        limits.setMaxStringLength(10_000_000);
        limits.setMaxObjectSize(10_000_000);
        ResourceMonitor monitor = new ResourceMonitor();
        monitor.configureLimits(limits);
        TinyscriptEngine engine = new TinyscriptEngine(resourceSet, monitor);
        engine.setSandboxed(true);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        engine.defineStdOut(stdout);
        try {
            engine.execute((Tinyscript) resource.getContents().get(0));
        }
        catch (TinyscriptRuntimeException e) {
            return new Outcome(null, failure(e, stdout));
        }
        catch (RuntimeException e) {
            // A bug of the interpreter: never the same as node
            return new Outcome("<the interpreter crashed: " + e + ">", null);
        }
        String out = stdout.toString(StandardCharsets.UTF_8);
        return new Outcome(out.endsWith("\n") ? out.substring(0, out.length() - 1) : out, null);
    }

    // The outcomes of all cases in node: one program, each case a function called in a try / catch
    private Map<Case, Outcome> runNode(List<Case> cases) {
        StringBuilder program = new StringBuilder(IDENTITY + "System.matrix = [\n");
        for (Case c : cases)
            program.append("() => {\n").append(c.declarations()).append("return ").append(c.call()).append(";\n},\n");
        program.append("];\n");
        String js = transpile(parse(program.toString()));
        String driver = """
                {
                    const text = x => ("" + x).split("\\\\").join("\\\\\\\\").split("\\n").join("\\\\n");
                    const lines = [];
                    for (let i = 0; i < System.matrix.length; i++) {
                        try {
                            lines.push("ok\\t" + text(__ts_str(System.matrix[i]())));
                        }
                        catch (e) {
                            let caught = true;
                            try { __ts_catchable(e); } catch (_) { caught = false; }
                            lines.push("error\\t" + caught + "\\t" + text(e && e.name) + "\\t" + text(e && e.message));
                        }
                    }
                    process.stdout.write(lines.join("\\n") + "\\n");
                }
                """;
        int map = js.lastIndexOf("//# sourceMappingURL=");
        js = map < 0 ? js + driver : js.substring(0, map) + driver + js.substring(map);
        String[] lines = NodeJsRunner.run(js).split("\n");
        if (lines.length != cases.size())
            fail("node gave " + lines.length + " results for " + cases.size() + " cases");
        Map<Case, Outcome> result = new LinkedHashMap<>();
        for (int i = 0; i < lines.length; i++) {
            String[] parts = lines[i].split("\t", -1);
            result.put(cases.get(i), parts[0].equals("ok")
                    ? new Outcome(unescape(parts[1]), null)
                    : new Outcome(null, new Failure("", unescape(parts[2]), unescape(parts[3]), Boolean.parseBoolean(parts[1]))));
        }
        return result;
    }

    private static String unescape(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(++i);
                result.append(next == 'n' ? '\n' : next);
            }
            else
                result.append(c);
        }
        return result.toString();
    }
}
