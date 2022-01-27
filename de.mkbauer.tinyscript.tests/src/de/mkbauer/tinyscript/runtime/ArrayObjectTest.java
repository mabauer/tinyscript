package de.mkbauer.tinyscript.runtime;

import static org.junit.Assert.fail;

import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@RunWith(XtextRunner.class)
public class ArrayObjectTest extends TinyscriptInterpreterTestHelper {

	@Test
	public void testPush() {
		TSValue value = executeScriptFromString("var arr = [\"Hallo\"]; var l = arr.push(\"Hugo\"); " 
				+ "assert (l==2); assert(\"Hugo\" == arr[1]);");	
	}
	
	@Test
	public void testPop() {
		TSValue value = executeScriptFromString("var arr = [\"Hallo\", \"Hugo\"]; var x = arr.pop(); " 
				+ "assert (1 == arr.length); assert(\"Hugo\" == x);");	
	}
	
	@Test
	public void testUnshift() {
		TSValue value = executeScriptFromString("var arr = [\"Hallo\"]; var l = arr.unshift(\"Hugo\"); " 
				+ "assert (l==2); assert(\"Hugo\" == arr[0]);");	
	}
	
	@Test
	public void testShift() {
		TSValue value = executeScriptFromString("var arr = [\"Hallo\", \"Hugo\"]; var x = arr.shift(); " 
				+ "assert (1 == arr.length); assert(\"Hallo\" == x);");	
	}
	
	@Test
	public void testMap() {
		TSValue value = executeScriptFromString("var arr = [1, 5, 4, 2, 9, 7]; var squares = arr.map(function(x) {return x*x; }); " 
				+ "assert(25 == squares[1]); assert(49 == squares[5]);");
	}
	
	@Test
	public void testMapWithArrowFunction() {
		TSValue value = executeScriptFromString("var arr = [1, 5, 4, 2, 9, 7]; var squares = arr.map(x => x*x); " 
				+ "assert(25 == squares[1]); assert(49 == squares[5]);");
	}
	
	@Test
	public void testMapWithInvalidFunction() {
		TSValue value = executeScriptFromString("var arr = [1, 5, 4, 2, 9, 7]; var empty = arr.map({}); " 
				+ "assert(0 == empty.length);");
	}
	
	@Test
	public void testFilter() {
		TSValue value = executeScriptFromString("var arr = [1, 5, 4, 2, 9, 7]; var odds = arr.filter(function(x) {return (x % 2 == 1); }); " 
				+ "assert(4 == odds.length);");
	}
	
	@Test
	public void testFilterWithArrowFunction() {
		TSValue value = executeScriptFromString("var arr = [1, 5, 4, 2, 9, 7]; var odds = arr.filter(x => x % 2 == 1); " 
				+ "assert(4 == odds.length);");
	}
	
	@Test
	public void testJoin() {
		TSValue value = executeScriptFromString("var arr = [1, 5, 4, 2, 9, 7]; var result = arr.join(\"\"); " 
				+ "assert(\"154297\" == result);");
		value = executeScriptFromString("var arr = [1, 5, 4, 2, 9, 7]; var result = arr.join(\"; \"); " 
				+ "assert(\"1; 5; 4; 2; 9; 7\" == result);");
	}

}
