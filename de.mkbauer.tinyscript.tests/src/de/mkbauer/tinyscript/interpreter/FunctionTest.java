package de.mkbauer.tinyscript.interpreter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.TinyscriptInjectorProvider;
import de.mkbauer.tinyscript.TinyscriptInterpreterTestHelper;
import de.mkbauer.tinyscript.ts.Expression;

@RunWith(XtextRunner.class)
public class FunctionTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testSimpleFunctionExpression() {
		TSValue value = executeScriptFromString("var f = function(x) {return x*x;}; assert (f(2)==4);");
		value = executeScriptFromString("var f = function(x) {return x*x;}; var result = f(2); assert (result==4);");
		value = executeScriptFromString("var f = function(x) {return x*x;}; var arg = 2; var result = f(arg); assert (result==4);");
		value = executeScriptFromString("var f = function(x) {return x*x;}; var arg = 2; var result = f(arg+1); assert (result==9);");
	}
	
	@Test
	public void testSimpleAnonymousFunctionExpression() {
		TSValue value = executeScriptFromString("(function(x) { assert(x==2); return x*x;})(2);");
		assertEquals(4, value.asDouble(), 0.0001);
	}
	
	@Test
	public void testSimpleFunctionDeclaration() {
		TSValue value = executeScriptFromString("function f(x) {assert(x==2); return x*x;}; assert (f(2)==4);");
	}

	
	@Test
	public void testFunctionExpressionWithoutArgs() {
		TSValue value = executeScriptFromString("var f = function() {return 4;}; assert (f()==4);");
		value = executeScriptFromString("var f = function() {return 4;}; assert (f(2)==4);");
	}
	
	@Test
	public void testRecursiveFibonacci() {
		TSValue value = executeScriptFromFile("fibonacci.ts");
	}
	
	@Test
	public void testIterativeFibonacci() {
		TSValue value = executeScriptFromFile("fibonacci_loop.ts");
	}
	
	@Test
	public void testRecursiveFactorial() {
		TSValue value = executeScriptFromFile("factorial.ts");
	}
	
	@Test
	public void testRecursiveggT() {
		TSValue value = executeScriptFromFile("ggt.ts");
	}
	
	@Test
	public void testRecursiveLinkedList() {
		TSValue value = executeScriptFromFile("linked_list.ts");
	}
	
	@Test
	public void testCounterWithClosures() {
		TSValue value = executeScriptFromFile("counter_with_closures.ts");
	}	
	
/*	
    @Test
	public void testModulesWithClosures() {
		TSValue value = executeScriptFromFile("modules_via_closures.ts");
	} 
*/
	
	@Test
	public void testPrimes() {
		TSValue value = executeScriptFromFile("primes.ts");
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
		TSValue value = executeScriptFromString("var x = Math.sqrt(4); assert(x==2);");	
		
	}

}

