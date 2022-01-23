package de.mkbauer.tinyscript.webdemo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.ResourceMonitor;
import de.mkbauer.tinyscript.interpreter.ResourceConsumption;
import de.mkbauer.tinyscript.interpreter.ResourceLimits;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.ts.Tinyscript;

@Component
public class TinyscriptExecutionService {
	
	private final static String fileExtension = "ts";
	
	public static final int MAX_STATEMENTS = 5000000;  
	public static final int MAX_CALL_DEPTH = 128;
	public static final int MAX_OBJECT_SIZE = 0; // 100000;
	public static final int MAX_STRING_LENGTH = 0; // 1024*8;
	public static final long MAX_MEMORY = 1024*1024*16;
	
	public static final int MAX_OUTPUT_SIZE = 1024*8;
	
	private static final Logger logger = LoggerFactory.getLogger(TinyscriptExecutionService.class);
	
	private Injector injector;
	private XtextResourceSet resourceSet;
	private Resource currentResource;
	
	public TinyscriptExecutionService() {
		injector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
		resourceSet = injector.getInstance(XtextResourceSet.class);
	}
	
	public TinyscriptExecutionResult executeScriptFromString(String script) {
		return executeScriptFromString(script, true);
	}
	
	public TinyscriptExecutionResult executeScriptFromString(String script, boolean log) {

		if (log)
			logger.debug("Script:\n" + script);
		
		String resultAsString = "";
		String errorMessage = "";
		int errorLine = 0;
		// TODO: Should be replaced by an OutputStream with a limited capacity
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		
		ResourceLimits limits = new ResourceLimits();
		limits.setMaxStatements(MAX_STATEMENTS);
		limits.setMaxRecursionDepth(MAX_CALL_DEPTH);
		limits.setMaxObjectSize(MAX_OBJECT_SIZE);
		limits.setMaxStringLength(MAX_STRING_LENGTH);
		limits.setMaxMemory(MAX_MEMORY);
		ResourceMonitor monitor = new ResourceMonitor();
		monitor.enableObjectTracking();
		monitor.enableMXBeanInspection();
		monitor.configureLimits(limits);
		TinyscriptEngine engine = new TinyscriptEngine(resourceSet, monitor);
		ResourceConsumption statistics = new ResourceConsumption();
		
		try {
			Tinyscript ast = parseScriptFromString(script);
			engine.defineStdOut(stdout);
			// executionvisitor.setResourceLimits(ResourceLimits.UNLIMITED);

			TSValue result = engine.execute(ast);
			resultAsString = result.asString();
			String output = stdoutToString(stdout);
			statistics = monitor.getTotalResourceConsumption();
			logExecution(script, null, statistics);
			// Unload Xtext resource! 
			try {
				currentResource.delete(null);
			}
			catch (IOException e) {
				logger.warn("Deleteing Xtext resource failed");
			}
			return new TinyscriptExecutionResult(resultAsString, output, statistics);
		}
		catch (TinyscriptRuntimeException e) {
			errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
			errorLine = e.getAffectedLine();
			String output = stdoutToString(stdout);
			statistics = monitor.getTotalResourceConsumption();
			logExecution(script, e, statistics);
			return new TinyscriptExecutionResult(resultAsString, output, statistics, errorMessage, errorLine);
		}
	}
	
	private String stdoutToString(ByteArrayOutputStream out) {
		String result = out.toString();
		return result.substring(Math.max(0, result.length()-MAX_OUTPUT_SIZE));
	}
	
	private void logExecution(String script, TinyscriptRuntimeException e, ResourceConsumption statistics) {
		if (e == null) {
			logger.info("Script executed: OK");
		}
		else {
			logger.info("Script executed with errors: " + e.getMessage() + e.getTinyscriptStacktraceAsString());
		}
		logger.info("Statistics: " + statistics.toString());
	}
	
	protected Tinyscript parseScriptFromString(String script) {
		Tinyscript ast;

		URI uri = newResourceUri(resourceSet);
		currentResource = resourceSet.getResource(uri, false);
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
