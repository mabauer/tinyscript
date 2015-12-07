package de.mkbauer.tinyscript.interpreter

import com.google.inject.Inject
import de.mkbauer.tinyscript.TinyscriptInjectorProvider
import de.mkbauer.tinyscript.ts.AssignmentExpression
import de.mkbauer.tinyscript.ts.Tinyscript
import static org.junit.Assert.*
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.junit4.util.ParseHelper
import org.junit.Test
import org.junit.runner.RunWith

@InjectWith(TinyscriptInjectorProvider)
@RunWith(XtextRunner)
class ExpressionsTest {

	@Inject
	ParseHelper<Tinyscript> parser

	def TSValue evaluateOneLineExpression(String line) {
		val ast = parser.parse(line) as Tinyscript
		val interpreter = new TinyscriptInterpreter(ast)
		val expr = ast.elements.statements.head as AssignmentExpression
		return interpreter.evaluate(expr)
	}

	@Test
	def void testIntLiteral() {
		val TSValue value = evaluateOneLineExpression("2")
		assertEquals(2, value.asInt())
	}

	@Test
	def void testIntAddition() {
		var TSValue value = evaluateOneLineExpression("2+2")
		assertEquals(4, value.asInt())
		value = evaluateOneLineExpression("4-2")
		assertEquals(2, value.asInt())
	}
	
	@Test
	def void testIntMultiplication() {
		var TSValue value = evaluateOneLineExpression("2*2")
		assertEquals(4, value.asInt())
	}
		
	@Test
	def void testIntExpressions() {
		var value = evaluateOneLineExpression("(2+3) * 2")
		assertEquals(10, value.asInt())
		value = evaluateOneLineExpression("2 + 3*2")
		assertEquals(8, value.asInt())
	}
	
	def void testDoubleLiteral() {
		val TSValue value = evaluateOneLineExpression("2.0")
		assertEquals(2.0, value.asDouble())
	}
	
	@Test
	def void testDoubleExpressions() {
		var value = evaluateOneLineExpression("2.5 * 2")
		assertEquals(5, value.asDouble(), 0.001)
		value = evaluateOneLineExpression("0.5E-3 *10000")
		assertEquals(5, value.asDouble(), 0.001)
	}	
	
	@Test
	def void testUnaryExpressions() {
		var value = evaluateOneLineExpression("-2.5 * 2")
		assertEquals(-5, value.asDouble(), 0.001)
		value = evaluateOneLineExpression("-2*(3+2)")
		assertEquals(-10, value.asDouble(), 0.001)
	}	
	
	@Test
	def void testStringLiteral() {
		val TSValue value = evaluateOneLineExpression("\"Test\"")
		assertEquals("Test", value.asString())
	}
	
	@Test
	def void testStringAddition() {
		var TSValue value = evaluateOneLineExpression("\"Hello\" + \", \" + \"World!\"")
		assertEquals("Hello, World!", value.asString())
		value = evaluateOneLineExpression("\"Hello \" + 2")
		assertEquals("Hello 2", value.asString())
		value = evaluateOneLineExpression("\"Hello, \" + 2 + \" Worlds!\"")
		assertEquals("Hello, 2 Worlds!", value.asString())
	}

}
