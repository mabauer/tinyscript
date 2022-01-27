package de.mkbauer.tinyscript.generator

import org.junit.runner.RunWith

import org.junit.Test
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import com.google.inject.Inject
import de.mkbauer.tinyscript.tests.TinyscriptInjectorProvider

import java.util.regex.Pattern
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner

@InjectWith(TinyscriptInjectorProvider)
@RunWith(XtextRunner)
class GeneratorTest extends TinyscriptInterpreterTestHelper {
	
	@Inject 
	TinyscriptGenerator generator
	
	def String generateScriptFromString(String script) {
		val ast = parseScriptFromString(script)
		generator.generate(ast, false).toString;
	}
	
	@Test
	def void testGenerateHelloWorld() {
		val result = generateScriptFromString('''
			var hello;
			hello = "Hello World!"; 
			assert (hello == "Hello World!");
		''')
		assertEquals('''
			var hello;
			hello = "Hello World!";
			assert (hello == "Hello World!");
		'''.toString, result)
		
	}
	
	@Test
	def void testgenerateLiteralExpressions() {
		var result = generateScriptFromString("3;")
		assertEquals("3;", result.trim())
		result = generateScriptFromString("2.534;")
		assertEquals("2.534;", result.trim())
		result = generateScriptFromString("\"Hello\";")
		assertEquals("\"Hello\";", result.trim())
		result = generateScriptFromString('''{key1: "Markus", key2: "Bauer"};''');
		assertEquals('''{key1: "Markus", key2: "Bauer"};'''.toString, result.trim())
		result = generateScriptFromString('''["Markus", "Bauer", 3];''');
		assertEquals('''["Markus", "Bauer", 3];'''.toString, result.trim())
	}
	
	
	@Test
	def void testgenerateBinaryExpressions() {
		var result = generateScriptFromString("3*4;")
		assertEquals("3 * 4;", result.trim())
		result = generateScriptFromString("(3+4)*5;")
		assertEquals("(__ts_add(3, 4)) * 5;", result.trim())
		result = generateScriptFromString("3 == 4;")
		assertEquals("3 == 4;", result.trim())
		result = generateScriptFromString("\"Hello\" + \" World\";")
		assertEquals("__ts_add(\"Hello\", \" World\");", result.trim())
	}
	
		@Test
	def void testgenerateUnaryExpressions() {
		var result = generateScriptFromString("-4;")
		assertEquals("-4;", result.trim())
		result = generateScriptFromString('''
			var x = 2.25;
			var y = -x;''')
		assertEquals('''
			var x = 2.25;
			var y = -x;'''.toString, 
			result.trim())
		result = generateScriptFromString("!false;")
		assertEquals("!false;", result.trim())
		result = generateScriptFromString('''
			var x = (3 == 4);
			var true_ = !x;''')
		assertEquals('''
			var x = (3 == 4);
			var true_ = !x;'''.toString, 
			result.trim())
	}
	
	@Test
	def void testgenerateCallOrPropertyAccesses() {
		val result = generateScriptFromString('''
			var obj;
			obj.key1;
			obj.key1.key2;
			obj["key"];
			obj();
			obj.method();
		''')
		assertEquals('''
			var obj;
			obj.key1;
			obj.key1.key2;
			obj["key"];
			obj();
			obj.method();
		'''.toString, result)
	}
	
		@Test
	def void testgenerateFunctionDeclaration() {
		val result = generateScriptFromString('''
			function f(p1, p2) {
				return p1 * p2;
			}		
		''')
		assertEquals('''
			function f(p1, p2) {
				return p1 * p2;
			}
			
		'''.toString, result)	
	}
	
	@Test
	def void testgenerateFunctionExpression() {
		val result = generateScriptFromString('''
			var f;
			f = function (p1, p2) {
				return p1 * p2;
			};
		''')
		assertEquals('''
			var f;
			f = function (p1, p2) {
				return p1 * p2;
			};
		'''.toString, result)	
	}
	
		@Test
	def void testgenerateNamedFunctionExpression() {
		val result = generateScriptFromString('''
			var f;
			f = function name(p1, p2) {
				return p1 * p2;
			};
		''')
		assertEquals('''
			var f;
			f = function name(p1, p2) {
				return p1 * p2;
			};
		'''.toString, result)	
	}
	
	
	@Test
	def void testgenerateIfThenElse() {
		val result = generateScriptFromString('''
			var x, y, max;
			if (x >= y) {
				max = x;
			}
			else {
				max = y;
			}
		''')
		assertEquals('''
			var x, y, max;
			if (x >= y) {
				max = x;
			}
			else {
				max = y;
			}
		'''.toString, result)	
	}
	
	@Test
	def void testGenerateNumericFor() {
		var result = generateScriptFromString('''
			var result;
			for (var i = 1, 10) {
				result = i;
			}
		''')
		assertEquals('''
			var result;
			for (var i = 1; i <= 10; i = i + 1) {
				result = i;
			}
		'''.toString, result)
		result = generateScriptFromString('''
			var result;
			for (var i = 10, 1, -1) {
				result = i;
			}
		''')
		assertEquals('''
			var result;
			for (var i = 10; (-1 > 0)?(i <= 1):(i >= 1); i = i + -1) {
				result = i;
			}
		'''.toString, result)
	}	
	
	@Test
	def void testGenerateNestedBlock() {
		val result = generateScriptFromString('''
			var i;
			{
				var i;
				i = 0;
			}
		''')
		assertTrue(matches('''
			var i;
			{
				var block\d*_i;
				block\d*_i = 0.0;
			}
		'''.toString, result))
		
	}
	
	@Test
	def void testGenerateNumericForWithExistingVariable() {
		var result = generateScriptFromString('''
			var result;
			var i;
			for (i = 1, 10) {
				result = i;
			}
		''')
		assertEquals('''
			var result;
			var i;
			for (i = 1; i <= 10; i = i + 1) {
				result = i;
			}
		'''.toString, result)	
	}
	
	def boolean matches(String expected, String result) {
		// Ignore all regex special characters. CAVEAT: * and . are NOT quoted.
		var String pattern = Pattern.quote(result);
		return Pattern.matches(pattern, result);		
	}
}