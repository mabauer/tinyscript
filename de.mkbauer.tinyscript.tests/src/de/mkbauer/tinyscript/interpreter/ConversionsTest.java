package de.mkbauer.tinyscript.interpreter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@RunWith(XtextRunner.class)
public class ConversionsTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testPrimitivesToString() {
		TSValue primValue = executeScriptFromString("var str = \"String\"; str;");
		assertEquals("String", TSObject.toString(getExecutionVisitor(), primValue));
		primValue = executeScriptFromString("var i = 8; i;");
		assertEquals("8", TSObject.toString(getExecutionVisitor(), primValue));
		primValue = executeScriptFromString("var x = 2.5; x;");
		assertEquals("2.5", TSObject.toString(getExecutionVisitor(), primValue));
		primValue = executeScriptFromString("var b = false; b;");
		assertEquals("false", TSObject.toString(getExecutionVisitor(), primValue));
		primValue = executeScriptFromString("var b = true; b;");
		assertEquals("true", TSObject.toString(getExecutionVisitor(), primValue));
	}
	
	@Test
	public void testSpecialsToString() {
		TSValue undefined = executeScriptFromString("var obj; obj;");
		assertEquals("[object Undefined]", TSObject.toString(getExecutionVisitor(), undefined));
		TSValue nullObject = executeScriptFromString("var obj = null; obj;");
		assertEquals("[object Null]", TSObject.toString(getExecutionVisitor(), nullObject));
	}
	
	@Test
	public void testSimpleObjectToString() {
		TSValue obj = executeScriptFromString("var obj = {}; obj;");
		assertEquals("[object Object]", TSObject.toString(getExecutionVisitor(), obj));
		obj = executeScriptFromString("var obj = { name: \"Hugo\" }; obj;");
		assertEquals("[object Object]", TSObject.toString(getExecutionVisitor(), obj));
	}
	
	@Test
	public void testArrayToString() {
		TSValue arr = executeScriptFromString("var arr = []; arr;");
		assertEquals("", TSObject.toString(getExecutionVisitor(), arr));
		arr = executeScriptFromString("var arr = [\"Hallo\", \"Hugo\"]; arr;");
		assertEquals("Hallo, Hugo", TSObject.toString(getExecutionVisitor(), arr));
	}
	
	@Test
	public void testFunctionToString() {
		TSValue f = executeScriptFromString("function f(x) {return x; }; f;");
		assertTrue(TSObject.toString(getExecutionVisitor(), f).startsWith("function f(x) {"));
		f = executeScriptFromString("print;");
		assertTrue(TSObject.toString(getExecutionVisitor(), f).startsWith("function print("));
	}
	

}

