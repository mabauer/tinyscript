package de.mkbauer.tinyscript.runtime;

import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@ExtendWith(InjectionExtension.class)
public class StringObjectTest extends TinyscriptInterpreterTestHelper {

	@Test
	public void testIndexOf() {
		TSValue value = executeScriptFromString("let str1 = \"Hallo Hugo\", str2 = \"Hugo\"; assert (str1.indexOf(str2) == 6);");	
	}
	
	@Test
	public void testConcat() {
		TSValue value = executeScriptFromString("let str1 = new String(\"Hallo\"), str2 = new String (\"Hugo\"); assert (str1 + \" \" + str2 == \"Hallo Hugo\");");	
	}
	
	@Test
	public void testCharAt() {
		TSValue value = executeScriptFromString("let str = \"Hallo\"; assert (str.charAt(0) == \"H\");");
		value = executeScriptFromString("let str = \"Hallo\"; assert (str.charAt(4) == \"o\");");
		value = executeScriptFromString("let str = \"Hallo\"; assert (str.charAt(-1) == \"\");");
		value = executeScriptFromString("let str = \"Hallo\"; assert (str.charAt(str.length) == \"\");");
		
	}
	
	@Test
	public void testSubstring() {
		TSValue value = executeScriptFromString("let str = \"Hallo Markus\"; assert (str.substring(0, str.length) == \"Hallo Markus\");");
		value = executeScriptFromString("let str = \"Hallo Markus\"; assert (str.substring(5, 0) == \"Hallo\");");
		value = executeScriptFromString("let str = \"Hallo Markus\"; assert (str.substring(-1, 25) == \"Hallo Markus\");");
		value = executeScriptFromString("let str = \"Hallo Markus\"; assert (str.substring(6, str.length) == \"Markus\");");
		value = executeScriptFromString("let str = \"Hallo Markus\"; assert (str.substring(6) == \"Markus\");");
		
	}
}
