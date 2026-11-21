package de.mkbauer.tinyscript.runtime;

import org.junit.jupiter.api.Test;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;


@ExtendWith(InjectionExtension.class)
public class MathObjectTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testsqrt() {
		executeScriptFromString("let x = 4; let result = Math.sqrt(x); assert (2 == result);");
	}
	
	@Test
	public void testround() {
		executeScriptFromString("let x = Math.PI; let result = Math.round(x); assert (3 == result);");
	}
	
	@Test
	public void testrandom() {
		executeScriptFromString("let result = Math.random(); assert (0.0 <= result && result < 1.0);");
	}

}
