package de.mkbauer.tinyscript.standalone.tests;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.util.StringInputStream;
import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.scoping.SharedGlobals;

/**
 * Which top-level names a script sees: its own, the builtins and those of shared
 * resources (the lines of an interactive REPL session), but never those of other files.
 */
public class ScopingTest extends TinyscriptTranspilerTestBase {

    @Test
    public void testGlobalsOfOtherFilesAreNotVisible() {
        parse("var shared = 1; function helper() { return 2; }");
        assertRejected("print(shared);");
        assertRejected("print(helper());");
    }

    @Test
    public void testBuiltinsAreVisible() {
        parse("print(Math.PI); print(Object.keys({}));");
    }

    @Test
    public void testGlobalsOfSharedResourcesAreVisible() throws IOException {
        Resource line = resourceSet().createResource(URI.createURI("__session0.tys"));
        line.load(new StringInputStream("var shared = 1;"), null);
        SharedGlobals.share(line);
        parse("print(shared);");
    }
}
