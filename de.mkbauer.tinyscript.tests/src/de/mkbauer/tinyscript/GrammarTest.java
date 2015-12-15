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
@InjectWith(TinyscriptInjectorProvider.class)
public class GrammarTest {
	
	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;
	
	@Inject
	private ParseHelper parser;
	
	@Inject 
	private ValidationTestHelper validator;

	void parseFromFile(String filename) {
		Tinyscript ast = null;
		try {
			URI uri = URI.createURI(filename);
			Resource resource = resourceSetProvider.get().createResource(uri);
			resource.load(new HashMap());
			ast = (Tinyscript) resource.getContents().get(0);
			validator.assertNoErrors(ast);
		}
		catch (IOException e) {
			fail("Script " + filename + " not found.");
		}
	}
	
	Tinyscript parseFromString(String script) {
		Tinyscript ast = null;
		try {
			ast = (Tinyscript)parser.parse(script);
			validator.assertNoErrors(ast);
		}
		catch (Exception e) {
			fail("Parser error");
		}
		return ast;
	}
	
	@Test
	public void testHelloWorld() {
		parseFromString("var hello = \"Hello, World!\";");
	}
	
	@Test
	public void testNestedBlock() {
		Tinyscript ast = parseFromString("var hello = \"Hello\"; {var world = \"World\"; }");
		assertEquals(EcoreUtil2.eAllOfType(ast, Block.class).size(), 2);
	}
	
	@Test
	public void testObjectInitializer() {
		Tinyscript ast = parseFromString("var anObject = {};");
		assertEquals(EcoreUtil2.eAllOfType(ast, ObjectInitializer.class).size(), 1);
		ast = parseFromString("var anObject = {hello : \"Hello\", world: \"World\"};");
		assertEquals(EcoreUtil2.eAllOfType(ast, ObjectInitializer.class).size(), 1);
	}
	
	@Test
	public void testExpressionStatements() {
		parseFromFile("expressionstatements.ts");
	}
	
	@Test
	public void testFunctionExpression() {
		parseFromFile("function_expression.ts");
	}
	
	@Test
	public void testCounterWithClosures() {
		parseFromFile("counter_with_closures.ts");
	}
	
	@Test
	public void testModulesViaClosures() {
		parseFromFile("modules_via_closures.ts");
	}
	
	@Test
	public void testTrigger_example() {
		parseFromFile("trigger_example.ts");
	}
	

}
