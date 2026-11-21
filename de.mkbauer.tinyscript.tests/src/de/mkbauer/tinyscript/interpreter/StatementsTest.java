package de.mkbauer.tinyscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.eclipse.xtext.testing.extensions.InjectionExtension;

import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;

@ExtendWith(InjectionExtension.class)
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
	
	// The validator rejects this outside a function; the engine is safe without it, too:
	// this is undefined there, so the global object with the builtins is not reachable
	@Test
	public void testThisIsUndefinedOutsideAFunction() throws Exception {
		assertEquals(TSValue.UNDEFINED, execute(getParser().parse("this;")));
		assertEquals(TSValue.UNDEFINED, execute(getParser().parse("let f = () => this; f();")));
		assertEquals("ok", execute(getParser().parse("let o = { m: function() { return this; }, s: \"ok\" }; o.m().s;")).asString());
	}

	@Test
	public void testIfStatement() {
		TSValue value = executeScriptFromString("let i=1; if (i==1) { assert(i==1); i=2; assert(i==2);} assert(i==2);");
		assertTrue(value.asBoolean());
	}
	
	@Test
	public void testIfthenElseStatement() {
		TSValue value = executeScriptFromString("let i=1; if (i==2) {i=2;} else {i=3;} assert(i==3);");
		assertTrue(value.asBoolean());
	}
	
	@Test
	public void testIfConditionMustBeABooleanNullOrAnObject() {
		// Numbers are no conditions; this also catches the typo "=" for "=="
		assertThrows(TinyscriptTypeError.class, () -> executeScriptFromString("let i=1; if (2) {i=2;} else {i=3;}"));
		assertThrows(TinyscriptTypeError.class, () -> executeScriptFromString("let i=1; if (i=0) {i=2;} else {i=3;}"));
		TSValue value = executeScriptFromString("let i=1; let o={}; if (o) {i=2;} else {i=3;} assert(i==2);");
		assertTrue(value.asBoolean());
		value = executeScriptFromString("let i=1; if (null) {i=2;} else {i=3;} assert(i==3);");
		assertTrue(value.asBoolean());
	}

	@Test
	public void testNestedBlocks() {
		TSValue value = executeScriptFromString("let i; i=1; { i=2; assert (i==2); } assert (i==2);");
	}
	
	@Test
	public void testNestedBlocksShadowing() {
		TSValue value = executeScriptFromString("let i; i=1; { let i; i=2; assert (i==2); } assert (i==1);");
	}
		
	@Test
	public void testSimpleNumericForLoop() {
		TSValue value = executeScriptFromString("let i=0; for (i : range(1, 43)) { let j = i; } assert(i==42);");
	}
	
	@Test
	public void testSimpleNumericForLoopWithVar() {
		TSValue value = executeScriptFromString("let result=0; for (const i : range(1, 43)) { result = i; } assert(result==42);");
	}
	
	@Test
	public void testNumericForLoopStep() {
		TSValue value = executeScriptFromString("let result=0; for (const i : range(42, -1, -2)) { result = result + 1; } assert(result==22);");
	}
	
	@Test
	public void testLongNumericForLoop() {
		TSValue value = executeScriptFromString("let i=0, result=0; function inc(x) {return x+1;}; for (i : range(1, 25001)) { result = inc(result); } assert(result==25000);");
		
	}
	
	@Test
	public void testLongNumericForLoopEvenSquares() {
		TSValue value = executeScriptFromString("let result=0; let iterations = 1000; function mult(x,y) { let result = 0; for (const i : range(1, y + 1)) { result = result + x; } return result; } function square(x) {let temp = mult(x,x); return temp; }; for (const i : range(1, iterations + 1)) { if (i % 2 == 0) {result = square(i);} else { result = i; } } assert(result==(iterations*iterations));");
		
	}
	
	@Test
	public void testIterableForOnArray() {
		TSValue value = executeScriptFromFile("simple_array_for.tys");
	}
	

}


