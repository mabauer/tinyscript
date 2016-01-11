package de.mkbauer.tinyscript.validation;

import de.mkbauer.tinyscript.TinyscriptModelUtil;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.FunctionDefinition;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.Statement;
import de.mkbauer.tinyscript.ts.Tinyscript;
import de.mkbauer.tinyscript.ts.TsPackage;
import de.mkbauer.tinyscript.ts.VariableStatement;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.Check;

/**
 * Validation rules for Tinyscript.
 * 
 * see http://www.eclipse.org/Xtext/documentation.html#validation
 */
@SuppressWarnings("all")
public class TinyscriptJavaValidator extends AbstractTinyscriptJavaValidator {
	
	/**
	 * Checks for already declared identifiers of the same name in the same block
	 * @param identifier
	 */
	@Check
	public void checkNoDuplicateIdentifiersInBlock(Identifier identifier) {
		if (isPartOfVariableStatement(identifier)) {
			// TODO: Replace with TinyscriptModelUtil.blockContainsConflictingIdentifiers()
			Block block = TinyscriptModelUtil.containingBlock(identifier);
			long countDuplicates = TinyscriptModelUtil.declaredVariablesInBlockBefore(block, identifier).stream()
				.filter(i->i.getName().equals(identifier.getName()))
				.count();
			// TODO: Also consider function declarations in the same block and (maybe?) the formal parameters of the enclosing function
			if (countDuplicates > 0) {
				error("Duplicate identifier", TsPackage.Literals.IDENTIFIER__NAME);
			}
		}
		// TODO: Check for duplicates in the names of formal parameters of a function...
	}
	
	@Check 
	void checkNoDuplicateIdentifiersInGlobalScope(Identifier identifier) {
		Block block = TinyscriptModelUtil.containingBlock(identifier);
		if (block.eContainer() instanceof Tinyscript) {
			// TODO: ...
		}
	}
	
	/*
	@Check
	public void checkReservedIdentifier(Identifier identifier) {
		if (identifier.getName().equals("reserved")) {
			error("Reserved identifier", TsPackage.Literals.IDENTIFIER__NAME);
		}
	}
	*/
	
	private boolean isPartOfVariableStatement(Identifier identifier) {
		EObject parent = identifier.eContainer();
		if ((parent == null) || (parent instanceof FunctionDefinition))
			return false;
		Statement stmt = TinyscriptModelUtil.containingStatement(identifier);
		return  ((stmt != null) && (stmt instanceof VariableStatement));
	}
	
}
