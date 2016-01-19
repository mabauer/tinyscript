package de.mkbauer.tinyscript;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import de.mkbauer.tinyscript.ts.BinaryExpression;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.ForStatement;
import de.mkbauer.tinyscript.ts.FunctionDefinition;
import de.mkbauer.tinyscript.ts.FunctionDeclaration;
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
			if (e instanceof FunctionDefinition)
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
	public static List<FunctionDefinition> functionDeclarationsInBlock(Block block) {
		List<FunctionDefinition> result = new ArrayList<FunctionDefinition>();
		List<Statement> stmts= block.getStatements();
		Iterator<Statement> it = stmts.iterator();
		while (it.hasNext()) {
			Statement stmt = it.next();
			if (stmt instanceof FunctionDeclaration) {
				result.add((FunctionDeclaration) stmt);
			}
		}
		return result;
	}
	

	
	public static int getLineOfASTNode(EObject elem) {
		return NodeModelUtils.getNode(elem).getStartLine();
	}
	
	public static String getFilenameOfASTNode(EObject elem) {
		return elem.eResource().getURI().toString();
	}

	public static boolean isShadowing(EObject elem) {
		Block block = containingBlock(elem);
		while (block != null) {
			if (blockContainsConflictingIdentifiers(elem, block))
				return true;
			EObject parent = block.eContainer();
			if ((parent instanceof Tinyscript) || (parent instanceof FunctionDeclaration)) 
				break;
			block = containingBlock(parent);
		}
		return false;
	}
	
	public static boolean blockContainsConflictingIdentifiers(EObject elem, Block block) {
		String name = getNameIfNamedElement(elem);
		if (name == null)
			return false;
		long countDuplicates = declaredVariablesInBlock(block).stream()
				.filter(i->(i.getName().equals(name) && i != elem))
				.count();
		if (countDuplicates > 0)
			return true;
		countDuplicates = functionDeclarationsInBlock(block).stream()
				.filter(f->(f.getId().getName().equals(name) && f.getId() != elem))
				.count();
		if (countDuplicates > 0)
			return true;
		if (block.eContainer() instanceof FunctionDefinition) {
			FunctionDefinition function = (FunctionDefinition) block.eContainer();
			countDuplicates = function.getParams().stream()
				.filter(p->p.getName().equals(name))
				.count();
		}
		if (countDuplicates > 0)
			return true;
		if (block.eContainer() instanceof ForStatement) {
			ForStatement forStmt = (ForStatement) block.eContainer();
			if (forStmt.getId().getName().equals(name))
				return true;
		}
		return false;
	}
	
	public static boolean blockContainsConflictingIdentifiersBefore(EObject elem, Block block) {
		String name = getNameIfNamedElement(elem);
		if (name == null)
			return false;
		long countDuplicates = declaredVariablesInBlockBefore(block, elem).stream()
				.filter(i->i.getName().equals(name))
				.count();
		if (countDuplicates > 0)
			return true;
		countDuplicates = functionDeclarationsInBlock(block).stream()
				.filter(f->f.getId().getName().equals(name))
				.count();
		if (countDuplicates > 0)
			return true;
		if (block.eContainer() instanceof FunctionDefinition) {
			FunctionDefinition function = (FunctionDefinition) block.eContainer();
			countDuplicates = function.getParams().stream()
				.filter(p->p.getName().equals(name))
				.count();
		}
		if (countDuplicates > 0)
			return true;
		if (block.eContainer() instanceof ForStatement) {
			ForStatement forStmt = (ForStatement) block.eContainer();
			if (forStmt.getId().getName().equals(name))
				return true;
		}

		return false;
	}
	
	public static String getNameIfNamedElement(EObject elem) {
		String name = null;
		if (elem instanceof FunctionDeclaration) {
			return ((FunctionDeclaration) elem).getId().getName();
		}
		if (elem instanceof Identifier) {
			return ((Identifier) elem).getName();
		}
		return null;
		
	}
	
	public static boolean isPartOfVariableStatement(Identifier identifier) {
		EObject parent = identifier.eContainer();
		if ((parent == null) || (parent instanceof FunctionDefinition))
			return false;
		Statement stmt = TinyscriptModelUtil.containingStatement(identifier);
		return  ((stmt != null) && (stmt instanceof VariableStatement));
	}
	
} 
