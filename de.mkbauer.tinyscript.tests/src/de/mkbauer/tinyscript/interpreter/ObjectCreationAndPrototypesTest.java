package de.mkbauer.tinyscript.interpreter;

import org.eclipse.xtext.junit4.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.TinyscriptInterpreterTestHelper;

@RunWith(XtextRunner.class)
public class ObjectCreationAndPrototypesTest extends
		TinyscriptInterpreterTestHelper {
	
	@Test
	public void testHaveBuiltinBaseObjects() {
		executeScriptFromString("assert (Object);");
		executeScriptFromString("assert (Function);");
	}
	
	@Test
	public void testBuiltinPrototypeand__Proto__Properties() {
		executeScriptFromString("assert (Function.__proto__ == Function.prototype);");
		executeScriptFromString("assert (Object.__proto__ == Function.__proto__);");
		executeScriptFromString("assert (Object.prototype == Object.__proto__.__proto__);");
		executeScriptFromString("var obj = {}; assert ( Object.prototype == obj.__proto__);");
		executeScriptFromString("assert (!Object.__proto__.__proto__.__proto__);");
	}
	
	@Test
	public void testHasOwnProperty() {
		executeScriptFromString("assert(Object.__proto__.__proto__.hasOwnProperty(\"toString\"));");
		executeScriptFromString("var obj = {}; assert (obj.__proto__.hasOwnProperty(\"toString\"));");
		executeScriptFromString("var obj = {}; assert (!obj.hasOwnProperty(\"toString\"));");
	}
	
	@Test 
	public void testCreationViaNew() {
		executeScriptFromFile("objectcreation_via_new.ts");
	}

}
