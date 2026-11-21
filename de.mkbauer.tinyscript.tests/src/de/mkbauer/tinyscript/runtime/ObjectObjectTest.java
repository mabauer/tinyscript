package de.mkbauer.tinyscript.runtime;

import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@ExtendWith(InjectionExtension.class)
public class ObjectObjectTest extends TinyscriptInterpreterTestHelper {

	@Test
	public void testkeys() {
		executeScriptFromString("let obj = {key1: \"Markus\", key2: \"Bauer\"}; let arr = Object.keys(obj); "
				+ "assert (\"key1\" == arr[0]); assert (\"key2\" == arr[1]);");
	}
	
	@Test
	public void testkeysOnArray() {
		// Arrays have no named properties, so their keys are the indexes
		executeScriptFromString("let arr = [\"Markus\", \"Bauer\"]; let keys = Object.keys(arr); "
				+ "assert (keys.length == 2); assert (\"0\" == keys[0]); assert (\"1\" == keys[1]);");
	}
	
	@Test 
	public void testLoopWithObjectKeys() {
		executeScriptFromFile("loop_with_object_keys.tys");
	}
	
	@Test
	public void testgetOwnPropertyDescriptor() {
		executeScriptFromString("let obj = {key1: \"Markus\"}; let pd = Object.getOwnPropertyDescriptor(obj, \"key1\"); "
				+ "assert (pd.value == \"Markus\"); "
				+ "assert ((pd.writable == true) && (pd.enumerable == true) && (pd.configurable == true));"
				);
	}
	
	@Test
	public void testdefineProperty() {
		executeScriptFromString("let obj = {key1: \"Bauer\"}; "
				+ "Object.defineProperty(obj, \"key2\", {value: \"Markus\", writable: false}); "
				+ "assert (obj.key2 == \"Markus\"); "
				+ "let pd = Object.getOwnPropertyDescriptor(obj, \"key2\"); "
				+ "assert (pd.value == \"Markus\"); "
				+ "assert ((pd.writable == false) && (pd.enumerable == true) && (pd.configurable == true));"
				);
	}
	
}
