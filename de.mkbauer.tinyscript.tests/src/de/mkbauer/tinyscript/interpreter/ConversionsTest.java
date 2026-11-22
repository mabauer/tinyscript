package de.mkbauer.tinyscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@ExtendWith(InjectionExtension.class)
public class ConversionsTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testPrimitivesToString() {
		TSValue primValue = executeScriptFromString("let str = \"String\"; str;");
		assertEquals("String", TSObject.toString(getExecutionVisitor(), primValue));
		primValue = executeScriptFromString("let i = 8; i;");
		assertEquals("8", TSObject.toString(getExecutionVisitor(), primValue));
		primValue = executeScriptFromString("let x = 2.5; x;");
		assertEquals("2.5", TSObject.toString(getExecutionVisitor(), primValue));
		primValue = executeScriptFromString("let b = false; b;");
		assertEquals("false", TSObject.toString(getExecutionVisitor(), primValue));
		primValue = executeScriptFromString("let b = true; b;");
		assertEquals("true", TSObject.toString(getExecutionVisitor(), primValue));
	}
	
	@Test
	public void testSpecialsToString() {
		TSValue undefined = executeScriptFromString("let obj; obj;");
		assertEquals("[object Undefined]", TSObject.toString(getExecutionVisitor(), undefined));
		TSValue nullObject = executeScriptFromString("let obj = null; obj;");
		assertEquals("[object Null]", TSObject.toString(getExecutionVisitor(), nullObject));
	}
	
	@Test
	public void testSimpleObjectToString() {
		// The same text as print (Javascript: "[object Object]")
		TSValue obj = executeScriptFromString("let obj = {}; obj;");
		assertEquals("{}", TSObject.toString(getExecutionVisitor(), obj));
		obj = executeScriptFromString("let obj = { name: \"Hugo\" }; obj;");
		assertEquals("{name: \"Hugo\"}", TSObject.toString(getExecutionVisitor(), obj));
	}
	
	@Test
	public void testArrayToString() {
		TSValue arr = executeScriptFromString("let arr = []; arr;");
		assertEquals("[]", TSObject.toString(getExecutionVisitor(), arr));
		arr = executeScriptFromString("let arr = [\"Hallo\", \"Hugo\"]; arr;");
		assertEquals("[\"Hallo\", \"Hugo\"]", TSObject.toString(getExecutionVisitor(), arr));
	}
	
	@Test
	public void testFunctionToString() {
		TSValue f = executeScriptFromString("function f(x) {return x; }; f;");
		assertTrue(TSObject.toString(getExecutionVisitor(), f).startsWith("function f(x) {"));
		f = executeScriptFromString("print;");
		assertTrue(TSObject.toString(getExecutionVisitor(), f).startsWith("function print("));
	}
	

}

