package de.mkbauer.tinyscript.interpreter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;


@RunWith(XtextRunner.class)
public class AssignmentTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testSimpleAssignment() {
		TSValue value = executeScriptFromString("var x; x=1; assert (x==1);");
		value = executeScriptFromString("var x=1; assert (x==1);");
	}
}

