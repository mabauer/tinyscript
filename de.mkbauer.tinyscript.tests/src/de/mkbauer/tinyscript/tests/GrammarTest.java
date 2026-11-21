package de.mkbauer.tinyscript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.validation.Issue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.ObjectInitializer;
import de.mkbauer.tinyscript.ts.Tinyscript;

@ExtendWith(InjectionExtension.class)
public class GrammarTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testHelloWorld() {
		parseScriptFromString("let hello = \"Hello, World!\";");
	}
	
	@Test
	public void testNestedBlock() {
		Tinyscript ast = parseScriptFromString("let hello = \"Hello\"; {let world = \"World\"; }");
		assertEquals(EcoreUtil2.eAllOfType(ast, Block.class).size(), 2);
	}
	
	@Test
	public void testObjectInitializer() {
		Tinyscript ast = parseScriptFromString("let anObject = {};");
		assertEquals(EcoreUtil2.eAllOfType(ast, ObjectInitializer.class).size(), 1);
		ast = parseScriptFromString("let anObject = {hello : \"Hello\", world: \"World\"};");
		assertEquals(EcoreUtil2.eAllOfType(ast, ObjectInitializer.class).size(), 1);
	}
	
	@Test
	public void testReturnStatements() {
		Tinyscript ast = parseScriptFromStringWithoutValidating("{ let x = 4; return x; }");
		List <Issue> issues = getValidator().validate(ast);
		assertEquals(1, issues.size());
		
		ast = parseScriptFromStringWithoutValidating("function f() { let x = 4; return x; }");
		getValidator().assertNoErrors(ast);
	}
	
	@Test
	public void testExpressionStatements() {
		parseScriptFromFile("expressionstatements.tys");
	}
	
	@Test
	public void testFunctionExpression() {
		parseScriptFromFile("function_expression.tys");
	}
	
	@Test
	public void testCounterWithClosures() {
		parseScriptFromFile("counter_with_closures.tys");
	}
	
	@Test
	public void testModulesViaClosures() {
		parseScriptFromFile("modules_via_closures.tys");
	}
	
	@Test
	public void testTrigger_example() {
		parseScriptFromFile("trigger_example.tys");
	}
	

}
