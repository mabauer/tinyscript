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
	public void testSparseArrayHitsTheObjectSizeLimit() {
		// Used to fill the heap: the gap was filled with 200 million undefined values
		TinyscriptExecutionResult result = executionService.executeScriptFromString(
				"var a = [];\na[200000000] = 1;");
		assertEquals("TinyscriptResourceLimitViolation: Object size limit reached", result.getErrorMessage());
		assertEquals(2, result.getErrorLine());
	}

	@Test
	public void testStandardLibraryModulesCanBeImported() {
		TinyscriptExecutionResult result = executionService.executeScriptFromString(
				"import range, sum from \"std:array\";\nprint(sum(range(4)));");
		assertEquals(0, result.getErrorCode(), result.getErrorMessage());
		assertEquals("6\n", result.getOutput());
	}

	@Test
	public void testStdFsIsNotAvailable() {
		TinyscriptExecutionResult result = executionService.executeScriptFromString(
				"import * as fs from \"std:fs\";\nprint(fs.readFile(\"/etc/passwd\"));");
		assertTrue(result.getErrorMessage().contains("std:fs is not available when running sandboxed"), result.getErrorMessage());
		assertEquals(1, result.getErrorLine());
	}

	@Test
	public void testFilesCannotBeImported() {
		TinyscriptExecutionResult result = executionService.executeScriptFromString(
				"import x from \"./secret\";\nprint(x);");
		assertTrue(result.getErrorMessage().contains("Only standard library modules (std:…) are available here"), result.getErrorMessage());
		assertEquals(1, result.getErrorLine());
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
