package de.mkbauer.tinyscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.TSStacktraceElement;
import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;


@ExtendWith(InjectionExtension.class)
public class ComplexFunctionsTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testRecursiveFibonacci() {
		TSValue value = executeScriptFromFile("fibonacci.tys");
	}
	
	@Test
	public void testIterativeFibonacci() {
		TSValue value = executeScriptFromFile("fibonacci_loop.tys");
	}
	
	@Test
	public void testRecursiveFactorial() {
		TSValue value = executeScriptFromFile("factorial.tys");
	}
	
	@Test
	public void testRecursiveggT() {
		TSValue value = executeScriptFromFile("ggt.tys");
	}
	
	@Test
	public void testRecursiveLinkedList() {
		TSValue value = executeScriptFromFile("linked_list.tys");
	}
	
	@Test
	public void testCounterWithClosures() {
		TSValue value = executeScriptFromFile("counter_with_closures.tys");
	}	
	
	@Test
	public void testPrimes() {
		TSValue value = executeScriptFromFile("primes.tys");
	}	

}

