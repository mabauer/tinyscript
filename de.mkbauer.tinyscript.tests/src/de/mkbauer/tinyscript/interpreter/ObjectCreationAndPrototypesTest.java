package de.mkbauer.tinyscript.interpreter;

import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@ExtendWith(InjectionExtension.class)
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
		executeScriptFromString("let obj = {}; assert ( Object.prototype == obj.__proto__);");
		executeScriptFromString("assert (!Object.__proto__.__proto__.__proto__);");
	}
	
	@Test
	public void testHasOwnProperty() {
		executeScriptFromString("assert(Object.__proto__.__proto__.hasOwnProperty(\"toString\"));");
		executeScriptFromString("let obj = {}; assert (obj.__proto__.hasOwnProperty(\"toString\"));");
		executeScriptFromString("let obj = {}; assert (!obj.hasOwnProperty(\"toString\"));");
	}
	
	@Test 
	public void testCreationViaNew() {
		executeScriptFromFile("objectcreation_via_new.tys");
	}
	
	@Test 
	public void testOO() {
		executeScriptFromFile("oo.tys");
	}
	
	@Test 
	public void testOO2() {
		executeScriptFromFile("oo2.tys");
	}
	
	@Test 
	public void testInstanceOfForBuiltins() {
		executeScriptFromString("let obj = {}; assert (obj instanceof Object);");
		executeScriptFromString("function f(x) {return x; } assert (f instanceof Function);");
		executeScriptFromString("assert (print instanceof Function);"); 
		executeScriptFromString("let obj = {}; assert (obj.toString instanceof Function);"); 
		executeScriptFromString("let f = function (x){return x*x;}; assert (f.toString instanceof Function);");
	}

}
