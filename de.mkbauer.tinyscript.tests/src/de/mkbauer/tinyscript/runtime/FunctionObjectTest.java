package de.mkbauer.tinyscript.runtime;

import org.junit.Test;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.runner.RunWith;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;


@RunWith(XtextRunner.class)
public class FunctionObjectTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testCall() {
		executeScriptFromString("function f(a, b) { return 10*a + b; }; "
				+ "assert (f.call(this, 2, 1) == 21);"
				+ "assert (f.call(3, 2, 1) == 21);");
	}
	
	@Test
	public void testApply() {
		executeScriptFromString("function f(a, b) { return 10*a + b; }; "
				+ "assert (f.apply(this, [2, 1]) == 21);"
				+ "assert (f.apply(3, [2, 1]) == 21);");
	}
	
	@Test
	public void testApplyWithoutArgs() {
		executeScriptFromString("function f() { return 42; }; "
				+ "assert (f.apply(this, []) == 42);"
				+ "assert (f.apply(this) == 42);"
				// + "assert (f.apply() == 42);"
				);
	}

}
