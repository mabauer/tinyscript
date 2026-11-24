package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.StringInputStream;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;

import com.google.inject.Injector;

import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import de.mkbauer.tinyscript.TinyscriptDiagnostics;
import de.mkbauer.tinyscript.TinyscriptSyntaxError;
import de.mkbauer.tinyscript.generator.TinyscriptGenerator;
import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.FileSystemModuleLoader;
import de.mkbauer.tinyscript.interpreter.TinyscriptArgumentError;
import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.interpreter.TinyscriptError;
import de.mkbauer.tinyscript.interpreter.TinyscriptIOError;
import de.mkbauer.tinyscript.interpreter.TinyscriptJsonError;
import de.mkbauer.tinyscript.interpreter.TinyscriptStackOverflowError;
import de.mkbauer.tinyscript.interpreter.ResourceLimits;
import de.mkbauer.tinyscript.interpreter.ResourceMonitor;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.ts.Tinyscript;

/**
 * Base class for differential transpiler tests and interpreter reference tests.
 *
 * <h2>Level 1 — focused feature tests (most debuggable)</h2>
 * <p>Use {@link #interpreterOutput(String)} to run a snippet through the
 * interpreter and compare it with JUnit's {@code assertEquals}.  Failure
 * message: {@code expected:<8> but was:<7>} — points directly to the buggy
 * AST visitor in {@code TinyscriptEngine}.
 *
 * <p>Use {@link #assertSameOutput(String)} to run a snippet through both the
 * interpreter and node.js and verify that their stdout is identical.  On
 * failure the generated JavaScript is shown so it can be pasted into {@code node}
 * directly.
 *
 * <h2>Level 2 — program-level integration tests</h2>
 * <p>Use {@link #assertTranspilerCorrectFromFile(String)} or
 * {@link #assertTranspilerCorrect(String)} to run a complete program through
 * both the interpreter (must not throw) and node (must exit 0).
 */
public abstract class TinyscriptTranspilerTestBase {

    private static final String FILE_EXTENSION = "tys";

    private Injector injector;
    private TinyscriptGenerator generator;
    private XtextResourceSet resourceSet;

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @BeforeEach
    public void setUpInjector() {
        injector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
        generator = injector.getInstance(TinyscriptGenerator.class);
        // With source maps, as tinyscript --node runs: every differential test checks that the
        // markers come out of the code again
        generator.setSourceMaps(true);
        resourceSet = injector.getInstance(XtextResourceSet.class);
    }

    /**
     * The resource set that {@link #parse(String)} and {@link #assertRejected(String)} use.
     */
    protected XtextResourceSet resourceSet() {
        return resourceSet;
    }

    /**
     * Skips any test that requires node.js if {@code node} is not on the PATH.
     * Subclasses that want auto-skip behaviour should call this from a
     * {@code @BeforeEach} method.
     */
    protected void assumeNodeAvailable() {
        // In CI (GitHub sets CI=true), node must be there: the differential tests fail instead
        if (System.getenv("CI") != null && !NodeJsRunner.isAvailable())
            throw new AssertionError("node.js is needed in CI, but not found on PATH");
        Assumptions.assumeTrue(NodeJsRunner.isAvailable(), "Skipping: node.js not found on PATH");
    }

    // -------------------------------------------------------------------------
    // Parse
    // -------------------------------------------------------------------------

