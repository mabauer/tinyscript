package de.mkbauer.tinyscript.scoping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.junit4.util.ParseHelper;
import org.eclipse.xtext.junit4.validation.ValidationTestHelper;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import de.mkbauer.tinyscript.TinyscriptInjectorProvider;
import de.mkbauer.tinyscript.TinyscriptInterpreterTestHelper;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.Reference;
import de.mkbauer.tinyscript.ts.Tinyscript;
import de.mkbauer.tinyscript.ts.TsPackage;

@RunWith(XtextRunner.class)
@InjectWith(TinyscriptInjectorProvider.class)
public class TinyScriptScopeProviderTest extends TinyscriptInterpreterTestHelper {
	
	@Inject
	private IScopeProvider scopeProvider;

	@Inject
	private Object qualifiedNameConverter;
	
	
	@Test
	public void testValidSimpleReference() {
		Tinyscript ast = parseScriptFromString("var x =\"Hello, World!\"; x;");
		getValidator().assertNoErrors(ast);
	}
	
	@Test
	public void testInvalidSimleReference() {
		Tinyscript ast = parseScriptFromStringWithoutValidating("x; var x =\"Hello, World!\";");
		assertFalse(getValidator().validate(ast).isEmpty());
	}
		
	@Test
	public void testFunctionScope() {
		Tinyscript ast = parseScriptFromString("var x = 1; var f = function(x) {return x;}; f(x);");
		getValidator().assertNoErrors(ast);

				// parseFromString("var x =\"Hello, World!\"; var f = function (x) { return x; }; f(x);");
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
		Tinyscript ast = parseScriptFromString("var f = function() {return global;}; var global;");
		getValidator().assertNoErrors(ast);
		// assertFalse(getValidator().validate(ast).isEmpty());
	}
	
	
	
	private Identifier lookup(Reference reference) {
		return reference.getId();
	}

}
