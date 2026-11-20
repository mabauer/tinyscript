package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

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
import de.mkbauer.tinyscript.TinyscriptSyntaxError;
import de.mkbauer.tinyscript.generator.TinyscriptGenerator;
import de.mkbauer.tinyscript.interpreter.FileSystemModuleLoader;
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
        List<Issue> issues = getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
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
        List<Issue> issues = getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
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

    /**
     * Like {@link #interpreterOutput(String)}, but with sandboxing switched off
     * (as in the REPL), e.g. to test the file system builtins.
     */
    public String interpreterOutputUnsandboxed(String tsCode) {
        return interpreterOutput(tsCode, null, false);
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
        List<Issue> issues = getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
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
        return runModule(main, false, modulePath);
    }

    /**
     * Like {@link #runModule(Path, Path...)}, but sandboxed (as in the webdemo).
     */
    protected String runModuleSandboxed(Path main, Path... modulePath) {
        return runModule(main, true, modulePath);
    }

    private String runModule(Path main, boolean sandboxed, Path... modulePath) {
        Injector localInjector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
        XtextResourceSet localResourceSet = localInjector.getInstance(XtextResourceSet.class);
        Resource resource = localResourceSet.getResource(URI.createFileURI(main.toAbsolutePath().toString()), true);
        List<Issue> issues = getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
        if (!issues.isEmpty())
            throw new TinyscriptSyntaxError(issues.get(0).getMessage(), main.toString(), issues.get(0).getLineNumber());
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        TinyscriptEngine engine = new TinyscriptEngine(localResourceSet);
        engine.setSandboxed(sandboxed);
        engine.setModuleLoader(new FileSystemModuleLoader(List.of(modulePath)));
        engine.defineStdOut(stdout);
        engine.execute((Tinyscript) resource.getContents().get(0));
        return stdout.toString();
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
        assumeNodeAvailable();
        String interpreterOut = runModule(main, modulePath);
        String jsCode = transpileModule(main, modulePath);
        String nodeOut;
        try {
            nodeOut = NodeJsRunner.run(jsCode);
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
     * Asserts that {@code tsCode} is rejected before execution, i.e. the parser
     * or the validator reports at least one error.
     */
    protected void assertRejected(String tsCode) {
        URI uri = freshUri();
        Resource resource = resourceSet.createResource(uri);
        try {
            resource.load(new StringInputStream(tsCode), null);
        } catch (IOException e) {
            throw new WrappedException(e);
        }
        List<Issue> issues = getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
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
