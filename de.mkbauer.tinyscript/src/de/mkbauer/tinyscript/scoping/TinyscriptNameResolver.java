package de.mkbauer.tinyscript.scoping;

import org.eclipse.emf.ecore.EObject;

import de.mkbauer.tinyscript.ts.BlockStatement;
import de.mkbauer.tinyscript.ts.ElseStatement;
import de.mkbauer.tinyscript.ts.ForEachStatement;
import de.mkbauer.tinyscript.ts.Function;
import de.mkbauer.tinyscript.ts.FunctionDeclaration;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.IfStatement;
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
	public String caseFunction(Function function) {
		if (function.getId() != null)
			return "function_" + function.getId().getName();
		else
			return "function_" + String.valueOf(function.hashCode());
	}

	@Override
	public String caseIfStatement(IfStatement object) {
		return "if_" + String.valueOf(object.hashCode());
	}

	@Override
	public String caseElseStatement(ElseStatement object) {
		return "else_" + String.valueOf(object.hashCode());
	}

	@Override
	public String caseForEachStatement(ForEachStatement object) {
		return "foreach_" + String.valueOf(object.hashCode());
	}

	@Override
	public String caseBlockStatement(BlockStatement object) {
		return "block_" + String.valueOf(object.hashCode());
	}	
	
	@Override
	public String caseIdentifier(Identifier identifier) {
		EObject parent = identifier.eContainer();
		if (parent instanceof Function) {
			// Let's ignore the function identifier
			Function function = (Function) (parent);
			if ( identifier == function.getId() ) {
				return null;
			}
		}
		return identifier.getName();
	}

}
