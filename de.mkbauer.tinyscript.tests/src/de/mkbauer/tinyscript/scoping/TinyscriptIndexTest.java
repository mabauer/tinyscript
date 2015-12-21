package de.mkbauer.tinyscript.scoping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
		assertEquals("[f, f.x, f.local, global]", exported.toString());
	}
	
	@Test
	public void testIndexWithBlock() {
		Tinyscript ast = parseFromString("{ var local=1; } var global = 1; assert (global==1); ");
		// tester.assertNoErrors(ast);
		Iterable<IEObjectDescription> exported = index.getExportedEObjectDescriptions(ast);
		assertIterableContainsString("block_", exported);
		assertIterableContainsString(".local", exported);
		assertIterableContainsString("global", exported);
	}
	
	@Test
	public void testIndexWithForeach() {
		Tinyscript ast = parseFromString("var obj = {}; var i=0; foreach (var x in obj) { i=i+1; } assert (i==0); ");
		// tester.assertNoErrors(ast);
		Iterable<IEObjectDescription> exported = index.getExportedEObjectDescriptions(ast);
		assertIterableContainsString("obj", exported);
		assertIterableContainsString("i", exported);
		assertIterableContainsString("foreach_", exported);
		assertIterableContainsString(".x", exported);

	}
	
	public <T> void assertIterableContainsString(String expected, Iterable<T> elements) {
		for (T element : elements) {
			if (element.toString().contains(expected))
				return;
		}
		fail(expected + " not found in " + elements.toString());
	}
}
