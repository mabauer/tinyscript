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

}
