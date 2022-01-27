package de.mkbauer.tinyscript.runtime;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import org.eclipse.xtext.testing.XtextRunner;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;


@RunWith(XtextRunner.class)
public class BuiltinConstructorsTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testStringConstructor() {
		executeScriptFromString("var s = new String(\"Hello\"); assert (\"Hello\" == s);");
		TSValue value = executeScriptFromString("var s = new String(\"Hello\"); s;");
		assertTrue("constructor String creates instances of StringObjects", 
				value.isObject() && (value.isString()));
	}
	
	@Test
	public void testStringConstructorWithoutNew() {
		executeScriptFromString("var s = String(\"Hello\"); assert (\"Hello\" == s);");
		executeScriptFromString("var s = String(4); assert (\"4\" == s);");
	}

}
