package de.mkbauer.tinyscript.runtime;

import org.eclipse.xtext.junit4.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.TinyscriptInterpreterTestHelper;
import de.mkbauer.tinyscript.interpreter.TSValue;

@RunWith(XtextRunner.class)
public class StringObjectTests extends TinyscriptInterpreterTestHelper {

	@Test
	public void testIndexOf() {
		TSValue value = executeScriptFromString("var str1 = \"Hallo Hugo\", str2 = \"Hugo\"; assert (str1.indexOf(str2) == 6);");	
	}
	
	@Test
	public void testConcat() {
		TSValue value = executeScriptFromString("var str1 = new String(\"Hallo\"), str2 = new String (\"Hugo\"); assert (str1 + str2 == \"Hallo Hugo\");");	
	}
}
