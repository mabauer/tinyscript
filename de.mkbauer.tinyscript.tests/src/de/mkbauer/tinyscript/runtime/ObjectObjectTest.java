package de.mkbauer.tinyscript.runtime;

import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@ExtendWith(InjectionExtension.class)
public class ObjectObjectTest extends TinyscriptInterpreterTestHelper {

	@Test
	public void testkeys() {
		executeScriptFromString("var obj = {key1: \"Markus\", key2: \"Bauer\"}; var arr = Object.keys(obj); "
				+ "assert (\"key1\" == arr[0]); assert (\"key2\" == arr[1]);");
	}
	
	@Test
	public void testkeysOnArray() {
		executeScriptFromString("var arr = [\"Markus\", \"Bauer\"]; arr.key = \"Test\"; var keys = Object.keys(arr); "
				+ "assert (\"0\" == keys[0]); assert (\"1\" == keys[1]); assert (\"key\" == keys[2]);");
	}
	
	@Test 
	public void testLoopWithObjectKeys() {
		executeScriptFromFile("loop_with_object_keys.ts");
	}
	
	@Test
	public void testgetOwnPropertyDescriptor() {
		executeScriptFromString("var obj = {key1: \"Markus\"}; var pd = Object.getOwnPropertyDescriptor(obj, \"key1\"); "
				+ "assert (pd.value == \"Markus\"); "
				+ "assert ((pd.writable == true) && (pd.enumerable == true) && (pd.configurable == true));"
				);
	}
	
	@Test
	public void testdefineProperty() {
		executeScriptFromString("var obj = {key1: \"Bauer\"}; "
				+ "Object.defineProperty(obj, \"key2\", {value: \"Markus\", writable: false}); "
				+ "assert (obj.key2 == \"Markus\"); "
				+ "var pd = Object.getOwnPropertyDescriptor(obj, \"key2\"); "
				+ "assert (pd.value == \"Markus\"); "
				+ "assert ((pd.writable == false) && (pd.enumerable == true) && (pd.configurable == true));"
				);
	}
	
}
