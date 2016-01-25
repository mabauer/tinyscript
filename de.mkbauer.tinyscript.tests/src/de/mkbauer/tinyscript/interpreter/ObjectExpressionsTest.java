package de.mkbauer.tinyscript.interpreter;

import org.eclipse.xtext.junit4.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.TinyscriptInterpreterTestHelper;

@RunWith(XtextRunner.class)
public class ObjectExpressionsTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testSimpleObjectExpression() {
		TSValue value = executeScriptFromString("var o = {}; o.key=1; assert (o.key==1);");
		value = executeScriptFromString("var o = {}; o[\"key\"]=1; assert (o[\"key\"]==1);");
	}
	
	@Test
	public void testMixedPropertyAccessExpressions() {
		TSValue value = executeScriptFromString("var o = {}; o.key = \"Test\"; assert (o[\"key\"]==\"Test\");");
	}
	
	@Test
	public void testObjectInitializers() {
		TSValue value = executeScriptFromString("var o = { key1: \"Hello\", key2: \"World\" }; assert (o[\"key1\"]==\"Hello\");"); //  && o.key2 == \"World\"
	}
	
	@Test
	public void testArrayInitializers() {
		TSValue value = executeScriptFromString("var a = [ \"Hello\", \"Dear\", \"World\", 3, true, \"Markus\" + \"Bauer\" ]; assert (a[0]==\"Hello\"); assert(a[3]==3); assert(a[4]==true); assert(a[5]==\"MarkusBauer\");");
	}
	
	@Test
	public void testNullObject() {
		TSValue value = executeScriptFromString("var x = null; assert (x == null);");
	}
	
	@Test
	public void testArrayLength() {
		TSValue value = executeScriptFromString("var a = [ \"Hello\", \"Dear\"]; assert (a.length == 2);");
	}
	
}
