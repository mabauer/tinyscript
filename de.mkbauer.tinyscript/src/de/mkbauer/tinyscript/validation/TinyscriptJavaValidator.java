package de.mkbauer.tinyscript.validation;

import de.mkbauer.tinyscript.TinyscriptModelUtil;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.FunctionDeclaration;
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
	 * Checks if a variable declaration conflicts with other declared identifiers of the same name in the same block
	 * @param identifier
	 */
	@Check
	public void checkVariableDoesNotCauseConflictInBlock(Identifier identifier) {
		if (TinyscriptModelUtil.isPartOfVariableStatement(identifier)) {
			Block block = TinyscriptModelUtil.containingBlock(identifier);
			if (TinyscriptModelUtil.blockContainsConflictingIdentifiersBefore(identifier, block))
				error("Duplicate identifier", TsPackage.Literals.IDENTIFIER__NAME);
		}
	}
	
	/**
	 * Checks if a function *declaration* conflicts with other declared identifiers of the same name in the same block
	 * @param identifier
	 */
	@Check
	public void checkFunctionDeclarationDoesNotCauseConflictInBlock(FunctionDefinition function) {
		if (function instanceof FunctionDeclaration) {
			Identifier identifier = function.getId();
			if (identifier != null) {
				Block block = TinyscriptModelUtil.containingBlock(function);
				if (TinyscriptModelUtil.blockContainsConflictingIdentifiers(identifier, block))
					error("Duplicate identifier", TsPackage.Literals.FUNCTION_DEFINITION__ID);
			}
		}
	}
	
	@Check 
	void checkNoDuplicateIdentifiersInGlobalScope(Identifier identifier) {
		Block block = TinyscriptModelUtil.containingBlock(identifier);
		if (block.eContainer() instanceof Tinyscript) {
			// TODO: ...
		}
	}

	/**
	 * Checks if an identifier conflicts with reserved names 
	 * (such as functions needed for the code generation to Javascript)
	 * @param identifier
	 */
	@Check
	public void checkReservedIdentifier(Identifier identifier) {
		if (identifier.getName().startsWith("__ts")) {
			error("Reserved identifier", TsPackage.Literals.IDENTIFIER__NAME);
		}
	}
	
}
