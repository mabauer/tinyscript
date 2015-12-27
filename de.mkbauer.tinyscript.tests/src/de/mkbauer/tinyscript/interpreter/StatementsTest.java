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
public class StatementsTest {
	
	private static final double epsilon = 0.001;

	@Inject
	ParseHelper<Tinyscript> parser;
	
	@Inject 
	private ValidationTestHelper validator;

	TSValue executeOneLineScript(String line) {
		Tinyscript ast = null;
		try {
			 ast = (Tinyscript)parser.parse(line);
			 
		}
		catch (Exception e) {
			fail("Syntax error in: " + line);
			e.printStackTrace();
		}
		validator.assertNoErrors(ast);
		ExecutionVisitor visitor = new ExecutionVisitor();
		return visitor.execute(ast);
	}
	
	@Test
	public void testAssertStatementTrue() {
		TSValue value = executeOneLineScript("assert (1==1);");
		assertTrue(value.asBoolean());
	}
	
	@Test
	public void testAssertStatementFalse() {
		try {
			TSValue value = executeOneLineScript("assert (1==2);");
			fail("assert (1==2); should raise an exception!");
		}
		catch (TinyscriptAssertationError e) {
			// We have the exception, so the test is successfful!
			assertTrue(true);
		}
	}
	
	@Test
	public void testIfStatement() {
		TSValue value = executeOneLineScript("var i=1; if (i==1) { assert(i==1); i=2; assert(i==2);} assert(i==2);");
		assertTrue(value.asBoolean());
	}
	
	@Test
	public void testIfthenElseStatement() {
		TSValue value = executeOneLineScript("var i=1; if (i==2) {i=2;} else {i=3;} assert(i==3);");
		assertTrue(value.asBoolean());
	}

	@Test
	public void testNestedBlocks() {
		TSValue value = executeOneLineScript("var i; i=1; { i=2; assert (i==2); } assert (i==2);");
	}
	
	@Test
	public void testNestedBlocksShadowing() {
		TSValue value = executeOneLineScript("var i; i=1; { var i; i=2; assert (i==2); } assert (i==1);");
	}
		
	@Test
	public void testSimpleNumericForLoop() {
		TSValue value = executeOneLineScript("var i=0; for (i = 1,42) { var j = i; } assert(i==42);");
	}
	
	@Test
	public void testSimpleNumericForLoopWithVar() {
		TSValue value = executeOneLineScript("var result=0; for (var i = 1, 42) { result = i; } assert(result==42);");
	}
	
	@Test
	public void testNumericForLoopStep() {
		TSValue value = executeOneLineScript("var result=0; for (var i = 42, 0, -2) { result = result + 1; } assert(result==22);");
	}
	
	@Test
	public void testLongNumericForLoop() {
		TSValue value = executeOneLineScript("var i=0, result=0; function inc(x) {return x+1;}; for (i = 1,25000) { result = inc(result); } assert(result==25000);");
		
	}
	
	@Test
	public void testLongNumericForLoopEvenSquares() {
		TSValue value = executeOneLineScript("var result=0; var iterations = 1000; function mult(x,y) { var result = 0; for (var i = 1,y) { result = result + x; } return result; } function square(x) {var temp = mult(x,x); return temp; }; for (var i = 1,iterations) { if (i % 2 == 0) {result = square(i);} else { result = i; } } assert(result==(iterations*iterations));");
		
	}
	

}


