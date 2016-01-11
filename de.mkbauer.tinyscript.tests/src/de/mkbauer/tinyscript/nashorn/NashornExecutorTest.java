package de.mkbauer.tinyscript.nashorn;

import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.TinyscriptInjectorProvider;
import de.mkbauer.tinyscript.TinyscriptInterpreterTestHelper;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.ts.Tinyscript;

@RunWith(XtextRunner.class)
public class NashornExecutorTest extends TinyscriptInterpreterTestHelper {

	@Inject
	private Provider<NashornExecutor> nashornExecutorProvider;
	
	public TSValue executeScriptFromString(String script) {
		Tinyscript ast = parseScriptFromString(script);
		// FIXME: This works but is probably not correct...
		// NashornExecutor nashorn = injectorProvider.getInjector().getInstance(NashornExecutor.class);
		NashornExecutor nashorn = nashornExecutorProvider.get();
		nashorn.execute(ast);
		return TSValue.UNDEFINED;
	}
	
	public TSValue executeScriptFromFileWithNashorn(String filename) {
		Tinyscript ast = parseScriptFromFile(filename);
		// FIXME: This works but is probably not correct...
		NashornExecutor nashorn = nashornExecutorProvider.get();
		nashorn.execute(ast);
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
	public void testbuiltinPrint() {
		executeScriptFromString("var hello=\"Hello!\"; print(hello);");
	}
	
}
