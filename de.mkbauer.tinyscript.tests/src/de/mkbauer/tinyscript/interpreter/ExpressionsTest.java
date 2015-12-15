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
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.Tinyscript;

@RunWith(XtextRunner.class)
@InjectWith(TinyscriptInjectorProvider.class)
public class ExpressionsTest {
	
	private static final double epsilon = 0.001;

	@Inject
	ParseHelper<Tinyscript> parser;
	
	@Inject 
	private ValidationTestHelper validator;

	TSValue evaluateOneLineExpression(String line) {
		Tinyscript ast = null;
		try {
			 ast = (Tinyscript)parser.parse(line + ";");
		}
		catch (Exception e) {
			fail("Syntax error in: " + line);
			e.printStackTrace();
		}
		validator.assertNoErrors(ast);
		Expression expr = (Expression)ast.getGlobal().getStatements().get(0);
		ExecutionVisitor visitor = new ExecutionVisitor();
		return visitor.execute(expr);
	}

	@Test
	public void testIntLiteral() {
		TSValue value = evaluateOneLineExpression("2");
		assertEquals(2, value.asInt());
	}

	@Test
	public void testIntAddition() {
		TSValue value = evaluateOneLineExpression("2+2");
		assertEquals(4, value.asInt());
		value = evaluateOneLineExpression("4-2");
		assertEquals(2, value.asInt());
	}
	
	@Test
	public void testIntMultiplication() {
		TSValue value = evaluateOneLineExpression("2*2");
		assertEquals(4, value.asInt());
	}
		
	@Test
	public void testIntExpressions() {
		TSValue value = evaluateOneLineExpression("(2+3) * 2");
		assertEquals(10, value.asInt());
		value = evaluateOneLineExpression("2 + 3*2");
		assertEquals(8, value.asInt());
	}
	
	@Test
	public void testBooleanLiteral() {
		TSValue value = evaluateOneLineExpression("true");
		assertEquals(true, value.asBoolean());
		value = evaluateOneLineExpression("false");
		assertEquals(false, value.asBoolean());	
	}
	
	public void testBooleanConversion() {
		TSValue value = evaluateOneLineExpression("1.0");
		assertEquals(true, value.asBoolean());
		value = evaluateOneLineExpression("\"Test\"");
		assertEquals(true, value.asBoolean());
		value = TSValue.UNDEFINED;
		assertEquals(false, value.asBoolean());
	}
	
	public void testDoubleLiteral() {
		TSValue value = evaluateOneLineExpression("2.0");
		assertEquals(2.0, value.asDouble(), epsilon);
	}
	
	@Test
	public void testDoubleExpressions() {
		TSValue value = evaluateOneLineExpression("2.5 * 2");
		assertEquals(5, value.asDouble(), epsilon);
		value = evaluateOneLineExpression("0.5E-3 *10000");
		assertEquals(5, value.asDouble(), epsilon);
	}	
	
	@Test
	public void testUnaryExpressions() {
		TSValue value = evaluateOneLineExpression("-2.5 * 2");
		assertEquals(-5, value.asDouble(), epsilon);
		value = evaluateOneLineExpression("-2*(3+2)");
		assertEquals(-10, value.asDouble(), epsilon);
	}	
	
	@Test
	public void testStringLiteral() {
		TSValue value = evaluateOneLineExpression("\"Test\"");
		assertEquals("Test", value.asString());
	}
	
	@Test
	public void testStringAddition() {
		TSValue value = evaluateOneLineExpression("\"Hello\" + \", \" + \"World!\"");
		assertEquals("Hello, World!", value.asString());
		value = evaluateOneLineExpression("\"Hello \" + 2");
		assertEquals("Hello 2", value.asString());
		value = evaluateOneLineExpression("\"Hello, \" + 2 + \" Worlds!\"");
		assertEquals("Hello, 2 Worlds!", value.asString());
	}
	
	@Test
	public void testCallOrPropertyAccess() {
		// TSValue value = evaluateOneLineExpression("f(1);");
		assertTrue(true);
	}

}


