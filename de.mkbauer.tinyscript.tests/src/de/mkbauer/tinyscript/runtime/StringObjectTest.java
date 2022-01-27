package de.mkbauer.tinyscript.runtime;

import static org.junit.Assert.fail;

import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@RunWith(XtextRunner.class)
public class StringObjectTest extends TinyscriptInterpreterTestHelper {

	@Test
	public void testIndexOf() {
		TSValue value = executeScriptFromString("var str1 = \"Hallo Hugo\", str2 = \"Hugo\"; assert (str1.indexOf(str2) == 6);");	
	}
	
	@Test
	public void testConcat() {
		TSValue value = executeScriptFromString("var str1 = new String(\"Hallo\"), str2 = new String (\"Hugo\"); assert (str1 + \" \" + str2 == \"Hallo Hugo\");");	
	}
	
	@Test
	public void testCharAt() {
		TSValue value = executeScriptFromString("var str = \"Hallo\"; assert (str.charAt(0) == \"H\");");
		value = executeScriptFromString("var str = \"Hallo\"; assert (str.charAt(4) == \"o\");");
		value = executeScriptFromString("var str = \"Hallo\"; assert (str.charAt(-1) == \"\");");
		value = executeScriptFromString("var str = \"Hallo\"; assert (str.charAt(str.length) == \"\");");
		
	}
	
	@Test
	public void testSubstring() {
		TSValue value = executeScriptFromString("var str = \"Hallo Markus\"; assert (str.substring(0, str.length) == \"Hallo Markus\");");
		value = executeScriptFromString("var str = \"Hallo Markus\"; assert (str.substring(5, 0) == \"Hallo\");");
		value = executeScriptFromString("var str = \"Hallo Markus\"; assert (str.substring(-1, 25) == \"Hallo Markus\");");
		value = executeScriptFromString("var str = \"Hallo Markus\"; assert (str.substring(6, str.length) == \"Markus\");");
		value = executeScriptFromString("var str = \"Hallo Markus\"; assert (str.substring(6) == \"Markus\");");
		
	}
}
