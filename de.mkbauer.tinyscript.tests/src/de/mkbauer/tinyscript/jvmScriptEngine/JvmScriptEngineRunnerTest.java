package de.mkbauer.tinyscript.jvmScriptEngine;


import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.xtext.testing.XtextRunner;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.jvmscriptengine.JvmScriptEngineRunner;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;
import de.mkbauer.tinyscript.ts.Tinyscript;

@RunWith(XtextRunner.class)
public class JvmScriptEngineRunnerTest extends TinyscriptInterpreterTestHelper {
	
	final static String SCRIPT_ENGINE = "graal.js"; // "nashorn"

	@Inject
	private Provider<JvmScriptEngineRunner> jvmRunnerProvider;
	
	public TSValue executeScriptFromString(String script) {
		Tinyscript ast = parseScriptFromString(script);
		// FIXME: This works but is probably not correct...
		// NashornExecutor nashorn = injectorProvider.getInjector().getInstance(NashornExecutor.class);
		JvmScriptEngineRunner engineRunner = jvmRunnerProvider.get();
		engineRunner.execute(ast);
		return TSValue.UNDEFINED;
	}
	
	public TSValue executeScriptFromFileWithNashorn(String filename) {
		Tinyscript ast = parseScriptFromFile(filename);
		// FIXME: This works but is probably not correct...
		JvmScriptEngineRunner engineRunner = jvmRunnerProvider.get();
		engineRunner.execute(ast);
		return TSValue.UNDEFINED;
	}
	
	@Test
	public void testHelloWorld() {
		executeScriptFromString("var hello = \"Hello World!\"; assert (hello == \"Hello World!\");");
	}
	
	@Test
	public void testRecursiveFibonacci() {
		executeScriptFromFileWithNashorn("fibonacci.ts");
	}
	
	@Test
	public void testIterativeFibonacci() {
		executeScriptFromFileWithNashorn("fibonacci_loop.ts");
	}
	
	@Test
	public void testRecursiveLinkedList() {
		executeScriptFromFileWithNashorn("linked_list.ts");
	}
	
	@Test
	public void testAdditionExpressions() {
		executeScriptFromFileWithNashorn("additions.ts");
	}
	
	@Test
	public void testbuiltinPrint() {
		executeScriptFromString("var hello=\"Hello!\"; print(hello);");
	}
	
	@Test
	public void testNumericFor() {
		executeScriptFromString("var result = 0; for (var i = 1, 3) { result = result + i; } assert(result == 6);");
	}
	
	@Test 
	public void testNumericForWithShadowing() {
		executeScriptFromString("var result = 0; var i = 10; for (var i = 1, 3) { result = result + i; } assert(result == 6); assert(i == 10);");
	}
	
	@Test
	public void testIterableFor() {
		executeScriptFromFileWithNashorn("simple_array_for.ts");
	}
	
	@Test
	public void testArrowFunction() {
		executeScriptFromString("var f = x => x*x; assert(9 == f(3));");
	}
	
	@Test
	public void testMapWithArrowFunction() {
		executeScriptFromString("var arr = [1, 5, 4, 2, 9, 7]; var squares = arr.map(x => x*x); " 
				+ "assert(25 == squares[1]); assert(49 == squares[5]);");
	}
	
	@Test
	public void testFilterWithArrowFunction() {
		executeScriptFromString("var arr = [1, 5, 4, 2, 9, 7]; var odds = arr.filter(x => x % 2 == 1); " 
				+ "assert(4 == odds.length);");
	}
	
	// @Test
	public void testBench() {
		executeScriptFromFileWithNashorn("ianbull_bench.ts");
	}
	
}
