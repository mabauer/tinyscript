package de.mkbauer.tinyscript.interpreter;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.Injector;

import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.Statement;
import de.mkbauer.tinyscript.ts.Tinyscript;
import de.mkbauer.tinyscript.ts.VariableStatement;
import de.mkbauer.tinyscript.ts.util.TsSwitch;

class TinyscriptInterpreter  {
	
	private ExecutionVisitor visitor = null; 
		
	public static void main(String[] args) {
		Tinyscript ast = null;
		try {
			String fileName = args[0];
	 		URI uri = URI.createURI(fileName);
	  
	 		Injector injector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
	 		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
	 		Resource resource = resourceSet.createResource(uri);
	 		resource.load(new HashMap());
	 		ast = (Tinyscript) resource.getContents().get(0);
		}
		catch (IOException e) {
			System.exit(1);
		}
 		ExecutionVisitor visitor = new ExecutionVisitor();
 		visitor.execute(ast);		
    }  
  	
}