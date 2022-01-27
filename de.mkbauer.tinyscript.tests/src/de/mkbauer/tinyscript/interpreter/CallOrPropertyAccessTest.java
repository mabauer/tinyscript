package de.mkbauer.tinyscript.interpreter;

import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@RunWith(XtextRunner.class)
public class CallOrPropertyAccessTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testSimplePropertyAccess() {
		executeScriptFromString("var o = {}; o.key=1; assert (o.key==1);");
		executeScriptFromString("var o = {}; o[\"key\"]=1; assert (o[\"key\"]==1);");
	}
	
	@Test
	public void testNestedPropertyAccess() {
		executeScriptFromString("var o = {}; o.key1 = {}; o.key1.key2 = 1; assert (o.key1.key2==1);");
		executeScriptFromString("var o = {}; o.key1 = {}; var test = o.key1; test.key2 = 1; assert (o.key1.key2==1);");
	}
	
	@Test 
	public void testFunctionCall() {
		executeScriptFromString("function f() { return 2; } assert (2 == f());");
	}
	
	@Test 
	public void testMethodCall() {
		executeScriptFromString("var o = {}; o.m = function() { return 2; }; assert (2 == o.m());");
	}
	
	@Test
	public void testPropertyAccessOnFunctionResult() {
		executeScriptFromString("function f() { var o = {}; o.key1 = \"Test\"; return o; } assert (\"Test\" == f().key1);");
		executeScriptFromString("function f(o) { o.key1 = \"Test\"; return o; } var o = {}; f(o).key2 = 2; assert(2 == f(o).key2);");
	}
	
	@Test
	public void testFunctionReturningFunction() {
		executeScriptFromString("function f() { var inner = function() { return \"Test\"; }; return inner; } assert (\"Test\" == f()());");
	}

}
