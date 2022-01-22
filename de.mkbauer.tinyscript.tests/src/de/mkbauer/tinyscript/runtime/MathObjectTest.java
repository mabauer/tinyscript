package de.mkbauer.tinyscript.runtime;

import org.junit.Test;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.runner.RunWith;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;


@RunWith(XtextRunner.class)
public class MathObjectTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testsqrt() {
		executeScriptFromString("var x = 4; var result = Math.sqrt(x); assert (2 == result);");
	}
	
	@Test
	public void testround() {
		executeScriptFromString("var x = Math.PI; var result = Math.round(x); assert (3 == result);");
	}
	
	@Test
	public void testrandom() {
		executeScriptFromString("var result = Math.random(); assert (0.0 <= result && result < 1.0);");
	}

}
