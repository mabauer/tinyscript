package de.mkbauer.tinyscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.TSStacktraceElement;
import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@ExtendWith(InjectionExtension.class)
public class StacktraceTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testFunctionCausingException() {
		try {
			TSValue value = executeScriptFromString("function f(x) {assert(x!=2); return x*x;}; assert (f(2)==4);");
			fail();
		}
		catch (TinyscriptAssertationError e) {
			TSStacktraceElement[] st = e.getTinyscriptStacktrace();
			assertEquals("f", st[0].getFunctionName());
			assertEquals("global", st[1].getFunctionName());
		}
	}

}
