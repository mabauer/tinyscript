package de.mkbauer.tinyscript.repl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.StringInputStream;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.scoping.SharedGlobals;
import de.mkbauer.tinyscript.ts.Tinyscript;

/**
 * Editing in the interactive session: open statements, the word to complete, completion and
 * highlighting.
 */
public class TinyscriptEditingTest {

    private static TinyscriptEngine engine;

    private static TinyscriptCompleter completer;

    @BeforeAll
    public static void setUpSession() throws Exception {
        XtextResourceSet resourceSet = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration()
                .getInstance(XtextResourceSet.class);
        engine = new TinyscriptEngine(resourceSet);
        engine.setSandboxed(false);
        Resource resource = resourceSet.createResource(URI.createURI("__repl1.tys"));
        SharedGlobals.share(resource);
        resource.load(new StringInputStream("const point = { x: 1, y: 2, move() { } };\nconst word = \"abc\";\nconst n = 5;\nlet maximum = 3;"), null);
        engine.execute((Tinyscript) resource.getContents().get(0));
        completer = new TinyscriptCompleter(() -> engine);
    }

    @Test
    public void testOpenStatements() {
        assertTrue(TinyscriptSyntax.isOpen("if (x) {"));
        assertTrue(TinyscriptSyntax.isOpen("const o = {\n    a: [1,"));
        assertTrue(TinyscriptSyntax.isOpen("print(1); /* a comment"));
        assertFalse(TinyscriptSyntax.isOpen("if (x) { print(\"{\"); }"));
        assertFalse(TinyscriptSyntax.isOpen("print(\"(\"); // ("));
        assertFalse(TinyscriptSyntax.isOpen("let x = 1 +"));
        assertFalse(TinyscriptSyntax.isOpen("print(\")\\\"(\");"));
        // An unclosed string ends at the end of its line; the parser reports it
        assertFalse(TinyscriptSyntax.isOpen("let s = \"(abc"));
    }

    @Test
    public void testTheWordToComplete() {
        assertEquals("Math.ma", TinyscriptLineParser.wordBefore("print(Math.ma"));
        assertEquals("point.", TinyscriptLineParser.wordBefore("let a = point."));
        assertEquals("pri", TinyscriptLineParser.wordBefore("pri"));
        assertEquals("!lo", TinyscriptLineParser.wordBefore("!lo"));
        assertEquals("", TinyscriptLineParser.wordBefore("x = 1.5"));
        assertEquals("", TinyscriptLineParser.wordBefore("print(\"Math.ma"));
        assertEquals("", TinyscriptLineParser.wordBefore("x; // Math.ma"));
        assertEquals("", TinyscriptLineParser.wordBefore("\"abc\".len"));
        assertEquals("", TinyscriptLineParser.wordBefore("f().x"));
        assertEquals("y", TinyscriptLineParser.wordBefore("print(\"a\", y"));
    }

    @Test
    public void testCompletesNamesKeywordsAndBuiltins() {
        assertEquals(List.of("maximum"), completer.completions("maxi"));
        assertTrue(completer.completions("pr").contains("print"), completer.completions("pr").toString());
        assertTrue(completer.completions("con").contains("const"), completer.completions("con").toString());
        assertTrue(completer.completions("con").contains("continue"), completer.completions("con").toString());
        assertTrue(completer.completions("").contains("point"));
        assertFalse(completer.completions("").stream().anyMatch(name -> name.startsWith("__")));
    }

    @Test
    public void testCompletesPropertiesAfterADot() {
        assertTrue(completer.completions("Math.ma").containsAll(List.of("Math.max")), completer.completions("Math.ma").toString());
        assertEquals(List.of("point.x", "point.y", "point.move"), completer.completions("point.").subList(0, 3));
        // With the methods of the prototypes
        assertTrue(completer.completions("point.").contains("point.hasOwnProperty"), completer.completions("point.").toString());
        assertTrue(completer.completions("word.st").contains("word.startsWith"), completer.completions("word.st").toString());
        assertTrue(completer.completions("word.").contains("word.length"), completer.completions("word.").toString());
        assertEquals(List.of("n.toFixed", "n.toString"), completer.completions("n.to"));
        assertEquals(List.of(), completer.completions("nothing.x"));
        assertEquals(List.of(), completer.completions("point.x.y"));
    }

    @Test
    public void testCompletesCommands() {
        assertEquals(List.of("!load"), completer.completions("!lo"));
        assertEquals(6, completer.completions("!").size());
    }

    @Test
    public void testHighlighting() {
        AttributedString line = TinyscriptHighlighter.highlight("let s = \"if\" + 12; // for\no.from = true;");
        assertEquals("let s = \"if\" + 12; // for\no.from = true;", line.toString());
        assertEquals(TinyscriptHighlighter.KEYWORD, line.styleAt(0));
        assertEquals(TinyscriptHighlighter.STRING, line.styleAt(9));
        assertEquals(TinyscriptHighlighter.NUMBER, line.styleAt(15));
        assertEquals(TinyscriptHighlighter.COMMENT, line.styleAt(22));
        // A property named like a keyword is no keyword
        assertFalse(TinyscriptHighlighter.KEYWORD.equals(line.styleAt(28)));
        assertEquals(TinyscriptHighlighter.ATOM, line.styleAt(35));
        assertEquals(TinyscriptHighlighter.COMMAND, TinyscriptHighlighter.highlight("!load x.tys").styleAt(1));
    }
}
