package de.mkbauer.tinyscript.runtime;

import org.junit.jupiter.api.Test;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;


@ExtendWith(InjectionExtension.class)
public class FunctionObjectTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testCall() {
		executeScriptFromString("function f(a, b) { return 10*a + b; }; "
				+ "assert (f.call(null, 2, 1) == 21);"
				+ "assert (f.call(3, 2, 1) == 21);");
	}
	
	@Test
	public void testApply() {
		executeScriptFromString("function f(a, b) { return 10*a + b; }; "
				+ "assert (f.apply(null, [2, 1]) == 21);"
				+ "assert (f.apply(3, [2, 1]) == 21);");
	}
	
	@Test
	public void testApplyWithoutArgs() {
		executeScriptFromString("function f() { return 42; }; "
				+ "assert (f.apply(null, []) == 42);"
				+ "assert (f.apply(null) == 42);"
				// + "assert (f.apply() == 42);"
				);
	}

}
