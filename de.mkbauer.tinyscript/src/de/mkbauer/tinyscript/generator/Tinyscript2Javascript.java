package de.mkbauer.tinyscript.generator;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.Injector;

import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import de.mkbauer.tinyscript.ts.Tinyscript;


public class Tinyscript2Javascript {
		
	public static void main(String[] args) {
		Tinyscript script = null;
		String sourceFilename = args[0];
		try {
			
			// String targetFilename = args[1];
			
	 		URI uri = URI.createURI(sourceFilename);
	  
	 		Injector injector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
	 		ResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
	 		Resource resource = resourceSet.createResource(uri);
	 		resource.load(null);
	 		script = (Tinyscript) resource.getContents().get(0);
		}
		catch (IOException e) {
			System.err.println("File " + sourceFilename + " not found.");
			System.exit(1);
		}
 		TinyscriptGenerator generator = new TinyscriptGenerator();
 		CharSequence jsSource = generator.generate(script, true);	
 		System.out.append(jsSource);
    }  
  	
}