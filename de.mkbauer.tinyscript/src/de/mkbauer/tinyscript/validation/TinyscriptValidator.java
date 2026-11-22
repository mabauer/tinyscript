package de.mkbauer.tinyscript.validation;

import de.mkbauer.tinyscript.Operators;
import de.mkbauer.tinyscript.TinyscriptModelUtil;
import de.mkbauer.tinyscript.ts.ArrayInitializer;
import de.mkbauer.tinyscript.ts.ArrowFunction;
import de.mkbauer.tinyscript.ts.BinaryExpression;
import de.mkbauer.tinyscript.ts.BinaryOperator;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.BreakStatement;
import de.mkbauer.tinyscript.ts.CallOrPropertyAccess;
import de.mkbauer.tinyscript.ts.CallOrPropertyAccessSuffix;
import de.mkbauer.tinyscript.ts.CallSuffix;
import de.mkbauer.tinyscript.ts.ClassDeclaration;
import de.mkbauer.tinyscript.ts.ClassMethod;
import de.mkbauer.tinyscript.ts.ComputedPropertyAccessSuffix;
import de.mkbauer.tinyscript.ts.ContinueStatement;
import de.mkbauer.tinyscript.ts.DotPropertyAccessSuffix;
import de.mkbauer.tinyscript.ts.Expression;
import de.mkbauer.tinyscript.ts.ExpressionStatement;
import de.mkbauer.tinyscript.ts.ForStatement;
import de.mkbauer.tinyscript.ts.FunctionDeclaration;
import de.mkbauer.tinyscript.ts.FunctionDefinition;
import de.mkbauer.tinyscript.ts.GroupingExpression;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.NewExpression;
import de.mkbauer.tinyscript.ts.NumberLiteral;
import de.mkbauer.tinyscript.ts.ObjectInitializer;
import de.mkbauer.tinyscript.ts.Pattern;
import de.mkbauer.tinyscript.ts.PropertyAssignment;
import de.mkbauer.tinyscript.ts.ImportStatement;
import de.mkbauer.tinyscript.ts.Reference;
import de.mkbauer.tinyscript.ts.ReturnStatement;
import de.mkbauer.tinyscript.ts.Statement;
import de.mkbauer.tinyscript.ts.StringLiteral;
import de.mkbauer.tinyscript.ts.Tinyscript;
import de.mkbauer.tinyscript.ts.TsPackage;
import de.mkbauer.tinyscript.ts.Unary;
import de.mkbauer.tinyscript.ts.UnaryOperator;
import de.mkbauer.tinyscript.ts.VariableStatement;
import de.mkbauer.tinyscript.ts.WhileStatement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.Check;

/**
 * Validation rules for Tinyscript.
 * 
 * see https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 */
@SuppressWarnings("all")
public class TinyscriptValidator extends AbstractTinyscriptValidator {
	
