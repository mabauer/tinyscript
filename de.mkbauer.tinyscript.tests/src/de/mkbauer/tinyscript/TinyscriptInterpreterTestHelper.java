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
import org.junit.Before;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.mkbauer.tinyscript.TinyscriptInjectorProvider;
import de.mkbauer.tinyscript.TinyscriptRuntimeModule;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.ExpressionStatement;
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
	
	private ExecutionVisitor visitor;

	public TinyscriptInterpreterTestHelper() {
		super();
	}
	
	@Before
	public void setUp()  {
		visitor = new ExecutionVisitor();
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
		// TODO: Check exception handling
		catch (Exception e) {
			fail("Error in: " + line + ": " + e.getClass());
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
			fail("Syntax error in: " + line + ": " + e.getClass());
			e.printStackTrace();
		}
		return ast;
	}
	
	protected TSValue execute(Tinyscript ast) {
		TSValue result = visitor.execute(ast);
		return result;
	}
	
	protected TSValue evaluateSimpleExpression(String line) {
		String statement = line + ";";
		Tinyscript ast = parseScriptFromString(statement);
		getValidator().assertNoErrors(ast);
		ExpressionStatement expr = (ExpressionStatement) ast.getGlobal().getStatements().get(0);
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
	
	protected ExecutionVisitor getExecutionVisitor() {
		return visitor;
	}

}