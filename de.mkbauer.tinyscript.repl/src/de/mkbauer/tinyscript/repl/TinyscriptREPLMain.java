package de.mkbauer.tinyscript.repl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import jline.console.ConsoleReader;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.LazyStringInputStream;
import org.eclipse.xtext.util.StringInputStream;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptReferenceError;
import de.mkbauer.tinyscript.ts.Tinyscript;


class TinyscriptREPLMain  {
	
	private static final String fileExtension = "ts"; 
	
	private ExecutionVisitor visitor = null;

	private XtextResourceSet resourceSet;

	private Resource currentResource; 
	
	private ConsoleReader device;
		
	public static void main(String[] args) {
		try {
			TinyscriptREPLMain repl = new TinyscriptREPLMain();
			repl.loop();
		}
		catch (IOException e) {
			System.err.println("Error initializing console.");
		}
	}
		
	public TinyscriptREPLMain() throws IOException {	
		Injector injector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
		resourceSet = injector.getInstance(XtextResourceSet.class);
		visitor = new ExecutionVisitor();
		device = new ConsoleReader();
		device.setExpandEvents(false);
    }  
	
	public void loop() throws IOException {
		String line; 
		String script = "";
		boolean quit = false;
		boolean multiline = false;
		while (!quit) {
			if (multiline) {
				device.setPrompt("... ");
			}
			else {
				device.setPrompt("> ");
			}
			device.flush();
			line = device.readLine();
			try {
				if (line == null || line.trim().equals("!bye")) {
					quit = true;
				}
				else {
					Tinyscript ast = parse(script + line, multiline);
					if (ast != null) {
						TSValue result = execute(ast);
						device.println(result.asString());
						multiline = false;
						script = "";
					}
					else {
						multiline = true;
						script = script + line;
					}
				}
			}
			catch (TinyscriptRuntimeException e) {
				device.println(e.getClass().getSimpleName() + ": " + e.getMessage());
				if (!multiline)
					script = "";
			}
		}
		device.println("Bye!");
		device.flush();
	}
	
	protected Tinyscript parse(String script, boolean reuseResource) {
		Tinyscript ast = null;
		if (!reuseResource) {
			URI uri = newResourceUri();
			currentResource = resourceSet.getResource(uri, false);
			if (currentResource == null) {
				currentResource = resourceSet.createResource(uri);
			}
		} 
		else {
			if (currentResource != null) {
				currentResource.unload();
			}
		}
		try {
			currentResource.load(new StringInputStream(script), null);
			ast = (Tinyscript) (currentResource.getContents().isEmpty() ? null : currentResource.getContents().get(0));
		} catch (IOException e) {
			throw new WrappedException(e);
		}	
		List<Issue> issues = getValidator(currentResource).validate(currentResource, CheckMode.ALL, CancelIndicator.NullImpl);
		if (!issues.isEmpty()) {
			String msg = issues.get(0).getMessage();
			if (msg.contains("mismatched input '<EOF>'"))
				if (issues.size() == 1)
					return null;
				else
					msg = issues.get(1).getMessage();
			throw new TinyscriptRuntimeException("in '" + script + "': " + msg);	
		}
		return ast;
	}
	
	protected TSValue execute(Tinyscript ast) {
 		TSValue result = visitor.execute(ast);
 		return result;	
	}
	
	protected IResourceValidator getValidator(Resource resource) {
		return ((XtextResource) resource).getResourceServiceProvider().getResourceValidator();
	}
	
	protected URI newResourceUri() {
		String name = "__repl";
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

