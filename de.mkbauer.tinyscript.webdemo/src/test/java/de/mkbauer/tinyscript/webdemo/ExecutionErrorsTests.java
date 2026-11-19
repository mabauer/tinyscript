package de.mkbauer.tinyscript.webdemo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class ExecutionErrorsTests {

	private TinyscriptExecutionService executionService;

	@Before
	public void setUp() {
		executionService = new TinyscriptExecutionService();
	}

	@Test
	public void testResourceLimitViolationReportsTheLine() {
		TinyscriptExecutionResult result = executionService.executeScriptFromString(
				"function f(n) {\n  return f(n + 1);\n}\nf(0);");
		assertTrue(result.getErrorMessage(), result.getErrorMessage().startsWith("TinyscriptResourceLimitViolation"));
		assertEquals(2, result.getErrorLine());
	}

}
