package de.mkbauer.tinyscript.scoping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.junit4.util.ParseHelper;
import org.eclipse.xtext.junit4.validation.ValidationTestHelper;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import de.mkbauer.tinyscript.TinyscriptInjectorProvider;
import de.mkbauer.tinyscript.ts.Tinyscript;


@RunWith(XtextRunner.class)
@InjectWith(TinyscriptInjectorProvider.class)
public class TinyscriptIndexTest {
	
	@Inject
	private ParseHelper<Tinyscript> parser;
	
	@Inject
	private ValidationTestHelper tester;
	
	@Inject
	private TinyscriptIndex index;
	
	Tinyscript parseFromString(String script) {
		Tinyscript ast = null;
		try {
			ast = (Tinyscript)parser.parse(script);

		}
		catch (Exception e) {
			fail("Parser error");
		}
		return ast;
	}
	
	
	@Test
	public void testIndexWithFunction() {
		Tinyscript ast = parseFromString("function f(x) {var local=1; return x;} var global=1; assert (f(global)==1); ");
		// tester.assertNoErrors(ast);
		Iterable<IEObjectDescription> exported = index.getExportedEObjectDescriptions(ast);
		assertIterableContainsString("f", exported);
		assertIterableContainsString("functionf", exported);
		assertIterableContainsString("functionf.x", exported);
		assertIterableContainsString("functionf.local", exported);
		assertIterableContainsString("global", exported);
		
		ast = parseFromString("var f = function (x) {var local=1; return x;} var global=1; assert (f(global)==1); ");
		exported = index.getExportedEObjectDescriptions(ast);
		assertIterableContainsString("f", exported);
		assertIterableContainsString("function\\d+", exported);
		assertIterableContainsString("function\\d+.x", exported);
		assertIterableContainsString("function\\d+.local", exported);
		assertIterableContainsString("global", exported);
	}
	
	@Test
	public void testIndexWithBlock() {
		Tinyscript ast = parseFromString("{ var local=1; } var global = 1; assert (global==1); ");
		// tester.assertNoErrors(ast);
		Iterable<IEObjectDescription> exported = index.getExportedEObjectDescriptions(ast);
		assertIterableContainsString("block\\d+", exported);
		assertIterableContainsString("block\\d+.local", exported);
		assertIterableContainsString("global", exported);
	}
	
	@Test
	public void testIndexWithForeach() {
		Tinyscript ast = parseFromString("var obj = {}; var i=0; foreach (var x in obj) { i=i+1; } assert (i==0); ");
		// tester.assertNoErrors(ast);
		Iterable<IEObjectDescription> exported = index.getExportedEObjectDescriptions(ast);
		assertIterableContainsString("obj", exported);
		assertIterableContainsString("i", exported);
		assertIterableContainsString("foreach\\d+", exported);
		assertIterableContainsString("foreach\\d+.x", exported);

	}
	
	public <T> void assertIterableContainsString(String expected, Iterable<T> elements) {
		for (T element : elements) {
			if (Pattern.matches(expected, element.toString()))
				return;
		}
		fail(expected + " not found in " + elements.toString());
	}
}