	/**
	 * Checks if a variable declaration conflicts with other declared identifiers of the same name in the same block
	 * @param identifier
	 */
	@Check
	public void checkVariableDoesNotCauseConflictInBlock(Identifier identifier) {
		if (TinyscriptModelUtil.isPartOfVariableStatement(identifier) || identifier.eContainer() instanceof ClassDeclaration) {
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
	
	@Check
	public void checkReturnInvalidInGlobal(ReturnStatement retStmt) {
		if (TinyscriptModelUtil.containingFunction(retStmt) instanceof Tinyscript) {
			error("Return only allowed in functions", null);
		}
	}

	/**
	 * new Num(…) always gives the new object: a constructor cannot return a value (a plain
	 * return; is fine).
	 */
	@Check
	public void checkConstructorReturnsNoValue(ReturnStatement retStmt) {
		if (retStmt.getExpr() != null && TinyscriptModelUtil.containingFunction(retStmt) instanceof FunctionDefinition function
				&& function.eContainer() instanceof ClassMethod method && isConstructor(method))
			error("A constructor cannot return a value: new " + className(method) + "(…) always gives the new object",
					TsPackage.Literals.RETURN_STATEMENT__EXPR);
	}

	/**
	 * A class only at the top level of a file, each method only once.
	 */
	@Check
	public void checkClassDeclaration(ClassDeclaration declaration) {
		if (!isTopLevel(declaration))
			error("A class can only be declared at the top level of a file, not in a block or function",
					TsPackage.Literals.CLASS_DECLARATION__ID);
		Set<String> names = new HashSet<String>();
		for (ClassMethod method : declaration.getMethods()) {
			String name = TinyscriptModelUtil.propertyKey(method.getName());
			if (name != null && !names.add(name))
				error(isConstructor(method) ? "A class has only one constructor"
						: "Duplicate method '" + name + "' in class " + declaration.getId().getName(),
						method, TsPackage.Literals.CLASS_METHOD__NAME);
		}
	}

	private static boolean isConstructor(ClassMethod method) {
		return "constructor".equals(TinyscriptModelUtil.propertyKey(method.getName()));
	}

	private static String className(ClassMethod method) {
		return ((ClassDeclaration) method.eContainer()).getId().getName();
	}

	/**
	 * Checks that break and continue statements are inside a loop of the same function
	 * (they cannot leave a function body).
	 */
	@Check
	public void checkBreakOnlyInsideLoop(BreakStatement breakStmt) {
		if (!isInsideLoop(breakStmt))
			error("Break only allowed inside a loop", null);
	}

	@Check
	public void checkContinueOnlyInsideLoop(ContinueStatement continueStmt) {
		if (!isInsideLoop(continueStmt))
			error("Continue only allowed inside a loop", null);
	}

	private static boolean isInsideLoop(EObject statement) {
		for (EObject container = statement.eContainer(); container != null; container = container.eContainer()) {
			if (container instanceof ForStatement || container instanceof WhileStatement)
				return true;
			if (container instanceof FunctionDefinition)
				return false;
		}
		return false;
	}

	/**
	 * Only variables and properties can be assigned to (x = 1, o.p += 1, a[i]++),
	 * not e.g. literals or calls.
	 */
	@Check
	public void checkAssignmentTarget(BinaryExpression assignment) {
		if (isAssignment(assignment) && !(assignment.getLeft() instanceof Identifier) && !(assignment.getLeft() instanceof Pattern)
				&& !isAssignable(assignment.getLeft()))
			error("Invalid left-hand side of an assignment", TsPackage.Literals.BINARY_EXPRESSION__LEFT);
	}

	@Check
	public void checkIncrementTarget(ExpressionStatement statement) {
		if (Operators.increment(statement) != null && !isAssignable(statement.getExpr()))
			error("'" + Operators.increment(statement).getLiteral() + "' needs a variable or a property", TsPackage.Literals.EXPRESSION_STATEMENT__EXPR);
	}

	private static boolean isAssignment(BinaryExpression expression) {
		return Operators.isAssignment(expression.getOp());
	}

	private static boolean isAssignable(Expression expression) {
		if (expression instanceof Reference reference)
			return !reference.isThis();
		return expression instanceof CallOrPropertyAccess access
				&& access.getSuffix() instanceof CallOrPropertyAccessSuffix suffix && suffix.getCall() == null
				&& !isOptionalChain(access);
	}

	// Whether a chain like a?.b.c contains an optional access (?.)
	private static boolean isOptionalChain(CallOrPropertyAccess access) {
		for (EObject part = access; part instanceof CallOrPropertyAccess chain; part = chain.getExpr()) {
			if (chain.getSuffix() instanceof CallOrPropertyAccessSuffix suffix
					&& (suffix.getProperty() instanceof DotPropertyAccessSuffix dot && dot.isOptional()
						|| suffix.getProperty() instanceof ComputedPropertyAccessSuffix computed && computed.isOptional()))
				return true;
		}
		return false;
	}

	/**
	 * A constant needs a value (const x = 1;) and cannot be assigned again: not with =, +=
	 * etc., ++ / --, or as the existing variable of a for loop.
	 */
	@Check
	public void checkConstantHasAValue(VariableStatement statement) {
		if (!isConstantDeclaration(statement.getKind()))
			return;
		for (Expression declaration : statement.getVardecls()) {
			if (declaration instanceof Identifier identifier)
				error("A constant needs a value: const " + identifier.getName() + " = ...;", identifier, TsPackage.Literals.IDENTIFIER__NAME);
		}
	}

	@Check
	public void checkNoAssignmentToConstant(BinaryExpression assignment) {
		if (isAssignment(assignment) && isConstant(assignment.getLeft()))
			error("Cannot assign to the " + constantKind(assignment.getLeft()) + " " + name(assignment.getLeft()), TsPackage.Literals.BINARY_EXPRESSION__LEFT);
	}

	@Check
	public void checkNoAssignmentToConstant(ExpressionStatement statement) {
		if (Operators.increment(statement) != null && isConstant(statement.getExpr()))
			error("Cannot assign to the " + constantKind(statement.getExpr()) + " " + name(statement.getExpr()), TsPackage.Literals.EXPRESSION_STATEMENT__EXPR);
	}

	@Check
	public void checkNoAssignmentToConstant(ForStatement forStatement) {
		if (forStatement.getRef() != null && isConstant(forStatement.getRef()))
			error("Cannot assign to the " + constantKind(forStatement.getRef()) + " " + name(forStatement.getRef()), TsPackage.Literals.FOR_STATEMENT__REF);
	}

	// "class" for the name of a class, otherwise "constant"
	private static String constantKind(Expression reference) {
		return ((Reference) reference).getId().eContainer() instanceof ClassDeclaration ? "class" : "constant";
	}

	private static boolean isConstantDeclaration(String kind) {
		return "const".equals(kind);
	}

	// Whether expression refers to a variable declared with const (also in a pattern or a for loop)
	private static boolean isConstant(Expression expression) {
		if (!(expression instanceof Reference reference) || reference.getId() == null)
			return false;
		// The name of a class cannot be assigned either
		if (reference.getId().eContainer() instanceof ClassDeclaration)
			return true;
		for (EObject container = reference.getId().eContainer(); container != null; container = container.eContainer()) {
			if (container instanceof VariableStatement statement)
				return isConstantDeclaration(statement.getKind());
			if (container instanceof ForStatement loop)
				return isConstantDeclaration(loop.getKind());
			if (!(container instanceof Pattern) && !(container instanceof BinaryExpression))
				return false;
		}
		return false;
	}

	private static String name(Expression reference) {
		return ((Reference) reference).getId().getName();
	}

	/**
	 * The shorthand { x } needs a variable; { this } is not allowed (as in Javascript).
	 */
	/**
	 * Outside a function, `this` is undefined (as in a Javascript module); an arrow function
	 * takes it from where it is defined, so at top level it is undefined there as well.
	 * Writing it there is a mistake, e.g. { total: () => this.sum } in an object literal.
	 */
	@Check
	public void checkThisInsideAFunction(Reference reference) {
		if (!reference.isThis())
			return;
		for (EObject container = reference.eContainer(); container != null; container = container.eContainer()) {
			if (container instanceof FunctionDefinition && !(container instanceof ArrowFunction))
				return;
		}
		error("'this' is undefined outside a function (an arrow function takes 'this' from where it is written)", null);
	}

	/**
	 * delete only removes a property: delete o.key or delete o[key] (not a variable, not a?.b).
	 */
	@Check
	public void checkDeleteOperand(Unary unary) {
		if (unary.getOp() != UnaryOperator.DELETE)
			return;
		if (!(unary.getExpr() instanceof CallOrPropertyAccess access)
				|| !(access.getSuffix() instanceof CallOrPropertyAccessSuffix suffix) || suffix.getCall() != null
				|| isOptional(suffix))
			error("delete needs a property: delete o.key or delete o[key]", null);
	}

	private static boolean isOptional(CallOrPropertyAccessSuffix suffix) {
		return suffix.getProperty() instanceof DotPropertyAccessSuffix dot && dot.isOptional()
				|| suffix.getProperty() instanceof ComputedPropertyAccessSuffix computed && computed.isOptional();
	}

	@Check
	public void checkShorthandProperty(PropertyAssignment property) {
		if (property.getShorthand() != null && property.getShorthand().isThis())
			error("this needs a key: { self: this }", TsPackage.Literals.PROPERTY_ASSIGNMENT__SHORTHAND);
	}

	/**
	 * Each parameter name only once, also across patterns: function f([a, b], a) is an error
	 */
	@Check
	public void checkParameterNamesAreUnique(FunctionDefinition function) {
		Set<String> names = new HashSet<String>();
		for (Identifier name : TinyscriptModelUtil.parameterIdentifiers(function)) {
			if (!names.add(name.getName()))
				error("Duplicate parameter", name, TsPackage.Literals.IDENTIFIER__NAME);
		}
	}

	/**
	 * Destructuring patterns: each name only once (let [a, b] = pair; for (const [key, value] : map)).
	 */
	@Check
	public void checkPattern(Pattern pattern) {
		Set<String> names = new HashSet<String>();
		for (Identifier name : pattern.getNames()) {
			if (!names.add(name.getName()))
				error("Duplicate identifier", name, TsPackage.Literals.IDENTIFIER__NAME);
		}
	}

	/**
	 * Like Javascript, a ?? b cannot be mixed with && or || without parentheses
	 * (a ?? b || c is ambiguous).
	 */
	@Check
	public void checkNullishCoalescingNotMixedWithAndOr(BinaryExpression expression) {
		if (expression.getOp() == BinaryOperator.NULLISH && (isAndOr(expression.getLeft()) || isAndOr(expression.getRight())))
			error("Use parentheses to combine ?? with && or ||", null);
	}

	/**
	 * && and || give true or false (as conditions do), not one of their operands as in
	 * Javascript. So const cfg = options || {}; is always true: an object, array, function or
	 * new ... as operand is always true, and a number or string is a TypeError at runtime.
	 */
	@Check
	public void checkAndOrOperandsCanBeConditions(BinaryExpression expression) {
		if (!isAndOr(expression))
			return;
		boolean or = expression.getOp() == BinaryOperator.OR;
		String message = or
				? "'||' gives true or false, not one of its operands: for a default value use ?? (e.g. options ?? {})"
				: "'&&' gives true or false, not one of its operands: to read a property of an object that may be null use ?. (e.g. user?.name)";
		if (isNeverACondition(expression.getLeft()))
			error(message, TsPackage.Literals.BINARY_EXPRESSION__LEFT);
		else if (isNeverACondition(expression.getRight()))
			error(message, TsPackage.Literals.BINARY_EXPRESSION__RIGHT);
	}

	// An operand that is always true (an object) or a TypeError in a condition (a number, a string)
	private static boolean isNeverACondition(Expression expression) {
		while (expression instanceof GroupingExpression grouping)
			expression = grouping.getExpr();
		// new Map() is the call of new Map
		if (expression instanceof CallOrPropertyAccess call && call.getSuffix() instanceof CallSuffix
				&& call.getExpr() instanceof NewExpression)
			return true;
		return expression instanceof ObjectInitializer || expression instanceof ArrayInitializer
				|| expression instanceof FunctionDefinition || expression instanceof NewExpression
				|| expression instanceof NumberLiteral || expression instanceof StringLiteral
				|| expression instanceof Unary unary && unary.getOp() == UnaryOperator.NEGATE;
	}

	private static boolean isAndOr(Expression expression) {
		return expression instanceof BinaryExpression binary && (binary.getOp() == BinaryOperator.AND || binary.getOp() == BinaryOperator.OR);
	}

	/**
	 * Only top-level functions and variables of a module can be exported.
	 */
	@Check
	public void checkExportOnlyAtTopLevel(FunctionDeclaration function) {
		if (function.isExported() && !isTopLevel(function))
			error("Only top-level functions, variables and classes can be exported", TsPackage.Literals.FUNCTION_DEFINITION__ID);
	}

	@Check
	public void checkExportOnlyAtTopLevel(VariableStatement variables) {
		if (variables.isExported() && !isTopLevel(variables))
			error("Only top-level functions, variables and classes can be exported", null);
	}

	private static boolean isTopLevel(EObject declaration) {
		Block block = TinyscriptModelUtil.containingBlock(declaration);
		return block != null && block.eContainer() instanceof Tinyscript;
	}

	/**
	 * Imported names must be unique and must not clash with top-level declarations.
	 */
	@Check
	public void checkImportedNamesAreUnique(Tinyscript script) {
		Set<String> names = new HashSet<String>();
		for (Identifier id : TinyscriptModelUtil.declaredVariablesInBlock(script.getGlobal()))
			names.add(id.getName());
		for (FunctionDefinition function : TinyscriptModelUtil.functionDeclarationsInBlock(script.getGlobal()))
			names.add(function.getId().getName());
		Set<String> imported = new HashSet<String>();
		for (Identifier id : TinyscriptModelUtil.importedIdentifiers(script)) {
			if (!imported.add(id.getName()) || names.contains(id.getName()))
				error("Duplicate identifier", id, TsPackage.Literals.IDENTIFIER__NAME);
		}
	}

	/**
	 * Imported names are read-only.
	 */
	@Check
	public void checkNoAssignmentToImport(BinaryExpression assignment) {
		if (isAssignment(assignment) && isImported(assignment.getLeft()))
			error("Cannot assign to an imported name", TsPackage.Literals.BINARY_EXPRESSION__LEFT);
	}

	@Check
	public void checkNoAssignmentToImport(ExpressionStatement statement) {
		if (Operators.increment(statement) != null && isImported(statement.getExpr()))
			error("Cannot assign to an imported name", TsPackage.Literals.EXPRESSION_STATEMENT__EXPR);
	}

	@Check
	public void checkNoAssignmentToImport(ForStatement forStatement) {
		if (forStatement.getRef() != null && isImported(forStatement.getRef()))
			error("Cannot assign to an imported name", TsPackage.Literals.FOR_STATEMENT__REF);
	}

	private static boolean isImported(EObject expression) {
		return expression instanceof Reference reference && reference.getId() != null
				&& reference.getId().eContainer() instanceof ImportStatement;
	}

}
