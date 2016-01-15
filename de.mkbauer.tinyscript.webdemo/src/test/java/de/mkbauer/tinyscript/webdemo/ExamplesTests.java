package de.mkbauer.tinyscript.webdemo;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

public class ExamplesTests {
	
	private static final String PUBLIC_FOLDER = "public";
	
	private TinyscriptExecutionService executionService;
	
	@Before
	public void setUp()  {
		executionService = new TinyscriptExecutionService();
	}
	
	private static String convertStreamToString(java.io.InputStream in) {
	    java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
	    String result = s.hasNext() ? s.next() : "";
	    return result;
	}
	
	private TinyscriptExecutionResult executeScriptFromFile(String filename) {
		InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream(PUBLIC_FOLDER + java.io.File.separator + filename);
		String script = convertStreamToString(in);
		TinyscriptExecutionResult result = executionService.executeScriptFromString(script);
		return result;
	}
	
	@Test
	public void testHelloWorld() {
		TinyscriptExecutionResult result = executeScriptFromFile("helloworld.ts");
		assertEquals(0, result.getErrorCode());
	}
	
	@Test
	public void testExpressions() {
		TinyscriptExecutionResult result = executeScriptFromFile("expressions.ts");
		assertEquals(0, result.getErrorCode());
	}
	
	@Test
	public void testStrings() {
		TinyscriptExecutionResult result = executeScriptFromFile("strings.ts");
		assertEquals(0, result.getErrorCode());
	}
	
	@Test
	public void testPrimes() {
		TinyscriptExecutionResult result = executeScriptFromFile("primes.ts");
		assertEquals(0, result.getErrorCode());
	}
	
	@Test
	public void testFibonacci() {
		TinyscriptExecutionResult result = executeScriptFromFile("fibonacci.ts");
		assertEquals(0, result.getErrorCode());
	}
	
	@Test
	public void testFibonacciRecursive() {
		TinyscriptExecutionResult result = executeScriptFromFile("fibonacci_recursive.ts");
		assertEquals(0, result.getErrorCode());
	}
	
	@Test
	public void testClosures() {
		TinyscriptExecutionResult result = executeScriptFromFile("closures.ts");
		assertEquals(0, result.getErrorCode());
	}
	
	@Test
	public void testOO() {
		TinyscriptExecutionResult result = executeScriptFromFile("oo.ts");
		assertEquals(0, result.getErrorCode());
	}

}
