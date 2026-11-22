package de.mkbauer.tinyscript;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import de.mkbauer.tinyscript.ts.BinaryExpression;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.CatchClause;
import de.mkbauer.tinyscript.ts.ClassDeclaration;
import de.mkbauer.tinyscript.ts.ForStatement;
import de.mkbauer.tinyscript.ts.FunctionDefinition;
import de.mkbauer.tinyscript.ts.FunctionDeclaration;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.Pattern;
import de.mkbauer.tinyscript.ts.PropertyName;
import de.mkbauer.tinyscript.ts.StringLiteral;
import de.mkbauer.tinyscript.ts.ImportStatement;
import de.mkbauer.tinyscript.ts.Statement;
import de.mkbauer.tinyscript.ts.Tinyscript;
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
	/**
	 * The names a module exports: its top-level functions and variables marked with export.
	 */
	public static List<Identifier> exportedIdentifiers(Tinyscript script) {
		List<Identifier> result = new ArrayList<Identifier>();
		for (Statement stmt : script.getGlobal().getStatements()) {
			if (stmt instanceof FunctionDeclaration function && function.isExported())
				result.add(function.getId());
			if (stmt instanceof VariableStatement variables && variables.isExported())
				result.addAll(declaredIdentifiers(variables));
			if (stmt instanceof ClassDeclaration declaration && declaration.isExported())
				result.add(declaration.getId());
		}
		return result;
	}

	/**
	 * The variables a statement declares in its block: those of let / const, or the name of a
	 * class (which, like a const, can be used from its declaration on).
	 */
	public static List<Identifier> declaredIdentifiers(Statement statement) {
		if (statement instanceof VariableStatement variables)
			return declaredIdentifiers(variables);
		if (statement instanceof ClassDeclaration declaration)
			return List.of(declaration.getId());
		return List.of();
	}

	/**
	 * The text of a property name: greet in { greet: … } or greet() { … }, "two words" for a string.
	 */
	public static String propertyKey(PropertyName key) {
		if (key.getName() != null)
			return key.getName();
		if (key.getExpr() instanceof StringLiteral literal)
			return literal.getValue();
		return null;
	}

	/**
	 * The names a script imports (named imports and namespaces).
	 */
	public static List<Identifier> importedIdentifiers(Tinyscript script) {
		List<Identifier> result = new ArrayList<Identifier>();
		for (ImportStatement importStatement : script.getImports()) {
			if (importStatement.getNamespace() != null)
				result.add(importStatement.getNamespace());
			result.addAll(importStatement.getNames());
		}
		return result;
	}

	/**
	 * The variables a let / const statement declares: let a = 1, b; or the names of a pattern, let [a, b] = pair;
	 */
	public static List<Identifier> declaredIdentifiers(VariableStatement statement) {
		List<Identifier> result = new ArrayList<Identifier>();
		for (Expression declaration : statement.getVardecls()) {
			Expression target = declaration instanceof BinaryExpression assignment ? assignment.getLeft() : declaration;
			if (target instanceof Identifier identifier)
				result.add(identifier);
			if (target instanceof Pattern pattern)
				result.addAll(pattern.getNames());
		}
		return result;
	}

	public static List<Identifier> declaredVariablesInBlock(Block block) {
		List<Identifier> result = new ArrayList<Identifier>();
		List<Statement> stmts= block.getStatements();
		Iterator<Statement> stmt_it = stmts.iterator();
		while (stmt_it.hasNext()) {
			Statement stmt = stmt_it.next();
			result.addAll(declaredIdentifiers(stmt));
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
			result.addAll(declaredIdentifiers(stmt));
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

	/**
	 * The column (1-based) where an element starts in its source.
	 */
	public static int getColumnOfASTNode(EObject elem) {
		INode node = NodeModelUtils.getNode(elem);
		String text = node.getRootNode().getText();
		int offset = node.getOffset();
		return offset - (text.lastIndexOf('\n', offset - 1) + 1) + 1;
	}

	/**
	 * The source text of an element for error messages, e.g. "o.lst": whitespace between tokens
	 * reduced to single spaces, shortened to 50 characters.
	 */
	public static String sourceText(EObject elem) {
		INode node = NodeModelUtils.findActualNodeFor(elem);
		if (node == null)
			return "?";
		String text = NodeModelUtils.getTokenText(node);
		return text.length() <= 50 ? text : text.substring(0, 48) + "…";
	}
	
	public static String getFilenameOfASTNode(EObject elem) {
		URI uri = elem.eResource().getURI();
		// Files are shown as paths (not file: URIs) in error messages and stack traces
		return uri.isFile() ? uri.toFileString() : uri.toString();
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
			countDuplicates = parameterIdentifiers(function).stream()
				.filter(p->p.getName().equals(name))
				.count();
		}
		if (countDuplicates > 0)
			return true;
		if (block.eContainer() instanceof ForStatement forStmt) {
			if (loopVariables(forStmt).stream().anyMatch(id -> id.getName().equals(name)))
				return true;
		}
		if (declaresCatchParameter(block, name))
			return true;
		return false;
	}

	/**
	 * The names a function's parameters declare: function f(a, [b, c], { d }) declares a, b, c
	 * and d (without the rest parameter).
	 */
	public static List<Identifier> parameterIdentifiers(FunctionDefinition function) {
		List<Identifier> result = new ArrayList<Identifier>();
		for (Expression param : function.getParams()) {
			if (param instanceof Identifier identifier)
				result.add(identifier);
			if (param instanceof Pattern pattern)
				result.addAll(pattern.getNames());
		}
		return result;
	}

	// Whether block is that of catch (e) { ... } with an error variable of this name
	private static boolean declaresCatchParameter(Block block, String name) {
		return block.eContainer() instanceof CatchClause handler && handler.getParam() != null
				&& handler.getParam().getName().equals(name);
	}

	/**
	 * The variables a for loop declares: for (let x ...), the names of for (const [a, b] : ...),
	 * none for a loop over an existing variable (for (x : ...)).
	 */
	public static List<Identifier> loopVariables(ForStatement loop) {
		if (loop.getId() != null)
			return List.of(loop.getId());
		if (loop.getPattern() != null)
			return loop.getPattern().getNames();
		return List.of();
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
			countDuplicates = parameterIdentifiers(function).stream()
				.filter(p->p.getName().equals(name))
				.count();
		}
		if (countDuplicates > 0)
			return true;
		if (block.eContainer() instanceof ForStatement) {
			ForStatement forStmt = (ForStatement) block.eContainer();
			if (loopVariables(forStmt).stream().anyMatch(id -> id.getName().equals(name)))
				return true;
		}
		if (declaresCatchParameter(block, name))
			return true;

		return false;
	}
	
	public static String getNameIfNamedElement(EObject elem) {
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
