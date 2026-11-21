package de.mkbauer.tinyscript.scoping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.List;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.inject.Inject;

import de.mkbauer.tinyscript.tests.TinyscriptInjectorProvider;
import de.mkbauer.tinyscript.tests.TinyscriptInterpreterTestHelper;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.Reference;
import de.mkbauer.tinyscript.ts.Tinyscript;

@ExtendWith(InjectionExtension.class)
@InjectWith(TinyscriptInjectorProvider.class)
public class TinyScriptScopeProviderTest extends TinyscriptInterpreterTestHelper {
	
	@Inject
	private IScopeProvider scopeProvider;

	@Inject
	private Object qualifiedNameConverter;
	
	
	@Test
	public void testValidSimpleReference() {
		Tinyscript ast = parseScriptFromString("let x =\"Hello, World!\"; x;");
		getValidator().assertNoErrors(ast);
	}
	
	@Test
	public void testInvalidSimleReference() {
		Tinyscript ast = parseScriptFromStringWithoutValidating("x; let x =\"Hello, World!\";");
		assertFalse(getValidator().validate(ast).isEmpty());
	}
		
	@Test
	public void testFunctionScope() {
		Tinyscript ast = parseScriptFromString("let x = 1; let f = function(x) {return x;}; f(x);");
		getValidator().assertNoErrors(ast);

		// parseFromString("let x =\"Hello, World!\"; let f = function (x) { return x; }; f(x);");
		List<Identifier> ids = EcoreUtil2.getAllContentsOfType(ast, Identifier.class);
		Identifier xOuter = ids.get(0);
		assertEquals(xOuter.getName(), "x");
		Identifier f = ids.get(1);
		assertEquals(f.getName(), "f");
		List<Reference> refs = EcoreUtil2.getAllContentsOfType(ast, Reference.class);
		Reference xReturn = refs.get(0);
		assertFalse(xReturn.getId().equals(xOuter));
		Reference fCall  = refs.get(1);
		assertEquals(fCall.getId(), f);
		Reference xFx = refs.get(2);
		assertEquals(xFx.getId(), xOuter);
		
	}
	
	@Test
	public void testFunctionGlobalVariable() {
		Tinyscript ast = parseScriptFromString("let f = function() {return global;}; let global;");
		getValidator().assertNoErrors(ast);
		// assertFalse(getValidator().validate(ast).isEmpty());
	}
	
	private Identifier lookup(Reference reference) {
		return reference.getId();
	}

}
