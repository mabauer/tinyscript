package de.mkbauer.tinyscript.tests;

import static org.junit.Assert.assertEquals;
import java.util.List;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.validation.Issue;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.ObjectInitializer;
import de.mkbauer.tinyscript.ts.Tinyscript;

@RunWith(XtextRunner.class)
public class GrammarTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testHelloWorld() {
		parseScriptFromString("var hello = \"Hello, World!\";");
	}
	
	@Test
	public void testNestedBlock() {
		Tinyscript ast = parseScriptFromString("var hello = \"Hello\"; {var world = \"World\"; }");
		assertEquals(EcoreUtil2.eAllOfType(ast, Block.class).size(), 2);
	}
	
	@Test
	public void testObjectInitializer() {
		Tinyscript ast = parseScriptFromString("var anObject = {};");
		assertEquals(EcoreUtil2.eAllOfType(ast, ObjectInitializer.class).size(), 1);
		ast = parseScriptFromString("var anObject = {hello : \"Hello\", world: \"World\"};");
		assertEquals(EcoreUtil2.eAllOfType(ast, ObjectInitializer.class).size(), 1);
	}
	
	@Test
	public void testReturnStatements() {
		Tinyscript ast = parseScriptFromStringWithoutValidating("{ var x = 4; return x; }");
		List <Issue> issues = getValidator().validate(ast);
		assertEquals(1, issues.size());
		
		ast = parseScriptFromStringWithoutValidating("function f() { var x = 4; return x; }");
		getValidator().assertNoErrors(ast);
	}
	
	@Test
	public void testExpressionStatements() {
		parseScriptFromFile("expressionstatements.ts");
	}
	
	@Test
	public void testFunctionExpression() {
		parseScriptFromFile("function_expression.ts");
	}
	
	@Test
	public void testCounterWithClosures() {
		parseScriptFromFile("counter_with_closures.ts");
	}
	
	@Test
	public void testModulesViaClosures() {
		parseScriptFromFile("modules_via_closures.ts");
	}
	
	@Test
	public void testTrigger_example() {
		parseScriptFromFile("trigger_example.ts");
	}
	

}
