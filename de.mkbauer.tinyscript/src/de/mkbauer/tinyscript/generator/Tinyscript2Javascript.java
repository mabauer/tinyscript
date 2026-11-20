package de.mkbauer.tinyscript.generator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.Injector;

import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import de.mkbauer.tinyscript.interpreter.FileSystemModuleLoader;
import de.mkbauer.tinyscript.ts.Tinyscript;


public class Tinyscript2Javascript {
		
	public static void main(String[] args) {
		// Guice's bytecode generation (AOP, unused by Xtext) calls sun.misc.Unsafe,
		// which prints a deprecation warning on Java 24+ and will be removed.
		if (System.getProperty("guice_bytecode_gen_option") == null)
			System.setProperty("guice_bytecode_gen_option", "DISABLED");
		Tinyscript script = null;
		String sourceFilename = args[0];
		Path source = Path.of(sourceFilename).toAbsolutePath();
		Injector injector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
		try {
	 		URI uri = URI.createFileURI(source.toString());
	 		ResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
	 		Resource resource = resourceSet.createResource(uri);
	 		resource.load(null);
	 		script = (Tinyscript) resource.getContents().get(0);
		}
		catch (IOException e) {
			System.err.println("File " + sourceFilename + " not found.");
			System.exit(1);
		}
		// Imports of bare paths are searched next to the source file
 		TinyscriptGenerator generator = injector.getInstance(TinyscriptGenerator.class);
 		CharSequence jsSource = generator.generate(script, true, new FileSystemModuleLoader(List.of(source.getParent())));
 		System.out.append(jsSource);
    }  
  	
}