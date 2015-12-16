package de.mkbauer.tinyscript.interpreter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.junit4.util.ParseHelper;
import org.eclipse.xtext.junit4.validation.ValidationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import de.mkbauer.tinyscript.TinyscriptInjectorProvider;
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.Tinyscript;

@RunWith(XtextRunner.class)
@InjectWith(TinyscriptInjectorProvider.class)
public class ObjectExpressionsTest {
	
	private static final double epsilon = 0.001;

	@Inject
	ParseHelper<Tinyscript> parser;
	
	@Inject 
	private ValidationTestHelper validator;

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
	public void testSimpleObjectExpression() {
		TSValue value = executeOneLineScript("var o = {}; o.key=1; assert (o.key==1);");
		value = executeOneLineScript("var o = {}; o[\"key\"]=1; assert (o[\"key\"]==1);");
	}
	
	@Test
	public void testChainedObjectExpression() {
		TSValue value = executeOneLineScript("var o = {}; o.key1 = {}; o.key1.key2 = 1; assert (o.key1.key2==1);");
		value = executeOneLineScript("var o = {}; o.key1 = {}; var test = o.key1; test.key2 = 1; assert (o.key1.key2==1);");
	}
	
	@Test
	public void testMixedPropertyAccessExpressions() {
		TSValue value = executeOneLineScript("var o = {}; o.key = \"Test\"; assert (o[\"key\"]==\"Test\");");
	}
	
	@Test
	public void testObjectInitializers() {
		TSValue value = executeOneLineScript("var o = { key1: \"Hello\", key2: \"World\" }; assert (o[\"key1\"]==\"Hello\");"); //  && o.key2 == \"World\"
	}
}
