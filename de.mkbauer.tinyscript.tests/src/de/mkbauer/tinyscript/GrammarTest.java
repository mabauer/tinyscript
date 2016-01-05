package de.mkbauer.tinyscript;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.junit4.util.ParseHelper;
import org.eclipse.xtext.junit4.validation.ValidationTestHelper;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.mkbauer.tinyscript.TinyscriptInjectorProvider;
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
