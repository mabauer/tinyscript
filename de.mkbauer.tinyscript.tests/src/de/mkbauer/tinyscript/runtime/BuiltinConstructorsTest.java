package de.mkbauer.tinyscript.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;


@ExtendWith(InjectionExtension.class)
public class BuiltinConstructorsTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testStringConstructor() {
		executeScriptFromString("let s = new String(\"Hello\"); assert (\"Hello\" == s);");
		TSValue value = executeScriptFromString("let s = new String(\"Hello\"); s;");
		assertTrue(value.isObject() && (value.isString()),
				"constructor String creates instances of StringObjects");
	}
	
	@Test
	public void testStringConstructorWithoutNew() {
		executeScriptFromString("let s = String(\"Hello\"); assert (\"Hello\" == s);");
		executeScriptFromString("let s = String(4); assert (\"4\" == s);");
	}

}
