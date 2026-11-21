package de.mkbauer.tinyscript.interpreter;

import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@ExtendWith(InjectionExtension.class)
public class ObjectExpressionsTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testSimpleObjectExpression() {
		TSValue value = executeScriptFromString("let o = {}; o.key=1; assert (o.key==1);");
		value = executeScriptFromString("let o = {}; o[\"key\"]=1; assert (o[\"key\"]==1);");
	}
	
	@Test
	public void testMixedPropertyAccessExpressions() {
		TSValue value = executeScriptFromString("let o = {}; o.key = \"Test\"; assert (o[\"key\"]==\"Test\");");
	}
	
	@Test
	public void testObjectInitializers() {
		TSValue value = executeScriptFromString("let o = { key1: \"Hello\", key2: \"World\" }; assert (o[\"key1\"]==\"Hello\");"); //  && o.key2 == \"World\"
	}
	
	@Test
	public void testArrayInitializers() {
		TSValue value = executeScriptFromString("let a = [ \"Hello\", \"Dear\", \"World\", 3, true, \"Markus\" + \"Bauer\" ]; assert (a[0]==\"Hello\"); assert(a[3]==3); assert(a[4]==true); assert(a[5]==\"MarkusBauer\");");
	}
	
	@Test
	public void testNullObject() {
		TSValue value = executeScriptFromString("let x = null; assert (x == null);");
	}
	
	@Test
	public void testArrayLength() {
		TSValue value = executeScriptFromString("let a = [ \"Hello\", \"Dear\"]; assert (a.length == 2);");
	}
	
}
