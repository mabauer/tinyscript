package de.mkbauer.tinyscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;


@ExtendWith(InjectionExtension.class)
public class AssignmentTest extends TinyscriptInterpreterTestHelper {
	
	@Test
	public void testSimpleAssignment() {
		TSValue value = executeScriptFromString("let x; x=1; assert (x==1);");
		value = executeScriptFromString("let x=1; assert (x==1);");
	}
}

