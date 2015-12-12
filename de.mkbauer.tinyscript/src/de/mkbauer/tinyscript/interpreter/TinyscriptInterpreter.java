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
import de.mkbauer.tinyscript.ts.util.TsSwitch;

class TinyscriptInterpreter extends TsSwitch<Object> {
	
	private ExpressionVisitor evaluator = null; 
		
	public static void main(String[] args) {
		Tinyscript script = null;
		try {
			String fileName = args[0];
	 		URI uri = URI.createURI(fileName);
	  
	 		Injector injector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
	 		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
	 		Resource resource = resourceSet.createResource(uri);
	 		resource.load(new HashMap());
	 		script = (Tinyscript) resource.getContents().get(0);
		}
		catch (IOException e) {
			System.exit(1);
		}
 		TinyscriptInterpreter interpreter = new TinyscriptInterpreter();
 		interpreter.execute(script);		
    }  
	
	public TinyscriptInterpreter() {
		evaluator = new ExpressionVisitor();
	}
  
  	public Object execute(EObject object) {
  		return doSwitch(object);		
  	}
  	
	@Override
	public Object defaultCase(EObject object) {
		throw new UnsupportedOperationException("Unsupported script node: " + object.eClass().getName());
	}
  	
    @Override
	public Object caseTinyscript(Tinyscript object) {
    	return execute(object.getGlobal());
	}

    @Override
	public Object caseBlock(Block object) {
        for (Statement s : object.getStatements()) {
        	execute(s); 
        }
        return object;
    }
    
    @Override
    public Object caseExpression(Expression object) {
    	// TODO: Complete!
    	TSValue value = evaluator.evaluate(object);
    	return object;
    }
    
}