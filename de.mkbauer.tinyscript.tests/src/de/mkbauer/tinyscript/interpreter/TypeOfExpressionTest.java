package de.mkbauer.tinyscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@ExtendWith(InjectionExtension.class)
public class TypeOfExpressionTest extends TinyscriptInterpreterTestHelper {

	@Test
	public void testUndefined() {
		TSValue value = executeScriptFromString("let undef; typeof undef;");
		assertEquals("undefined", value.asString());
	}
	
	@Test
	public void testFunction() {
		TSValue value = executeScriptFromString("typeof Object;");
		assertEquals("function", value.asString());
	}
	
	@Test
	public void testObject() {
		TSValue value = executeScriptFromString("let obj = {}; typeof obj;");
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
