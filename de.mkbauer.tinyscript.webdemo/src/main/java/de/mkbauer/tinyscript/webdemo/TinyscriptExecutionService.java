package de.mkbauer.tinyscript.webdemo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.LazyStringInputStream;
import org.eclipse.xtext.util.StringInputStream;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.springframework.stereotype.Component;

import com.google.inject.Injector;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import de.mkbauer.tinyscript.TinyscriptSyntaxError;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.ResourceConsumption;
import de.mkbauer.tinyscript.interpreter.ResourceLimits;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.ts.Tinyscript;

@Component
public class TinyscriptExecutionService {
	
	private final static String fileExtension = "ts";
	
	public static final int MAX_STATEMENTS = 500000;
	public static final int MAX_CALL_DEPTH = 256;
	public static final int MAX_OBJECT_SIZE = 1024*32;
	public static final int MAX_STRING_LENGTH = 1024*8;
	
	private Injector injector;
	
	public TinyscriptExecutionService() {
		injector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
	}
	
	public TinyscriptExecutionResult executeScriptFromString(String script) {

		ExecutionVisitor executionvisitor = new ExecutionVisitor();
		String resultAsString = "";
		String errorMessage = "";
		int errorLine = 0;
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ResourceConsumption statistics = new ResourceConsumption();
		
		try {
			Tinyscript ast = parseScriptFromString(script);
			executionvisitor.defineStdOut(stdout);
			// executionvisitor.setResourceLimits(ResourceLimits.UNLIMITED);
			ResourceLimits limits = new ResourceLimits();
			limits.setMaxStatements(MAX_STATEMENTS);
			limits.setMaxRecursionDepth(MAX_CALL_DEPTH);
			limits.setMaxObjectSize(MAX_OBJECT_SIZE);
			limits.setMaxStringLength(MAX_STRING_LENGTH);
			executionvisitor.setResourceLimits(limits);
			TSValue result = executionvisitor.execute(ast);
			resultAsString = result.asString();
			String output = stdout.toString();
			statistics = executionvisitor.getResourceConsumption();
			return new TinyscriptExecutionResult(resultAsString, output, statistics);
		}
		catch (TinyscriptRuntimeException e) {
			errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
			errorLine = e.getAffectedLine();
			String output = stdout.toString();
			statistics = executionvisitor.getResourceConsumption();
			return new TinyscriptExecutionResult(resultAsString, output, statistics, errorMessage, errorLine);
		}
	}
	
	protected Tinyscript parseScriptFromString(String script) {
		Tinyscript ast;
		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
		URI uri = newResourceUri(resourceSet);
		Resource currentResource = resourceSet.getResource(uri, false);
		if (currentResource == null) {
			currentResource = resourceSet.createResource(uri);
		}
		try {
			currentResource.load(new StringInputStream(script), null);
			ast = (Tinyscript) (currentResource.getContents().isEmpty() ? null : currentResource.getContents().get(0));
		} catch (IOException e) {
			throw new WrappedException(e);
		}	
		List<Issue> issues = getValidator(currentResource).validate(currentResource, CheckMode.ALL, CancelIndicator.NullImpl);
		if (issues.size()!=0)
			throw new TinyscriptSyntaxError(issues.get(0).getMessage(), currentResource.getURI().toString(), issues.get(0).getLineNumber());
		return ast;
	}
	
	protected IResourceValidator getValidator(Resource resource) {
		return ((XtextResource) resource).getResourceServiceProvider().getResourceValidator();
	}
	
	protected URI newResourceUri(XtextResourceSet resourceSet) {
		String name = "__webdemo";
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			URI syntheticUri = URI.createURI(name + i + "." + fileExtension);
			if (resourceSet.getResource(syntheticUri, false) == null)
				return syntheticUri;
		}
		throw new IllegalStateException();
	}
	
	protected InputStream getAsStream(CharSequence text) {
		return new LazyStringInputStream(text == null ? "" : text.toString());
	}

}
