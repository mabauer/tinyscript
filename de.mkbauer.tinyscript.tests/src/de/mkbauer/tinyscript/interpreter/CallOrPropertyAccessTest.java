package de.mkbauer.tinyscript.interpreter;

import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@ExtendWith(InjectionExtension.class)
public class CallOrPropertyAccessTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testSimplePropertyAccess() {
		executeScriptFromString("let o = {}; o.key=1; assert (o.key==1);");
		executeScriptFromString("let o = {}; o[\"key\"]=1; assert (o[\"key\"]==1);");
	}
	
	@Test
	public void testNestedPropertyAccess() {
		executeScriptFromString("let o = {}; o.key1 = {}; o.key1.key2 = 1; assert (o.key1.key2==1);");
		executeScriptFromString("let o = {}; o.key1 = {}; let test = o.key1; test.key2 = 1; assert (o.key1.key2==1);");
	}
	
	@Test 
	public void testFunctionCall() {
		executeScriptFromString("function f() { return 2; } assert (2 == f());");
	}
	
	@Test 
	public void testMethodCall() {
		executeScriptFromString("let o = {}; o.m = function() { return 2; }; assert (2 == o.m());");
	}
	
	@Test
	public void testPropertyAccessOnFunctionResult() {
		executeScriptFromString("function f() { let o = {}; o.key1 = \"Test\"; return o; } assert (\"Test\" == f().key1);");
		executeScriptFromString("function f(o) { o.key1 = \"Test\"; return o; } let o = {}; f(o).key2 = 2; assert(2 == f(o).key2);");
	}
	
	@Test
	public void testFunctionReturningFunction() {
		executeScriptFromString("function f() { let inner = function() { return \"Test\"; }; return inner; } assert (\"Test\" == f()());");
	}

}