    /**
     * Parses a Tinyscript source string and returns the AST root.
     * Fails the test on parse errors.
     */
    protected Tinyscript parse(String tsCode) {
        URI uri = freshUri();
        Resource resource = resourceSet.createResource(uri);
        try {
            resource.load(new StringInputStream(tsCode), null);
        } catch (IOException e) {
            throw new WrappedException(e);
        }
        List<Issue> issues = errorsOf(getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl));
        if (!issues.isEmpty()) {
            fail("Parse/validation error in Tinyscript source: " + issues.get(0).getMessage()
                    + "\n--- source ---\n" + tsCode);
        }
        return (Tinyscript) resource.getContents().get(0);
    }

    /**
     * Loads a Tinyscript file from the test classpath.
     */
    protected Tinyscript parseFile(String resourcePath) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            fail("Test resource not found: " + resourcePath);
        }
        URI uri = URI.createURI(resourcePath);
        Resource resource = resourceSet.createResource(uri);
        try {
            resource.load(in, null);
        } catch (IOException e) {
            throw new WrappedException(e);
        }
        List<Issue> issues = errorsOf(getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl));
        if (!issues.isEmpty()) {
            fail("Parse/validation error in " + resourcePath + ": " + issues.get(0).getMessage());
        }
        return (Tinyscript) resource.getContents().get(0);
    }

    // -------------------------------------------------------------------------
    // Transpile
    // -------------------------------------------------------------------------

    /**
     * Runs the Tinyscript generator on the given AST and returns the generated
     * JavaScript source (including builtins).
     */
    protected String transpile(Tinyscript ast) {
        return generator.generate(ast, true).toString();
    }

    // -------------------------------------------------------------------------
    // Interpreter output
    // -------------------------------------------------------------------------

    /**
     * Runs {@code tsCode} through the Tinyscript interpreter with a fresh engine,
     * captures everything written to {@code print()}, and returns it as a string.
     *
     * <p>Fails the test if the interpreter throws any exception.
     */
    public String interpreterOutput(String tsCode) {
        return interpreterOutput(tsCode, null);
    }

    /**
     * Like {@link #interpreterOutput(String)}, but runs the engine with a
     * {@link ResourceMonitor} enforcing {@code limits} (as the web demo does).
     * {@code limits == null} runs without resource monitoring.
     */
    public String interpreterOutput(String tsCode, ResourceLimits limits) {
        return interpreterOutput(tsCode, limits, true);
    }

    /**
     * Runs {@code tsCode} through a fresh, sandboxed interpreter that writes to
     * {@code stdout}, e.g. to test what happens when the output fails.
     */
    protected void runInterpreter(String tsCode, OutputStream stdout) {
        runInterpreter(tsCode, null, true, stdout);
    }


    private String interpreterOutput(String tsCode, ResourceLimits limits, boolean sandboxed) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        runInterpreter(tsCode, limits, sandboxed, stdout);
        return stdout.toString();
    }

    private void runInterpreter(String tsCode, ResourceLimits limits, boolean sandboxed, OutputStream stdout) {
        // Fresh injector + engine per call so tests are independent.
        Injector localInjector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
        XtextResourceSet localResourceSet = localInjector.getInstance(XtextResourceSet.class);

        URI uri = freshUri(localResourceSet);
        Resource resource = localResourceSet.createResource(uri);
        try {
            resource.load(new StringInputStream(tsCode), null);
        } catch (IOException e) {
            throw new WrappedException(e);
        }
        List<Issue> issues = errorsOf(getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl));
        if (!issues.isEmpty()) {
            fail("Parse/validation error: " + issues.get(0).getMessage()
                    + "\n--- source ---\n" + tsCode);
        }
        Tinyscript ast = (Tinyscript) resource.getContents().get(0);

        TinyscriptEngine engine;
        if (limits == null) {
            engine = new TinyscriptEngine(localResourceSet);
        } else {
            ResourceMonitor monitor = new ResourceMonitor();
            monitor.configureLimits(limits);
            engine = new TinyscriptEngine(localResourceSet, monitor);
        }
        engine.setSandboxed(sandboxed);
        engine.defineStdOut(stdout);
        engine.execute(ast);
    }

    /**
     * Runs the file {@code main} like the REPL does (not sandboxed), with a file system
     * module loader that searches {@code modulePath}; returns the captured stdout.
     * Validation errors of {@code main} are thrown as {@link TinyscriptSyntaxError}.
     */
    protected String runModule(Path main, Path... modulePath) {
        return runModule(main, false, List.of(), "", modulePath);
    }

    /**
     * Like {@link #runModule(Path, Path...)}, with {@code input} as the standard input (std:io).
     */
    protected String runModuleWithInput(Path main, String input) {
        return runModule(main, false, List.of(), input);
    }

    /**
     * Like {@link #runModule(Path, Path...)}, but sandboxed (as in the webdemo).
     */
    protected String runModuleSandboxed(Path main, Path... modulePath) {
        return runModule(main, true, List.of(), "", modulePath);
    }

    private String runModule(Path main, boolean sandboxed, List<String> args, String input, Path... modulePath) {
        return runModule(main, sandboxed, args,
                engine -> engine.defineStdIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))), modulePath);
    }

    /**
     * Like {@link #runModule(Path, Path...)}; {@code setUp} prepares the engine (e.g. its
     * standard input) before it runs {@code main}.
     */
    protected String runModuleWithEngine(Path main, Consumer<TinyscriptEngine> setUp) {
        return runModule(main, false, List.of(), setUp);
    }

    private String runModule(Path main, boolean sandboxed, List<String> args, Consumer<TinyscriptEngine> setUp, Path... modulePath) {
        Injector localInjector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
        XtextResourceSet localResourceSet = localInjector.getInstance(XtextResourceSet.class);
        Resource resource = localResourceSet.getResource(URI.createFileURI(main.toAbsolutePath().toString()), true);
        List<Issue> issues = errorsOf(getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl));
        TinyscriptSyntaxError error = TinyscriptDiagnostics.firstError(issues, main.toString());
        if (error != null)
            throw error;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        TinyscriptEngine engine = new TinyscriptEngine(localResourceSet);
        engine.setSandboxed(sandboxed);
        engine.setModuleLoader(new FileSystemModuleLoader(List.of(modulePath)));
        engine.setScriptArguments(args);
        engine.defineStdOut(stdout);
        setUp.accept(engine);
        engine.execute((Tinyscript) resource.getContents().get(0));
        return stdout.toString();
    }

    /**
     * Integration test for a complete program: run with the script arguments {@code args}
     * (System.args), the interpreter and node, running the bundled Javascript, must both
     * print {@code expected}. Imports are relative to {@code main}.
     */
    protected void assertProgramOutput(String expected, Path main, String... args) {
        assertEquals(expected, runModule(main, false, List.of(args), ""), "Interpreter output of " + main);
        assumeNodeAvailable();
        String jsCode = transpileModule(main);
        String nodeOut;
        try {
            nodeOut = NodeJsRunner.run(jsCode, args);
        } catch (AssertionError e) {
            fail("node.js failed for " + main + "\n--- Generated JS ---\n" + jsCode + "\n--- node error ---\n" + e.getMessage());
            return;
        }
        assertEquals(expected, nodeOut, "node output of " + main + "\n--- Generated JS ---\n" + jsCode);
    }

    /**
     * Transpiles the file {@code main} with its modules into one Javascript program.
     */
    protected String transpileModule(Path main, Path... modulePath) {
        Injector localInjector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
        XtextResourceSet localResourceSet = localInjector.getInstance(XtextResourceSet.class);
        Resource resource = localResourceSet.getResource(URI.createFileURI(main.toAbsolutePath().toString()), true);
        return generator.generate((Tinyscript) resource.getContents().get(0), true,
                new FileSystemModuleLoader(List.of(modulePath))).toString();
    }

    /**
     * Differential test for a program with modules: the interpreter ({@link #runModule})
     * and node, running the bundled Javascript, must print the same.
     */
    protected void assertSameOutputModule(Path main, Path... modulePath) {
        assertSameOutputModuleWithInput(main, "", modulePath);
    }

    /**
     * Like {@link #assertSameOutputModule(Path, Path...)}, with {@code input} as the standard
     * input of both.
     */
    protected void assertSameOutputModuleWithInput(Path main, String input, Path... modulePath) {
        assumeNodeAvailable();
        String interpreterOut = runModule(main, false, List.of(), input, modulePath);
        String jsCode = transpileModule(main, modulePath);
        String nodeOut;
        try {
            nodeOut = NodeJsRunner.runWithInput(jsCode, input);
        } catch (AssertionError e) {
            fail("node.js failed for " + main + "\n--- Generated JS ---\n" + jsCode + "\n--- node error ---\n" + e.getMessage());
            return;
        }
        if (!interpreterOut.equals(nodeOut)) {
            fail("Interpreter and node.js produced different output for " + main
                    + "\n--- Generated JS ---\n" + jsCode
                    + "\n--- Interpreter stdout ---\n" + interpreterOut
                    + "\n--- node stdout ---\n" + nodeOut);
        }
    }

    /**
     * The errors reported for {@code tsCode} (see {@link TinyscriptDiagnostics#relevantErrors}),
     * as "line:column: message".
     */
    protected List<String> reportedErrors(String tsCode) {
        URI uri = freshUri();
        Resource resource = resourceSet.createResource(uri);
        try {
            resource.load(new StringInputStream(tsCode), null);
        } catch (IOException e) {
            throw new WrappedException(e);
        }
        List<Issue> issues = errorsOf(getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl));
        return TinyscriptDiagnostics.relevantErrors(issues).stream()
                .map(issue -> issue.getLineNumber() + ":" + issue.getColumn() + ": " + issue.getMessage())
                .toList();
    }

    /**
     * Asserts that {@code tsCode} is rejected before execution, i.e. the parser
     * or the validator reports at least one error.
     */
    // Only the errors: a warning (a name that hides a builtin) does not stop a script
    private static List<Issue> errorsOf(List<Issue> issues) {
        return issues.stream().filter(issue -> issue.getSeverity() == org.eclipse.xtext.diagnostics.Severity.ERROR).toList();
    }

    protected List<String> reportedWarnings(String tsCode) {
        URI uri = freshUri();
        Resource resource = resourceSet.createResource(uri);
        try {
            resource.load(new StringInputStream(tsCode), null);
        } catch (IOException e) {
            throw new WrappedException(e);
        }
        List<Issue> issues = getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
        return issues.stream()
                .filter(issue -> issue.getSeverity() == org.eclipse.xtext.diagnostics.Severity.WARNING)
                .map(issue -> issue.getLineNumber() + ":" + issue.getColumn() + ": " + issue.getMessage())
                .toList();
    }

    protected void assertRejected(String tsCode) {
        URI uri = freshUri();
        Resource resource = resourceSet.createResource(uri);
        try {
            resource.load(new StringInputStream(tsCode), null);
        } catch (IOException e) {
            throw new WrappedException(e);
        }
        List<Issue> issues = errorsOf(getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl));
        if (issues.isEmpty()) {
            fail("Expected a parse or validation error, but none was reported"
                    + "\n--- source ---\n" + tsCode);
        }
    }

    // -------------------------------------------------------------------------
    // Level 1 assertion — differential stdout comparison
    // -------------------------------------------------------------------------

    /**
     * Level 1 differential test: runs {@code tsCode} through both the interpreter
     * and node.js, and asserts that their stdout is identical.
     *
     * <p>On failure the test message includes the Tinyscript source, the generated
     * JavaScript, and both stdouts so the problem is immediately reproducible.
     */
    protected void assertSameOutput(String tsCode) {
        assumeNodeAvailable();

        String jsCode = transpile(parse(tsCode));
        String interpreterOut = interpreterOutput(tsCode);
        String nodeOut;
        try {
            nodeOut = NodeJsRunner.run(jsCode);
        } catch (AssertionError e) {
            fail("node.js failed for source:\n--- Tinyscript ---\n" + tsCode
                    + "\n--- Generated JS ---\n" + jsCode
                    + "\n--- node error ---\n" + e.getMessage());
            return; // unreachable
        }

        if (!interpreterOut.equals(nodeOut)) {
            fail("Interpreter and node.js produced different output."
                    + "\n--- Tinyscript source ---\n" + tsCode
                    + "\n--- Generated JS ---\n" + jsCode
                    + "\n--- Interpreter stdout ---\n" + interpreterOut
                    + "\n--- node stdout ---\n" + nodeOut);
        }
    }

    // -------------------------------------------------------------------------
    // Level 2 assertions — program-level integration
    // -------------------------------------------------------------------------

    /**
     * Level 2 integration test from inline source: runs {@code tsCode} through
     * the interpreter (must not throw), then transpiles and runs through node
     * (must exit 0).
     *
     * <p>The generated JS is shown on failure.
     */
    protected void assertTranspilerCorrect(String tsCode) {
        assumeNodeAvailable();

        Tinyscript ast = parse(tsCode);

        // Interpreter must not throw
        try {
            interpreterOutput(tsCode);
        } catch (Exception e) {
            fail("Interpreter threw an exception:\n" + e.getMessage()
                    + "\n--- source ---\n" + tsCode);
        }

        // node must exit 0
        String jsCode = transpile(ast);
        try {
            NodeJsRunner.run(jsCode);
        } catch (AssertionError e) {
            fail("node.js failed for source:\n--- Tinyscript ---\n" + tsCode
                    + "\n--- Generated JS ---\n" + jsCode
                    + "\n" + e.getMessage());
        }
    }

    /**
     * Level 2 integration test from a classpath resource: loads {@code filename}
     * (relative to the test classpath root), runs through the interpreter (must not
     * throw), then transpiles and runs through node (must exit 0).
     */
    protected void assertTranspilerCorrectFromFile(String filename) {
        assumeNodeAvailable();

        Tinyscript ast = parseFile(filename);

        // Interpreter must not throw
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        TinyscriptEngine engine = new TinyscriptEngine(resourceSet);
        engine.defineStdOut(stdout);
        try {
            engine.execute(ast);
        } catch (Exception e) {
            fail("Interpreter threw an exception for " + filename + ":\n" + e.getMessage());
        }

        // node must exit 0
        String jsCode = transpile(ast);
        try {
            NodeJsRunner.run(jsCode);
        } catch (AssertionError e) {
            fail("node.js failed for " + filename
                    + "\n--- Generated JS ---\n" + jsCode
                    + "\n" + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Exception assertion helper
    // -------------------------------------------------------------------------

    /**
     * Asserts that running {@code tsCode} through the interpreter throws an
     * exception of the given type (or a subtype).
     */
    protected void assertInterpreterThrows(Class<? extends Throwable> expectedType, String tsCode) {
        try {
            interpreterOutput(tsCode);
        } catch (Throwable t) {
            if (!expectedType.isInstance(t)) {
                fail("Expected " + expectedType.getSimpleName()
                        + " but got " + t.getClass().getSimpleName() + ": " + t.getMessage()
                        + "\n--- source ---\n" + tsCode);
            }
            return;
        }
        fail("Expected " + expectedType.getSimpleName() + " but no exception was thrown"
                + "\n--- source ---\n" + tsCode);
    }

    // -------------------------------------------------------------------------
    // Failures: the same behavior in both backends
    // -------------------------------------------------------------------------

    /**
     * How a run failed: the output up to the error, the class (as Javascript names it) and
     * message of the error, and whether catch gets it.
     */
    record Failure(String output, String name, String message, boolean catchable) {
    }

    /**
     * Asserts that both backends fail in the same way (see TODO.md, what "equal" means): at the
     * same point (the same output up to the error) and in the same category for catch. Class and
     * message are only reported, in target/backend-differences.txt.
     */
    protected void assertSameFailure(String tsCode) {
        assertSameFailure(TinyscriptRuntimeException.class, tsCode);
    }

    /**
     * Like {@link #assertSameFailure(String)}; the interpreter must throw {@code expectedType}.
     */
    protected void assertSameFailure(Class<? extends TinyscriptRuntimeException> expectedType, String tsCode) {
        Failure interpreter = interpreterFailure(expectedType, tsCode);
        assumeNodeAvailable();
        String jsCode = transpile(parse(tsCode));
        NodeJsRunner.Result result = NodeJsRunner.execute(withFailureHandler(jsCode), "");
        if (result.exitCode() == 0)
            fail("The interpreter fails, but node goes on (if that is known, use assertKnownGap)"
                    + "\n--- Tinyscript ---\n" + tsCode
                    + "\n--- interpreter ---\n" + interpreter
                    + "\n--- node stdout ---\n" + result.stdout());
        Failure node = nodeFailure(result);
        if (node == null)
            fail("node failed without the failure line (an error in the generated code?)"
                    + "\n--- Tinyscript ---\n" + tsCode + "\n--- Generated JS ---\n" + jsCode
                    + "\n--- node stderr ---\n" + result.stderr());
        assertEquals(interpreter.output(), node.output(), "the output up to the error\n--- Tinyscript ---\n" + tsCode);
        assertEquals(interpreter.catchable(), node.catchable(), "catch gets the error in one backend, not in the other"
                + "\n--- Tinyscript ---\n" + tsCode + "\n--- interpreter ---\n" + interpreter + "\n--- node ---\n" + node);
        BackendDifferences.note(tsCode, interpreter, node);
    }

    /**
     * A known gap: the interpreter fails, the generated Javascript goes on. When node fails as
     * well (the gap is closed), this fails: use {@link #assertSameFailure(String)} instead.
     */
    protected void assertKnownGap(String tsCode) {
        assertKnownGap(TinyscriptRuntimeException.class, tsCode);
    }

    /**
     * Like {@link #assertKnownGap(String)}; the interpreter must throw {@code expectedType}.
     */
    protected void assertKnownGap(Class<? extends TinyscriptRuntimeException> expectedType, String tsCode) {
        interpreterFailure(expectedType, tsCode);
        assumeNodeAvailable();
        NodeJsRunner.Result result = NodeJsRunner.execute(transpile(parse(tsCode)), "");
        if (result.exitCode() != 0)
            fail("node fails now as well, the gap is closed: use assertSameFailure"
                    + "\n--- Tinyscript ---\n" + tsCode + "\n--- node stderr ---\n" + result.stderr());
    }

    /** How the interpreter fails for {@code tsCode}; fails the test if it does not. */
    protected Failure interpreterFailure(String tsCode) {
        return interpreterFailure(TinyscriptRuntimeException.class, tsCode);
    }

    private Failure interpreterFailure(Class<? extends TinyscriptRuntimeException> expectedType, String tsCode) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        try {
            runInterpreter(tsCode, null, true, stdout);
        }
        catch (TinyscriptRuntimeException e) {
            if (!expectedType.isInstance(e))
                fail("Expected " + expectedType.getSimpleName() + " but got " + e.getClass().getSimpleName() + ": "
                        + e.getMessage() + "\n--- Tinyscript ---\n" + tsCode);
            boolean catchable = e instanceof TinyscriptError || e instanceof TinyscriptIOError || e instanceof TinyscriptJsonError;
            return new Failure(stdout.toString(StandardCharsets.UTF_8), javascriptName(e), e.getMessage(), catchable);
        }
        fail("The interpreter does not fail\n--- Tinyscript ---\n" + tsCode + "\n--- output ---\n" + stdout);
        return null;
    }

    // The class of an interpreter error as Javascript names it (TinyscriptTypeError: TypeError)
    private static String javascriptName(TinyscriptRuntimeException e) {
        return switch (e) {
            case TinyscriptJsonError json -> "SyntaxError";
            case TinyscriptStackOverflowError overflow -> "RangeError";
            case TinyscriptArgumentError argument -> "TypeError";
            case TinyscriptAssertationError assertion -> "AssertionError";
            default -> e.getClass().getSimpleName().replaceFirst("^Tinyscript", "");
        };
    }

    private static final String FAILURE_MARKER = "__ts_failure\t";

    /**
     * Adds a handler for an uncaught error to a generated program: it asks the prelude whether
     * catch would get the error (__ts_catchable) and writes a line with the result to stderr.
     * The program then ends with exit code 70 (not with process.exit, which may cut the output).
     */
    static String withFailureHandler(String jsCode) {
        String strict = "\"use strict\";\n";
        if (!jsCode.startsWith(strict))
            throw new IllegalArgumentException("not a generated program");
        return strict
                + "process.on(\"uncaughtException\", e => {\n"
                + "    let caught = true;\n"
                + "    try { __ts_catchable(e); } catch (_) { caught = false; }\n"
                + "    const text = x => String(x).split(\"\\\\\").join(\"\\\\\\\\\").split(\"\\n\").join(\"\\\\n\").split(\"\\t\").join(\"\\\\t\");\n"
                + "    process.stderr.write(\"" + FAILURE_MARKER.replace("\t", "\\t") + "\" + caught + \"\\t\" + text(e?.name) + \"\\t\" + text(e?.message) + \"\\n\");\n"
                + "    process.exitCode = 70;\n"
                + "});\n"
                + jsCode.substring(strict.length());
    }

    // The failure the handler of withFailureHandler reported, or null
    static Failure nodeFailure(NodeJsRunner.Result result) {
        for (String line : result.stderr().split("\n")) {
            if (line.startsWith(FAILURE_MARKER)) {
                String[] parts = line.substring(FAILURE_MARKER.length()).split("\t", 3);
                return new Failure(result.stdout(), unescape(parts[1]), unescape(parts[2]), Boolean.parseBoolean(parts[0]));
            }
        }
        return null;
    }

    private static String unescape(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(++i);
                result.append(next == 'n' ? '\n' : next == 't' ? '\t' : next);
            }
            else
                result.append(c);
        }
        return result.toString();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private int uriCounter = 0;

    private URI freshUri() {
        return freshUri(resourceSet);
    }

    private URI freshUri(XtextResourceSet rs) {
        String name = "__gentest";
        for (int i = uriCounter; i < Integer.MAX_VALUE; i++) {
            URI uri = URI.createURI(name + i + "." + FILE_EXTENSION);
            if (rs.getResource(uri, false) == null) {
                uriCounter = i + 1;
                return uri;
            }
        }
        throw new IllegalStateException("Could not allocate a fresh URI");
    }

    private IResourceValidator getValidator(Resource resource) {
        return ((XtextResource) resource).getResourceServiceProvider().getResourceValidator();
    }
}
