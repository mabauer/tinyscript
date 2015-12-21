package de.mkbauer.tinyscript.repl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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

import de.mkbauer.console.TextDevice;
import de.mkbauer.console.TextDevices;
import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.ts.Tinyscript;


class TinyscriptREPLDemo  {
	
	private Provider<XtextResourceSet> resourceSetProvider;
	
	private static final String fileExtension = "ts"; 
	
	private ExecutionVisitor visitor = null;

	private XtextResourceSet resourceSet;

	private Resource currentResource; 
	
	private TextDevice device;
		
	public static void main(String[] args) {
		TinyscriptREPLDemo repl = new TinyscriptREPLDemo();
		repl.loop();
	}
		
	public TinyscriptREPLDemo() {	
		Injector injector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
		resourceSet = injector.getInstance(XtextResourceSet.class);
		visitor = new ExecutionVisitor();	
		device = TextDevices.defaultTextDevice();
		// execute("var x = 2; var f = function(x) { var b=2; return b*x; };");
		// execute("assert(f(x)==4);"); // Fix me!
    }  
	
	public void loop() {
		String line; 
		boolean quit = false;
		while (!quit) {
			device.printf("> ");
			line = device.readLine();
			try {
				line = line.trim();
				if (line == null || line.equals("")) {
					quit = true;
				}
				else {
					TSValue result = execute(line);
					device.printf("%s\n", result.asString());
				}
			}
			catch (TinyscriptRuntimeException e) {
				device.printf("%s\n", e.getMessage());
			}
		}
		device.printf("Bye!\n");
	}
	
	protected TSValue execute(String input) {
		try {
			parse(input);
			Tinyscript ast = (Tinyscript) (currentResource.getContents().isEmpty() ? null : currentResource.getContents().get(0));
			List<Issue> issues = getValidator(currentResource).validate(currentResource, CheckMode.ALL, CancelIndicator.NullImpl);
			if (!issues.isEmpty())
				throw new TinyscriptRuntimeException(issues.get(0).getMessage() + " in " + input);
	 		TSValue result = visitor.execute(ast);
	 		return result;
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}
	
	protected void parse(String input) throws IOException {
		URI uri = newResourceUri();
		currentResource = resourceSet.getResource(uri, false);
		if (currentResource == null) {
			currentResource = resourceSet.createResource(uri);
		} else {
			currentResource.unload();
		}
		currentResource.load(new StringInputStream(input), null);
		EcoreUtil2.resolveAll(currentResource);
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

