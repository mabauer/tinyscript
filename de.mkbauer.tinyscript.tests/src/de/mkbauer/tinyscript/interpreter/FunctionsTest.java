package de.mkbauer.tinyscript.interpreter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.TSStacktraceElement;
import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;


@RunWith(XtextRunner.class)
public class FunctionsTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testSimpleFunctionExpression() {
		executeScriptFromString("var f = function(x) {return x*x;}; assert (f(2)==4);");
		executeScriptFromString("var f = function(x) {return x*x;}; var result = f(2); assert (result==4);");
		executeScriptFromString("var f = function(x) {return x*x;}; var arg = 2; var result = f(arg); assert (result==4);");
		executeScriptFromString("var f = function(x) {return x*x;}; var arg = 2; var result = f(arg+1); assert (result==9);");
	}
	
	@Test
	public void testSimpleAnonymousFunctionExpression() {
		TSValue value = executeScriptFromString("(function(x) { assert(x==2); return x*x;})(2);");
		assertEquals(4, value.asDouble(), 0.0001);
	}
	
	@Test
	public void testSimpleFunctionDeclaration() {
		executeScriptFromString("function f(x) {assert(x==2); return x*x;}; assert (f(2)==4);");
	}

	
	@Test
	public void testFunctionExpressionWithoutArgs() {
		executeScriptFromString("var f = function() {return 4;}; assert (f()==4);");
	}
	
	@Test
	public void testNamedFunctionExpression() {
		executeScriptFromString("var f = function square(x) {return x*x;}; assert (f(2)==4);");
	}

	
	@Test
	public void testFunctionCausingException() {
		// TODO: Move to an extra test, checking stacktrace generation
		try {
			executeScriptFromString("function f(x) {assert(x!=2); return x*x;}; assert (f(2)==4);");
			fail();
		}
		catch (TinyscriptAssertationError e) {
			TSStacktraceElement[] st = e.getTinyscriptStacktrace();
			assertEquals("f", st[0].getFunctionName());
			assertEquals("global", st[1].getFunctionName());
		}
	}	
	
	@Test
	public void testFunctionCallOnLHSExpression() {
		TSValue value = executeScriptFromString("function f() {return {};} f().key1 = \"Hello\";");
		assertEquals("Hello", value.asString());
		try {
			value = executeScriptFromString("function f() {return {};} f() = \"Hello\";");
			fail("Expected an exception indicating an invalid lefthand-side expression.");
		}
		catch (TinyscriptTypeError e) {
			assertTrue(true);
		}
	}
	
	@Test
	public void testCallToBuiltInFunction() {
		executeScriptFromString("var x = Math.sqrt(4); assert(x==2);");	
		
	}
	
	@Test
	public void testCallWithTooManyArgs() {
		executeScriptFromString("function f(a, b) { return a*b; }; assert(f(2, 3, 4)==6);");	
		
	}
	
	@Test
	public void testCallWithTooFewArgs() {
		TSValue result = executeScriptFromString("function f(a, b) { return b; }; f(2);");	
		assertEquals(TSValue.UNDEFINED, result);
	}

}

