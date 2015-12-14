package de.mkbauer.tinyscript;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import de.mkbauer.tinyscript.ts.BinaryExpression;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.Function;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.Reference;
import de.mkbauer.tinyscript.ts.Statement;
import de.mkbauer.tinyscript.ts.Tinyscript;
import de.mkbauer.tinyscript.ts.TsFactory;
import de.mkbauer.tinyscript.ts.VariableStatement;

/**
 * Utility functions for querying the AST of Tinyscript.
 * @author markus.bauer
 *
 */
public class TinyscriptModelUtil {
	
	/**
	 * Returns the enclosing statement for some element.
	 * If the element is a statement itself, it is returned.
	 */
	public static Statement containingStatement(EObject elem) {
		for (EObject e = elem; e != null; e = e.eContainer()) {
			if (e instanceof Statement) 
				if ((e.eContainer() != null) && (e.eContainer() instanceof Block))
					return (Statement) e;		
		}
		return null;
	}

	/**
	 * Returns the enclosing block for some element.
	 */
	public static Block containingBlock(EObject elem) {
		Block block = EcoreUtil2.getContainerOfType(elem, Block.class);
		return block;
	}
	
	/**
	 * Returns the enclosing function for some element.
	 * If the element is a function itself, it is returned.
	 */
	public static EObject containingFunction(EObject elem) {
		for (EObject e = elem; e != null; e = e.eContainer()) {
			if (e instanceof Function)
				return e;
			if (e instanceof Tinyscript)
				return e;
		}
		return null;
	}
	
	/**
	 * Returns all declared variables of some block.
	 * 
	 * @param block
	 * @return
	 */
	public static List<Identifier> declaredVariablesInBlock(Block block) {
		List<Identifier> result = new ArrayList<Identifier>();
		List<Statement> stmts= block.getStatements();
		Iterator<Statement> stmt_it = stmts.iterator();
		while (stmt_it.hasNext()) {
			Statement stmt = stmt_it.next();
			if (stmt instanceof VariableStatement) {
				VariableStatement varstmt = (VariableStatement) stmt;
				Iterator<Expression> expr_it = varstmt.getVardecls().iterator();
				while (expr_it.hasNext()) {
					Expression expr = expr_it.next();
					if (expr instanceof BinaryExpression) {
						expr = ((BinaryExpression) expr).getLeft();
					}
					if (expr instanceof Identifier) {
						result.add((Identifier)expr);
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns all declared variables of some block as identifiers *before* some element.
	 * 
	 * @param block
	 * @param elem Only considers variables before the statement containing the element.
	 * @return
	 */
	public static List<Identifier> declaredVariablesInBlockBefore(Block block, EObject elem) {
		List<Identifier> result = new ArrayList<Identifier>();
		List<Statement> stmts= block.getStatements();
		Iterator<Statement> stmt_it = stmts.iterator();
		while (stmt_it.hasNext()) {
			Statement stmt = stmt_it.next();
			if (stmt == containingStatement(elem))
				return result;
			if (stmt instanceof VariableStatement) {
				VariableStatement varstmt = (VariableStatement) stmt;
				Iterator<Expression> expr_it = varstmt.getVardecls().iterator();
				while (expr_it.hasNext()) {
					Expression expr = expr_it.next();
					if (expr instanceof BinaryExpression) {
						expr = ((BinaryExpression) expr).getLeft();
					}
					if (expr instanceof Identifier) {
						result.add((Identifier)expr);
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns all function declarations in some block.
	 * @param block
	 * @return
	 */
	public static List<Function> functionDeclarationsInBlock(Block block) {
		List<Function> result = new ArrayList<Function>();
		List<Statement> stmts= block.getStatements();
		Iterator<Statement> it = stmts.iterator();
		while (it.hasNext()) {
			Statement stmt = it.next();
			if (stmt instanceof Function) {
				result.add((Function) stmt);
			}
		}
		return result;
	}

} 
