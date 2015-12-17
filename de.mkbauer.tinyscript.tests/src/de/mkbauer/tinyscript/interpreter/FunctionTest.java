package de.mkbauer.tinyscript.interpreter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
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
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.Tinyscript;

@RunWith(XtextRunner.class)
@InjectWith(TinyscriptInjectorProvider.class)
public class FunctionTest {
	
	private static final double epsilon = 0.001;

	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;
	
	@Inject
	ParseHelper<Tinyscript> parser;
	
	@Inject 
	private ValidationTestHelper validator;
	
	TSValue executeScriptFromFile(String filename) {
		Tinyscript ast = null;
		try {
			URI uri = URI.createURI(filename);
			Resource resource = resourceSetProvider.get().createResource(uri);
			resource.load(new HashMap());
			ast = (Tinyscript) resource.getContents().get(0);
		}
		catch (IOException e) {
			fail("Script " + filename + " not found.");
		}
		validator.assertNoErrors(ast);
		ExecutionVisitor visitor = new ExecutionVisitor();
		return visitor.execute(ast); 
	}

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
	public void testSimpleFunctionExpression() {
		TSValue value = executeOneLineScript("var f = function(x) {return x*x;}; assert (f(2)==4);");
		value = executeOneLineScript("var f = function(x) {return x*x;}; var result = f(2); assert (result==4);");
		value = executeOneLineScript("var f = function(x) {return x*x;}; var arg = 2; var result = f(arg); assert (result==4);");
		value = executeOneLineScript("var f = function(x) {return x*x;}; var arg = 2; var result = f(arg+1); assert (result==9);");
	}
	
	@Test
	public void testSimpleAnonymousFunctionExpression() {
		TSValue value = executeOneLineScript("(function(x) {return x*x;})(2);");
		assertEquals(4, value.asDouble(), 0.0001);
	}
	
	@Test
	public void testSimpleFunctionDeclaration() {
		TSValue value = executeOneLineScript("function f(x) {return x*x;}; assert (f(2)==4);");
	}

	
	@Test
	public void testFunctionExpressionWithoutArgs() {
		TSValue value = executeOneLineScript("var f = function() {return 4;}; assert (f()==4);");
		value = executeOneLineScript("var f = function() {return 4;}; assert (f(2)==4);");
	}
	
	@Test
	public void testRecursiveFunctionCall() {
		TSValue value = executeScriptFromFile("fibonacci.ts");
	}

	
	@Test
	public void testCounterWithClosures() {
		TSValue value = executeScriptFromFile("counter_with_closures.ts");
	}	
	
/*	
    @Test
	public void testModulesWithClosures() {
		TSValue value = executeScriptFromFile("modules_via_closures.ts");
	} 
*/
	
	@Test
	public void testFunctionCallOnLHSExpression() {
		TSValue value = executeOneLineScript("function f() {return {};} f().key1 = \"Hello\";");
		assertEquals("Hello", value.asString());
		try {
			value = executeOneLineScript("function f() {return {};} f() = \"Hello\";");
			fail("Expected an exception indicating an invalid lefthand-side expression.");
		}
		catch (TinyscriptTypeError e) {
			assertTrue(true);
		}
	}

}

