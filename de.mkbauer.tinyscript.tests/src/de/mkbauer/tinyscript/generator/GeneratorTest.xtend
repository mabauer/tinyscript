package de.mkbauer.tinyscript.generator

import org.junit.jupiter.api.^extension.ExtendWith

import org.junit.jupiter.api.Test
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import com.google.inject.Inject
import de.mkbauer.tinyscript.tests.TinyscriptInjectorProvider

import java.util.regex.Pattern
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension

@InjectWith(TinyscriptInjectorProvider)
@ExtendWith(InjectionExtension)
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
			let hello;
			hello = "Hello World!"; 
			assert (hello == "Hello World!");
		''')
		assertEquals('''
			let hello;
			hello = "Hello World!";
			assert((__ts_eq(hello, "Hello World!")));
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
		assertEquals("__ts_mul((__ts_add(3, 4)), 5);", result.trim())
		result = generateScriptFromString("3 == 4;")
		assertEquals("__ts_eq(3, 4);", result.trim())
		result = generateScriptFromString("\"Hello\" + \" World\";")
		assertEquals("__ts_add(\"Hello\", \" World\");", result.trim())
	}
	
		@Test
	def void testgenerateUnaryExpressions() {
		var result = generateScriptFromString("-4;")
		assertEquals("-4;", result.trim())
		result = generateScriptFromString('''
			let x = 2.25;
			let y = -x;''')
		assertEquals('''
			let x = 2.25;
			let y = __ts_neg(x);'''.toString, 
			result.trim())
		result = generateScriptFromString("!false;")
		assertEquals("!false;", result.trim())
		result = generateScriptFromString('''
			let x = (3 == 4);
			let true_ = !x;''')
		assertEquals('''
			let x = (__ts_eq(3, 4));
			let true_ = !__ts_cond(x);'''.toString, 
			result.trim())
	}
	
	@Test
	def void testgenerateCallOrPropertyAccesses() {
		val result = generateScriptFromString('''
			let obj;
			obj.key1;
			obj.key1.key2;
			obj["key"];
			obj();
			obj.method();
		''')
		// __ts_r checks the receiver (a number or boolean has no properties) and returns it
		assertEquals('''
			let obj;
			__ts_r(obj, "key1").key1;
			__ts_r(__ts_r(obj, "key1").key1, "key2").key2;
			__ts_r(obj, "key")["key"];
			obj();
			__ts_r(obj, "method").method();
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
				return __ts_mul(p1, p2);
			}
			
		'''.toString, result)	
	}
	
	@Test
	def void testgenerateFunctionExpression() {
		val result = generateScriptFromString('''
			let f;
			f = function (p1, p2) {
				return p1 * p2;
			};
		''')
		assertEquals('''
			let f;
			f = function (p1, p2) {
				return __ts_mul(p1, p2);
			};
		'''.toString, result)	
	}
	
		@Test
	def void testgenerateNamedFunctionExpression() {
		val result = generateScriptFromString('''
			let f;
			f = function name(p1, p2) {
				return p1 * p2;
			};
		''')
		assertEquals('''
			let f;
			f = function name(p1, p2) {
				return __ts_mul(p1, p2);
			};
		'''.toString, result)	
	}
	
	
	@Test
	def void testgenerateIfThenElse() {
		val result = generateScriptFromString('''
			let x, y, max;
			if (x >= y) {
				max = x;
			}
			else {
				max = y;
			}
		''')
		assertEquals('''
			let x, y, max;
			if (__ts_ge(x, y)) {
				max = x;
			}
			else {
				max = y;
			}
		'''.toString, result)	
	}
		
	
	@Test
	def void testGenerateNestedBlock() {
		val result = generateScriptFromString('''
			let i;
			{
				let i;
				i = 0;
			}
		''')
		assertTrue(matches('''
			let i;
			{
				let block\d*_i;
				block\d*_i = 0.0;
			}
		'''.toString, result))
		
	}
	
	
	def boolean matches(String expected, String result) {
		// Ignore all regex special characters. CAVEAT: * and . are NOT quoted.
		var String pattern = Pattern.quote(result);
		return Pattern.matches(pattern, result);		
	}
}