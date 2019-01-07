package de.mkbauer.tinyscript.runtime;

import static org.junit.Assert.fail;

import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@RunWith(XtextRunner.class)
public class ObjectObjectTest extends TinyscriptInterpreterTestHelper {

	@Test
	public void testkeys() {
		TSValue value = executeScriptFromString("var obj = {key1: \"Markus\", key2: \"Bauer\"}; var arr = Object.keys(obj); "
				+ "assert (\"key1\" == arr[0]); assert (\"key2\" == arr[1]);");
	}
	
	@Test
	public void testkeysOnArray() {
		TSValue value = executeScriptFromString("var arr = [\"Markus\", \"Bauer\"]; arr.key = \"Test\"; var keys = Object.keys(arr); "
				+ "assert (\"0\" == keys[0]); assert (\"1\" == keys[1]); assert (\"key\" == keys[2]);");
	}
	
	@Test 
	public void testLoopWithObjectKeys() {
		TSValue value = executeScriptFromFile("loop_with_object_keys.ts");
	}
	
}
