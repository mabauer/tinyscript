package de.mkbauer.tinyscript.interpreter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.TSStacktraceElement;
import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@RunWith(XtextRunner.class)
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
