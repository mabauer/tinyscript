package de.mkbauer.tinyscript.webdemo;

import java.io.IOException;
import java.io.InputStream;
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

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import de.mkbauer.tinyscript.TinyscriptSyntaxError;
import de.mkbauer.tinyscript.generator.TinyscriptGenerator;
import de.mkbauer.tinyscript.ts.Tinyscript;

@Component
public class TinyscriptCrossCompilingService {
	
	private static final String fileExtension = "ts";
	
	@Inject
	private TinyscriptGenerator generator;

	private Injector injector;
	
	public TinyscriptCrossCompilingService() {
		injector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
	}
	
	
	public TinyscriptCrossCompilingResult crossCompileScriptFromString(String script) {
		String jsscript = "";
		int errorCode = 0;
		try {
			Tinyscript ast = parseScriptFromString(script);
			injector.injectMembers(this);
			// TinyscriptGenerator generator = new TinyscriptGenerator();
			jsscript = generator.generate(ast, true).toString();
			return new TinyscriptCrossCompilingResult(jsscript);
		}
		catch (TinyscriptRuntimeException e) {
			// TODO: Error handling
			String errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
			int errorLine = e.getAffectedLine();
			return new TinyscriptCrossCompilingResult(errorMessage, errorLine);
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
