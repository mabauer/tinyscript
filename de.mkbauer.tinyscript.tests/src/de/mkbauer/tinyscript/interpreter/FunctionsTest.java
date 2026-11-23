package de.mkbauer.tinyscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.TSStacktraceElement;
import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;
import de.mkbauer.tinyscript.ts.Tinyscript;
import de.mkbauer.tinyscript.ts.TsPackage;


@ExtendWith(InjectionExtension.class)
public class FunctionsTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testSimpleFunctionExpression() {
		executeScriptFromString("let f = function(x) {return x*x;}; assert (f(2)==4);");
		executeScriptFromString("let f = function(x) {return x*x;}; let result = f(2); assert (result==4);");
		executeScriptFromString("let f = function(x) {return x*x;}; let arg = 2; let result = f(arg); assert (result==4);");
		executeScriptFromString("let f = function(x) {return x*x;}; let arg = 2; let result = f(arg+1); assert (result==9);");
	}
	
	@Test
	public void testSimpleAnonymousFunctionExpression() {
		TSValue value = executeScriptFromString("(function(x) { assert(x==2); return x*x;})(2);");
		assertEquals(4, value.asDouble(), 0.0001);
	}
	
	@Test
	public void testSimpleFunctionDeclaration() {
		executeScriptFromString("function f(x) {assert(x==2); return x*x;}; assert (f(2)==4);");
	}

	
	@Test
	public void testFunctionExpressionWithoutArgs() {
		executeScriptFromString("let f = function() {return 4;}; assert (f()==4);");
	}
	
	@Test
	public void testNamedFunctionExpression() {
		executeScriptFromString("let f = function square(x) {return x*x;}; assert (f(2)==4);");
	}

	
	@Test
	public void testFunctionCausingExceptionHasTinyscriptStacktrace() {
		try {
			executeScriptFromString("function f(x) {assert(x!=2); return x*x;}; assert (f(2)==4);");
			fail();
		}
		catch (TinyscriptAssertationError e) {
			TSStacktraceElement[] st = e.getTinyscriptStacktrace();
			assertEquals("f", st[0].getFunctionName());
			assertEquals("global", st[1].getFunctionName());
		}
	}	
	
	@Test
	public void testFunctionCallOnLHSExpression() {
		TSValue value = executeScriptFromString("function f() {return {};} f().key1 = \"Hello\";");
		assertEquals("Hello", value.asString());
		// A call is no valid assignment target: rejected by the validator, and by the interpreter
		Tinyscript ast = parseScriptFromStringWithoutValidating("function f() {return {};} f() = \"Hello\";");
		getValidator().assertError(ast, TsPackage.Literals.BINARY_EXPRESSION, null, "Invalid left-hand side");
		assertThrows(TinyscriptTypeError.class, () -> execute(ast));
	}
	
	@Test
	public void testCallToBuiltInFunction() {
		executeScriptFromString("let x = Math.sqrt(4); assert(x==2);");	
		
	}
	
	@Test
	public void testCallWithTooManyArgs() {
		// The validator rejects f(2, 3, 4) for a declared f; a function as a value (e.g. a
		// callback) ignores extra arguments, as in Javascript
		executeScriptFromString("function f(a, b) { return a*b; }; const g = f; assert(g(2, 3, 4)==6);");
	}
	
	@Test
	public void testCallWithTooFewArgs() {
		TSValue result = executeScriptFromString("function f(a, b) { return b; }; f(2);");	
		assertEquals(TSValue.UNDEFINED, result);
	}
	
	@Test
	public void testFunctionWithRestParameters() {
		executeScriptFromString("function f(...rest) { return rest; }; let result = f(1, 2, 3); "
				+ "assert(result[0] == 1);"
				+ "assert(result[1] == 2);"
				+ "assert(result[2] == 3);");	
		executeScriptFromString("function f(a, b, ...rest) { return rest; }; let result = f(1, 2, 3, 4); "
				+ "assert(result[0] == 3);"
				+ "assert(result[1] == 4);");	
	}

}

