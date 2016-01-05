package de.mkbauer.tinyscript;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.util.ParseHelper;
import org.eclipse.xtext.junit4.validation.ValidationTestHelper;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.mkbauer.tinyscript.TinyscriptInjectorProvider;
import de.mkbauer.tinyscript.TinyscriptRuntimeModule;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.Tinyscript;

@InjectWith(TinyscriptInjectorProvider.class)
public class TinyscriptInterpreterTestHelper {

	protected static final double epsilon = 0.000001;

	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;

	@Inject
	private ParseHelper<Tinyscript> parser;

	@Inject
	private ValidationTestHelper validator;

	public TinyscriptInterpreterTestHelper() {
		super();
	}
	
	protected TSValue executeScriptFromFile(String filename) {
		Tinyscript ast = parseScriptFromFile(filename);
		return execute(ast);
	}

	protected Tinyscript parseScriptFromFile(String filename) {
		Tinyscript ast = null;
		try {
			URI uri = URI.createURI(filename);
			Resource resource = resourceSetProvider.get().createResource(uri);
			resource.load(null);
			ast = (Tinyscript) resource.getContents().get(0);
			validator.assertNoErrors(ast);
		}
		catch (IOException e) {
			fail("Script " + filename + " not found.");
		}
		return ast;
	}
	
	protected TSValue executeScriptFromString(String line) {
		Tinyscript ast = parseScriptFromString(line);
		return execute(ast);
	}
	
	protected Tinyscript parseScriptFromString(String line) {
		Tinyscript ast = null;
		try {
			 ast = (Tinyscript) parser.parse(line);	
			 validator.assertNoErrors(ast);
		}
		catch (Exception e) {
			fail("Syntax error in: " + line + ": " + e.getMessage());
			e.printStackTrace();
		}
		return ast;
	}
	
	protected Tinyscript parseScriptFromStringWithoutValidating(String line) {
		Tinyscript ast = null;
		try {
			 ast = (Tinyscript) parser.parse(line);	
		}
		catch (Exception e) {
			fail("Syntax error in: " + line + ": " + e.getMessage());
			e.printStackTrace();
		}
		return ast;
	}
	
	protected TSValue execute(Tinyscript ast) {
		ExecutionVisitor visitor = new ExecutionVisitor();
		TSValue result = visitor.execute(ast);
		return result; 
	}
	
	protected TSValue evaluateSimpleExpression(String line) {
		String statement = line + ";";
		Tinyscript ast = parseScriptFromString(statement);
		getValidator().assertNoErrors(ast);
		Expression expr = (Expression) ast.getGlobal().getStatements().get(0);
		ExecutionVisitor visitor = new ExecutionVisitor();
		TSValue result = visitor.execute(expr);
		return result;
	}

	protected Provider<XtextResourceSet> getResourceSetProvider() {
		return resourceSetProvider;
	}

	protected ParseHelper<Tinyscript> getParser() {
		return parser;
	}

	protected ValidationTestHelper getValidator() {
		return validator;
	}

}