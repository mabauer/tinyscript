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
import de.mkbauer.tinyscript.TinyscriptInterpreterTestHelper;
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.Tinyscript;

@RunWith(XtextRunner.class)
public class AssignmentTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testSimpleAssignment() {
		TSValue value = executeScriptFromString("var x; x=1; assert (x==1);");
		value = executeScriptFromString("var x=1; assert (x==1);");
	}
}

