package de.mkbauer.tinyscript.runtime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@ExtendWith(InjectionExtension.class)
public class ArrayObjectTest extends TinyscriptInterpreterTestHelper {

	@Test
	public void testPush() {
		TSValue value = executeScriptFromString("let arr = [\"Hallo\"]; let l = arr.push(\"Hugo\"); " 
				+ "assert (l==2); assert(\"Hugo\" == arr[1]);");	
	}
	
	@Test
	public void testPop() {
		TSValue value = executeScriptFromString("let arr = [\"Hallo\", \"Hugo\"]; let x = arr.pop(); " 
				+ "assert (1 == arr.length); assert(\"Hugo\" == x);");	
	}
	
	@Test
	public void testUnshift() {
		TSValue value = executeScriptFromString("let arr = [\"Hallo\"]; let l = arr.unshift(\"Hugo\"); " 
				+ "assert (l==2); assert(\"Hugo\" == arr[0]);");	
	}
	
	@Test
	public void testShift() {
		TSValue value = executeScriptFromString("let arr = [\"Hallo\", \"Hugo\"]; let x = arr.shift(); " 
				+ "assert (1 == arr.length); assert(\"Hallo\" == x);");	
	}
	
	@Test
	public void testMap() {
		TSValue value = executeScriptFromString("let arr = [1, 5, 4, 2, 9, 7]; let squares = arr.map(function(x) {return x*x; }); " 
				+ "assert(25 == squares[1]); assert(49 == squares[5]);");
	}
	
	@Test
	public void testMapWithArrowFunction() {
		TSValue value = executeScriptFromString("let arr = [1, 5, 4, 2, 9, 7]; let squares = arr.map(x => x*x); " 
				+ "assert(25 == squares[1]); assert(49 == squares[5]);");
	}
	
	@Test
	public void testMapWithInvalidFunction() {
		assertThrows(TinyscriptTypeError.class, () -> executeScriptFromString("let arr = [1, 5, 4, 2, 9, 7]; arr.map({});"));
	}
	
	@Test
	public void testFilter() {
		TSValue value = executeScriptFromString("let arr = [1, 5, 4, 2, 9, 7]; let odds = arr.filter(function(x) {return (x % 2 == 1); }); " 
				+ "assert(4 == odds.length);");
	}
	
	@Test
	public void testFilterWithArrowFunction() {
		TSValue value = executeScriptFromString("let arr = [1, 5, 4, 2, 9, 7]; let odds = arr.filter(x => x % 2 == 1); " 
				+ "assert(4 == odds.length);");
	}
	
	@Test
	public void testJoin() {
		TSValue value = executeScriptFromString("let arr = [1, 5, 4, 2, 9, 7]; let result = arr.join(\"\"); " 
				+ "assert(\"154297\" == result);");
		value = executeScriptFromString("let arr = [1, 5, 4, 2, 9, 7]; let result = arr.join(\"; \"); " 
				+ "assert(\"1; 5; 4; 2; 9; 7\" == result);");
	}

}
