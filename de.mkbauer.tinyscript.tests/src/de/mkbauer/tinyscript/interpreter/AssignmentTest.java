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
public class AssignmentTest {
	
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
	public void testSimpleAssignment() {
		TSValue value = executeOneLineScript("var x; x=1; assert (x==1);");
		value = executeOneLineScript("var x=1; assert (x==1);");
	}
}

