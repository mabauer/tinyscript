package de.mkbauer.tinyscript.webdemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExecutionErrorsTests {

	private TinyscriptExecutionService executionService;

	@BeforeEach
	public void setUp() {
		executionService = new TinyscriptExecutionService();
	}

	@Test
	public void testResourceLimitViolationReportsTheLine() {
		TinyscriptExecutionResult result = executionService.executeScriptFromString(
				"function f(n) {\n  return f(n + 1);\n}\nf(0);");
		assertTrue(result.getErrorMessage().startsWith("TinyscriptResourceLimitViolation"), result.getErrorMessage());
		assertEquals(2, result.getErrorLine());
	}

	@Test
	public void testFloodingTheOutputHitsTheOutputLimit() {
		TinyscriptExecutionResult result = executionService.executeScriptFromString(
				"while (true) {\n  print(\"0123456789\");\n}");
		assertEquals("TinyscriptResourceLimitViolation: Output limit reached", result.getErrorMessage());
		assertEquals(2, result.getErrorLine());
		assertTrue(result.getOutput().length() <= TinyscriptExecutionService.MAX_OUTPUT_SIZE);
	}

	@Test
	public void testDoublingAStringHitsTheStringLengthLimit() {
		// Used to fill the heap: a 1 MB string printed in a loop
		TinyscriptExecutionResult result = executionService.executeScriptFromString(
				"var s = \"x\";\nfor (var i = 1, 20) {\n  s = s + s;\n}\nwhile (true) { print(s); }");
		assertEquals("TinyscriptResourceLimitViolation: String length limit reached", result.getErrorMessage());
		assertEquals(3, result.getErrorLine());
	}

}
