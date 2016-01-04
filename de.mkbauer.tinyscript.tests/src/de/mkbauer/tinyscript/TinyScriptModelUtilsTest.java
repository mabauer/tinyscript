package de.mkbauer.tinyscript;

import static org.junit.Assert.fail;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.junit4.util.ParseHelper;
import org.eclipse.xtext.junit4.validation.ValidationTestHelper;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.mkbauer.tinyscript.TinyscriptInjectorProvider;
import de.mkbauer.tinyscript.ts.FunctionDefinition;
import de.mkbauer.tinyscript.ts.Tinyscript;
import de.mkbauer.tinyscript.ts.Identifier;

@RunWith(XtextRunner.class)
@InjectWith(TinyscriptInjectorProvider.class)
public class TinyScriptModelUtilsTest {
	
	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;
	
	@Inject
	private ParseHelper<Tinyscript> parser;
	
	@Inject 
	private ValidationTestHelper tester;

	Tinyscript parseFromString(String script) {
		Tinyscript ast = null;
		try {
			ast = parser.parse(script);
			tester.assertNoErrors(ast);
		}
		catch (Exception e) {
			fail("Parser error");
		}
		return ast;
	}
	
		
	@Test
	public void testGetGlobalVariables() {
		Tinyscript ast = parseFromString("var x = 2; var f = function (x) { var local = x*x; return local; }; f(x)==4;");
		List<Identifier> vars = TinyscriptModelUtil.declaredVariablesInBlock(ast.getGlobal());
		List<String> names = vars.stream().map(var -> var.getName()).collect(Collectors.toList());
		Assert.assertTrue(names.stream().anyMatch(s -> s.equals("x")));
		Assert.assertTrue(names.stream().anyMatch(s -> s.equals("f")));
		Assert.assertFalse(names.stream().anyMatch(s -> s.equals("local")));
	}
	
	@Test
	public void testGetVariablesInFunctionScope() {
		Tinyscript ast = parseFromString("var x = 2; var f = function (x) { var local = x*x; return local; }; f(x)==4;");
		FunctionDefinition f = EcoreUtil2.getAllContentsOfType(ast.getGlobal(), FunctionDefinition.class).get(0);
		List<Identifier> vars = TinyscriptModelUtil.declaredVariablesInBlock(f.getBlock());
		List<String> names = vars.stream().map(var -> var.getName()).collect(Collectors.toList());
		Assert.assertTrue(names.stream().anyMatch(s -> s.equals("local")));
		Assert.assertFalse(names.stream().anyMatch(s -> s.equals("x")));
		Assert.assertFalse(names.stream().anyMatch(s -> s.equals("f")));
	}
	
	
	@Test
	public void testGetGlobalFunctionDeclarations() {
		String source = "var x = 2; function f(x) { var local = x*x; return local; } f(x)==4;";
		Tinyscript ast = parseFromString(source);
				
		List<FunctionDefinition> functions = TinyscriptModelUtil.functionDeclarationsInBlock(ast.getGlobal());
		List<String> names = functions.stream().map(f -> f.getId().getName()).collect(Collectors.toList());
		Assert.assertTrue(names.stream().anyMatch(s -> s.equals("f")));
		
		source = "var x = 2; (function (x) { var local = x*x; return local; })(2) == 4;";
		ast = parseFromString(source);
		functions = TinyscriptModelUtil.functionDeclarationsInBlock(ast.getGlobal());
		Assert.assertEquals(0, functions.size());
	}
	
}
