package de.mkbauer.tinyscript.interpreter;

import static org.junit.Assert.assertEquals;

import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@RunWith(XtextRunner.class)
public class ExpressionsTest extends TinyscriptInterpreterTestHelper {
	


	@Test
	public void testIntLiteral() {
		TSValue value = evaluateSimpleExpression("2");
		assertEquals(2, value.asInt());
	}

	@Test
	public void testIntAddition() {
		TSValue value = evaluateSimpleExpression("2+2");
		assertEquals(4, value.asInt());
		value = evaluateSimpleExpression("4-2");
		assertEquals(2, value.asInt());
	}
	
	@Test
	public void testIntMultiplication() {
		TSValue value = evaluateSimpleExpression("2*2");
		assertEquals(4, value.asInt());
	}
		
	@Test
	public void testIntExpressions() {
		TSValue value = evaluateSimpleExpression("(2+3) * 2");
		assertEquals(10, value.asInt());
		value = evaluateSimpleExpression("2 + 3*2");
		assertEquals(8, value.asInt());
	}
	
	@Test
	public void testModulo() {
		TSValue value = evaluateSimpleExpression("8 % 2");
		assertEquals(0, value.asInt());
		value = evaluateSimpleExpression("9 % 2");
		assertEquals(1, value.asInt());
	}
	
	@Test
	public void testBooleanLiteral() {
		TSValue value = evaluateSimpleExpression("true");
		assertEquals(true, value.asBoolean());
		value = evaluateSimpleExpression("false");
		assertEquals(false, value.asBoolean());	
	}
	
	public void testBooleanConversion() {
		TSValue value = evaluateSimpleExpression("1.0");
		assertEquals(true, value.asBoolean());
		value = evaluateSimpleExpression("\"Test\"");
		assertEquals(true, value.asBoolean());
		value = TSValue.UNDEFINED;
		assertEquals(false, value.asBoolean());
	}
	
	public void testDoubleLiteral() {
		TSValue value = evaluateSimpleExpression("2.0");
		assertEquals(2.0, value.asDouble(), epsilon);
	}
	
	@Test
	public void testDoubleExpressions() {
		TSValue value = evaluateSimpleExpression("2.5 * 2");
		assertEquals(5, value.asDouble(), epsilon);
		value = evaluateSimpleExpression("0.5E-3 *10000");
		assertEquals(5, value.asDouble(), epsilon);
	}	
	
	@Test
	public void testUnaryExpressions() {
		TSValue value = evaluateSimpleExpression("-2.5 * 2");
		assertEquals(-5, value.asDouble(), epsilon);
		value = evaluateSimpleExpression("-2*(3+2)");
		assertEquals(-10, value.asDouble(), epsilon);
	}	
	
	@Test
	public void testStringLiteral() {
		TSValue value = evaluateSimpleExpression("\"Test\"");
		assertEquals("Test", value.asString());
	}
	
	@Test
	public void testStringAddition() {
		TSValue value = evaluateSimpleExpression("\"Hello\" + \", \" + \"World!\"");
		assertEquals("Hello, World!", value.asString());
		value = evaluateSimpleExpression("\"Hello \" + 2");
		assertEquals("Hello 2", value.asString());
		value = evaluateSimpleExpression("\"Hello, \" + 2 + \" Worlds!\"");
		assertEquals("Hello, 2 Worlds!", value.asString());
	}
	
	@Test
	public void testArrayAddition() {
		executeScriptFromString("var a1 = [ \"Hello\", \"Dear\"], a2 = [ \"Markus\", \"Bauer\" ]; var result = a1 + a2; assert (result[0]==\"Hello\"); assert (result[3]==\"Bauer\");");
		executeScriptFromString("var s = \"Hello\"; var a = [ \"Markus\", \"Bauer\" ]; var result = s + a; assert (result[0]==\"Hello\"); assert (result[2]==\"Bauer\");");
		executeScriptFromString("var s = \"Hello\"; var a = [ \"Markus\", \"Bauer\" ]; ; var result = a + s; assert (result[0]==\"Markus\"); assert (result[2]==\"Hello\");");
	}

}


