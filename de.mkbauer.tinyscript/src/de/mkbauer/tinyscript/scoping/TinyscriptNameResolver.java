package de.mkbauer.tinyscript.scoping;


import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.ts.BlockStatement;
import de.mkbauer.tinyscript.ts.ElseStatement;
import de.mkbauer.tinyscript.ts.ForEachStatement;
import de.mkbauer.tinyscript.ts.FunctionDefinition;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.IfStatement;
import de.mkbauer.tinyscript.ts.NumericForStatement;
import de.mkbauer.tinyscript.ts.Tinyscript;
import de.mkbauer.tinyscript.ts.util.TsSwitch;

public class TinyscriptNameResolver extends TsSwitch<String> {
	
	public String getName(EObject object) {
		return doSwitch(object);
	}
	
	@Override
	public String defaultCase(EObject object) {
		return null;
	}
	
	@Override
	public String caseTinyscript(Tinyscript object) {
		return null;
	}

	@Override
	public String caseFunctionDefinition(FunctionDefinition function) {
		if (function.getId() != null)
			return "function" + function.getId().getName();
		else
			return "function" + getUniqueID(function);
	}

	@Override
	public String caseIfStatement(IfStatement object) {
		return "if" + getUniqueID(object);
	}

	@Override
	public String caseElseStatement(ElseStatement object) {
		return "else" + getUniqueID(object);
	}

	@Override
	public String caseForEachStatement(ForEachStatement object) {
		return "foreach" + getUniqueID(object);
	}
	
	@Override
	public String caseNumericForStatement(NumericForStatement object) {
		return "for" + getUniqueID(object);
	}

	@Override
	public String caseBlockStatement(BlockStatement object) {
		return "block" + getUniqueID(object);
	}	
	
	@Override
	public String caseIdentifier(Identifier identifier) {
		EObject parent = identifier.eContainer();
		if (parent instanceof FunctionDefinition) {
			// Let's ignore the function identifier
			FunctionDefinition function = (FunctionDefinition) (parent);
			if ( identifier == function.getId() ) {
				return null;
			}
		}
		return identifier.getName();
	}
	
	public String getUniqueID(EObject object) {		
		String id = String.valueOf(object.hashCode());
		return id;
	}

}
