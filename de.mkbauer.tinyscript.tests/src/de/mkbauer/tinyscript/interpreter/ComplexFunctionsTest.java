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
public class ComplexFunctionsTest extends TinyscriptInterpreterTestHelper {
	
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
	
	@Test
	public void testPrimes() {
		TSValue value = executeScriptFromFile("primes.ts");
	}	

}

