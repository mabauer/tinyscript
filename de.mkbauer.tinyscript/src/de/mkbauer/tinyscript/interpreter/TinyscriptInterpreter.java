package de.mkbauer.tinyscript.interpreter;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.Injector;

import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import de.mkbauer.tinyscript.ts.Tinyscript;

class TinyscriptInterpreter  {
		
	public static void main(String[] args) {
		Tinyscript ast = null;
		try {
			String fileName = args[0];
	 		URI uri = URI.createURI(fileName);
	  
	 		Injector injector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
	 		ResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
	 		Resource resource = resourceSet.createResource(uri);
	 		resource.load(null);
	 		ast = (Tinyscript) resource.getContents().get(0);
		}
		catch (IOException e) {
			System.exit(1);
		}
 		ExecutionVisitor visitor = new ExecutionVisitor();
 		visitor.execute(ast);		
    }  
  	
}