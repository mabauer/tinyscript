package de.mkbauer.tinyscript.interpreter;

import static org.junit.Assert.assertEquals;

import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@RunWith(XtextRunner.class)
public class TypeOfExpressionTest extends TinyscriptInterpreterTestHelper {

	@Test
	public void testUndeclared() {
		TSValue value = executeScriptFromString("typeof nothing;");
		assertEquals("undefined", value.asString());
	}
	
	@Test
	public void testUndefined() {
		TSValue value = executeScriptFromString("var undef; typeof undef;");
		assertEquals("undefined", value.asString());
	}
	
	@Test
	public void testFunction() {
		TSValue value = executeScriptFromString("typeof Object;");
		assertEquals("function", value.asString());
	}
	
	@Test
	public void testObject() {
		TSValue value = executeScriptFromString("var obj = {}; typeof obj;");
		assertEquals("object", value.asString());
	}
	
	@Test
	public void testUndefinedProperty() {
		TSValue value = executeScriptFromString("typeof Object.XXX;");
		assertEquals("undefined", value.asString());
	}
	
	@Test
	public void testFunctionAsProperty() {
		TSValue value = executeScriptFromString("typeof Object.toString;");
		assertEquals("function", value.asString());
	}	
	
}
