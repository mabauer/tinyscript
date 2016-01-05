package de.mkbauer.tinyscript.interpreter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.junit4.util.ParseHelper;
import org.eclipse.xtext.junit4.validation.ValidationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import de.mkbauer.tinyscript.TinyscriptInjectorProvider;
import de.mkbauer.tinyscript.TinyscriptInterpreterTestHelper;
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.Tinyscript;

@RunWith(XtextRunner.class)
public class ObjectExpressionsTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testSimpleObjectExpression() {
		TSValue value = executeScriptFromString("var o = {}; o.key=1; assert (o.key==1);");
		value = executeScriptFromString("var o = {}; o[\"key\"]=1; assert (o[\"key\"]==1);");
	}
	
	@Test
	public void testChainedObjectExpression() {
		TSValue value = executeScriptFromString("var o = {}; o.key1 = {}; o.key1.key2 = 1; assert (o.key1.key2==1);");
		value = executeScriptFromString("var o = {}; o.key1 = {}; var test = o.key1; test.key2 = 1; assert (o.key1.key2==1);");
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
	public void testArrayAddition() {
		TSValue value = executeScriptFromString("var a1 = [ \"Hello\", \"Dear\"], a2 = [ \"Markus\", \"Bauer\" ]; var result = a1 + a2; assert (result[0]==\"Hello\"); assert (result[3]==\"Bauer\");");
		value = executeScriptFromString("var s = \"Hello\"; var a = [ \"Markus\", \"Bauer\" ]; var result = s + a; assert (result[0]==\"Hello\"); assert (result[2]==\"Bauer\");");
		value = executeScriptFromString("var s = \"Hello\"; var a = [ \"Markus\", \"Bauer\" ]; ; var result = a + s; assert (result[0]==\"Markus\"); assert (result[2]==\"Hello\");");
	}
	
	@Test
	public void testArrayLength() {
		TSValue value = executeScriptFromString("var a = [ \"Hello\", \"Dear\"]; assert (a.length == 2);");
	}
	
}
