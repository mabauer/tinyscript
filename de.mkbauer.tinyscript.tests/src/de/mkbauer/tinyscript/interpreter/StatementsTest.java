package de.mkbauer.tinyscript.interpreter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.xtext.testing.XtextRunner;

import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@RunWith(XtextRunner.class)
public class StatementsTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testAssertStatementTrue() {
		TSValue value = executeScriptFromString("assert (1==1);");
		assertTrue(value.asBoolean());
	}
	
	@Test
	public void testAssertStatementFalse() {
		try {
			TSValue value = executeScriptFromString("assert (1==2);");
			fail("assert (1==2); should raise an exception!");
		}
		catch (TinyscriptAssertationError e) {
			// We have the exception, so the test is successful!
			assertTrue(true);
		}
	}
	
	@Test
	public void testIfStatement() {
		TSValue value = executeScriptFromString("var i=1; if (i==1) { assert(i==1); i=2; assert(i==2);} assert(i==2);");
		assertTrue(value.asBoolean());
	}
	
	@Test
	public void testIfthenElseStatement() {
		TSValue value = executeScriptFromString("var i=1; if (i==2) {i=2;} else {i=3;} assert(i==3);");
		assertTrue(value.asBoolean());
	}
	
	@Test
	public void testIfthenElseStatementWithBooleanConversion() {
		TSValue value = executeScriptFromString("var i=1; if (2) {i=2;} else {i=3;} assert(i==2);");
		assertTrue(value.asBoolean());
		value = executeScriptFromString("var i=1; if (i=2) {i=2;} else {i=3;} assert(i==2);");
		assertTrue(value.asBoolean());
		value = executeScriptFromString("var i=1; if (0) {i=2;} else {i=3;} assert(i==3);");
		assertTrue(value.asBoolean());
		value = executeScriptFromString("var i=1; if (i=0) {i=2;} else {i=3;} assert(i==3);");
		assertTrue(value.asBoolean());		
	}

	@Test
	public void testNestedBlocks() {
		TSValue value = executeScriptFromString("var i; i=1; { i=2; assert (i==2); } assert (i==2);");
	}
	
	@Test
	public void testNestedBlocksShadowing() {
		TSValue value = executeScriptFromString("var i; i=1; { var i; i=2; assert (i==2); } assert (i==1);");
	}
		
	@Test
	public void testSimpleNumericForLoop() {
		TSValue value = executeScriptFromString("var i=0; for (i = 1,42) { var j = i; } assert(i==42);");
	}
	
	@Test
	public void testSimpleNumericForLoopWithVar() {
		TSValue value = executeScriptFromString("var result=0; for (var i = 1, 42) { result = i; } assert(result==42);");
	}
	
	@Test
	public void testNumericForLoopStep() {
		TSValue value = executeScriptFromString("var result=0; for (var i = 42, 0, -2) { result = result + 1; } assert(result==22);");
	}
	
	@Test
	public void testLongNumericForLoop() {
		TSValue value = executeScriptFromString("var i=0, result=0; function inc(x) {return x+1;}; for (i = 1,25000) { result = inc(result); } assert(result==25000);");
		
	}
	
	@Test
	public void testLongNumericForLoopEvenSquares() {
		TSValue value = executeScriptFromString("var result=0; var iterations = 1000; function mult(x,y) { var result = 0; for (var i = 1,y) { result = result + x; } return result; } function square(x) {var temp = mult(x,x); return temp; }; for (var i = 1,iterations) { if (i % 2 == 0) {result = square(i);} else { result = i; } } assert(result==(iterations*iterations));");
		
	}
	
	@Test
	public void testIterableForOnArray() {
		TSValue value = executeScriptFromFile("simple_array_for.ts");
	}
	

}


