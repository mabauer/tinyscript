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
import de.mkbauer.tinyscript.ts.InterfaceDeclaration;
import de.mkbauer.tinyscript.ts.InterfaceMethod;
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
import de.mkbauer.tinyscript.ts.SpreadElement;
import de.mkbauer.tinyscript.ts.StringLiteral;
import de.mkbauer.tinyscript.ts.Tinyscript;
import de.mkbauer.tinyscript.ts.TsPackage;
import de.mkbauer.tinyscript.ts.Unary;
import de.mkbauer.tinyscript.ts.ArrayPattern;
import de.mkbauer.tinyscript.ts.ThrowStatement;
import de.mkbauer.tinyscript.ts.ConditionalExpression;
import de.mkbauer.tinyscript.ts.AssertStatement;
import de.mkbauer.tinyscript.ts.IfStatement;
import de.mkbauer.tinyscript.ts.UnaryOperator;
import de.mkbauer.tinyscript.ts.VariableStatement;
import de.mkbauer.tinyscript.ts.WhileStatement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.validation.Check;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.mkbauer.tinyscript.runtime.BuiltinSignatures;

/**
 * Validation rules for Tinyscript.
 * 
 * see https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 */
@SuppressWarnings("all")
public class TinyscriptValidator extends AbstractTinyscriptValidator {

	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;

	// The members and signatures of the builtins, from the runtime
	private BuiltinSignatures signatures() {
		return BuiltinSignatures.get(resourceSetProvider::get);
	}

	/**
	 * A member of a builtin that does not exist (Math.sqr(2), Math.PIE, JSON.stringify) or a
	 * method of a literal that does not exist ("abc".lenght()), with a suggestion; and a call of
	 * a builtin method with too few or too many arguments (Math.sqrt(), JSON.parse(s, f),
	 * (1).toFixed(1, 2)). Reading an unknown member of a builtin gives undefined at run time, but
	 * is always a mistake; adding one (Math.TAU = …) is allowed, and then it can be read.
	 */
	@Check
	public void checkBuiltinMembers(CallOrPropertyAccess access) {
		if (!(access.getSuffix() instanceof CallOrPropertyAccessSuffix suffix)
				|| !(suffix.getProperty() instanceof DotPropertyAccessSuffix dot) || dot.isOptional()
				|| dot.getKey() == null || dot.getKey().getName() == null)
			return;
		String name = dot.getKey().getName();
		boolean call = suffix.getCall() != null;
		Expression receiver = access.getExpr();
		while (receiver instanceof GroupingExpression grouping)
			receiver = grouping.getExpr();
		BuiltinSignatures.Members members;
		String what;
		String builtin = null;
		if (receiver instanceof Reference reference && !reference.isThis() && reference.getId() != null
				&& !reference.getId().eIsProxy() && isBuiltin(reference.getId())) {
			builtin = reference.getId().getName();
			members = signatures().global(builtin);
			what = builtin;
		}
		else if (call && literalKind(receiver) != null) {
			String kind = literalKind(receiver);
			members = signatures().literal(kind);
			what = kind.equals(BuiltinSignatures.ARRAY) ? "an array" : "a " + kind;
		}
		else
			return;
		if (members == null)
			return;
		if (!members.has(name)) {
			// typeof Math.TAU asks whether it is there
			if (builtin != null && !call && (isChanged(access) || isAddedInScript(access, builtin, name)
					|| access.eContainer() instanceof Unary unary && unary.getOp() == UnaryOperator.TYPEOF))
				return;
			String suggestion = TinyscriptLinkingDiagnosticMessageProvider.suggestion(name, members.members().keySet());
			error(what + " has no " + (call ? "method" : "member") + " '" + name + "'"
					+ (suggestion != null ? " (did you mean '" + suggestion + "'?)" : ""), null);
			return;
		}
		// A member the script replaces (Math.sqrt = …) may be anything
		if (!call || (builtin != null && isAddedInScript(access, builtin, name)))
			return;
		if (members.signature(name) == null)
			error("'" + name + "' of " + what + " is not a function", null);
		else
			checkBuiltinArguments(name, members.signature(name), suffix.getCall().getArguments());
		// The functions of Math take numbers (MathObject.number)
		if ("Math".equals(builtin)) {
			for (Expression argument : suffix.getCall().getArguments()) {
				String kind = Kinds.kind(argument);
				if (kind != null && kind != Kinds.NUMBER)
					error("Math functions need numbers, but " + operand(argument, kind), argument, null);
			}
		}
	}

	// "number", "string" or "array" for a literal whose methods are known, otherwise null
	private static String literalKind(Expression expression) {
		return expression instanceof NumberLiteral ? BuiltinSignatures.NUMBER
				: expression instanceof StringLiteral ? BuiltinSignatures.STRING
				: expression instanceof ArrayInitializer ? BuiltinSignatures.ARRAY : null;
	}

	// The target of an assignment, ++ / -- or delete
	private static boolean isChanged(CallOrPropertyAccess access) {
		EObject container = access.eContainer();
		return (container instanceof BinaryExpression assignment && isAssignment(assignment) && assignment.getLeft() == access)
				|| (container instanceof ExpressionStatement statement && Operators.increment(statement) != null)
				|| (container instanceof Unary unary && unary.getOp() == UnaryOperator.DELETE);
	}

	// Whether the script assigns builtin.name somewhere (Math.TAU = 2 * Math.PI)
	private static boolean isAddedInScript(CallOrPropertyAccess access, String builtin, String name) {
		Resource resource = access.eResource();
		if (resource == null)
			return false;
		for (var iterator = resource.getAllContents(); iterator.hasNext();) {
			if (iterator.next() instanceof CallOrPropertyAccess other && other != access && isChanged(other)
					&& other.getExpr() instanceof Reference reference && reference.getId() != null && !reference.getId().eIsProxy()
					&& builtin.equals(reference.getId().getName()) && isBuiltin(reference.getId())
					&& other.getSuffix() instanceof CallOrPropertyAccessSuffix suffix && suffix.getProperty() instanceof DotPropertyAccessSuffix dot
					&& dot.getKey() != null && name.equals(dot.getKey().getName()))
				return true;
		}
		return false;
	}

	// The number of arguments of a call of a builtin: at least min, at most max (spread: unknown)
	private void checkBuiltinArguments(String name, BuiltinSignatures.Signature signature, List<Expression> arguments) {
		if (signature == null || arguments.stream().anyMatch(argument -> argument instanceof SpreadElement))
			return;
		int count = arguments.size();
		if (count < signature.min())
			error(name + "() needs " + (signature.min() == signature.max() ? "" : "at least ") + arguments(signature.min())
					+ ", not " + count, null);
		else if (count > signature.max())
			error(name + "() takes " + (signature.max() == 0 ? "no arguments" : "at most " + arguments(signature.max()))
					+ ", not " + count, null);
	}

	private static String arguments(int count) {
		return count == 1 ? "1 argument" : count + " arguments";
	}
	
	/**
	 * Checks if a variable declaration conflicts with other declared identifiers of the same name in the same block
	 * @param identifier
	 */
	@Check
	public void checkVariableDoesNotCauseConflictInBlock(Identifier identifier) {
		if (TinyscriptModelUtil.isPartOfVariableStatement(identifier) || identifier.eContainer() instanceof ClassDeclaration
				|| identifier.eContainer() instanceof InterfaceDeclaration) {
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
	/**
	 * A declaration with the name of a builtin hides it in its scope, which is legal (as in
	 * Javascript) but rarely meant: for (const range : ranges) makes range(n) fail further down.
	 */
	@Check
	public void checkBuiltinIsNotHidden(Identifier identifier) {
		Resource resource = identifier.eResource();
		if (resource == null || resource.getResourceSet() == null || resource.getURI().toString().equals(BUILTINS))
			return;
		Resource builtins = resource.getResourceSet().getResource(URI.createURI(BUILTINS), false);
		if (builtins == null)
			return;
		String name = identifier.getName();
		boolean isBuiltin = builtins.getContents().stream()
				.anyMatch(object -> object instanceof Identifier builtin && name != null && name.equals(builtin.getName()));
		if (isBuiltin)
			warning("'" + name + "' hides the builtin " + name + " here: choose another name", TsPackage.Literals.IDENTIFIER__NAME);
	}

	private static final String BUILTINS = "__builtins.tys";

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

	/**
	 * class Circle implements Shape: a name that is an interface, and for one of the same file,
	 * the class has all its methods (one imported from another file is checked when the class is
	 * declared, see TinyscriptEngine.checkImplements).
	 */
	@Check
	public void checkImplements(ClassDeclaration declaration) {
		if (declaration.getId() == null)
			return;
		String className = declaration.getId().getName();
		Set<String> methods = new HashSet<String>();
		for (ClassMethod method : declaration.getMethods())
			methods.add(TinyscriptModelUtil.propertyKey(method.getName()));
		Set<String> seen = new HashSet<String>();
		for (Reference reference : declaration.getInterfaces()) {
			Identifier id = reference.getId();
			if (id == null || id.eIsProxy()) {
				if (reference.isThis())
					error("A class implements interfaces, not this", reference, null);
				continue;
			}
			if (!seen.add(id.getName()))
				error(className + " implements " + id.getName() + " twice", reference, null);
			EObject container = id.eContainer();
			if (container instanceof InterfaceDeclaration anInterface) {
				List<String> missing = anInterface.getMethods().stream()
						.map(method -> TinyscriptModelUtil.propertyKey(method.getName()))
						.filter(name -> !methods.contains(name))
						.toList();
				if (!missing.isEmpty())
					error(TinyscriptModelUtil.missingMethods(className, id.getName(), missing), reference, null);
			}
			else if (container instanceof ClassDeclaration || isFunctionName(id)
					|| TinyscriptModelUtil.isPartOfVariableStatement(id) || container instanceof FunctionDefinition)
				error(className + " implements " + id.getName() + ", but " + id.getName() + " is "
						+ (container instanceof ClassDeclaration ? "a class" : isFunctionName(id) ? "a function" : "a variable")
						+ ", not an interface", reference, null);
		}
	}

	/**
	 * An interface only at the top level of a file, each method only once, no constructor.
	 */
	@Check
	public void checkInterfaceDeclaration(InterfaceDeclaration declaration) {
		if (!isTopLevel(declaration))
			error("An interface can only be declared at the top level of a file, not in a block or function",
					TsPackage.Literals.INTERFACE_DECLARATION__ID);
		Set<String> names = new HashSet<String>();
		for (InterfaceMethod method : declaration.getMethods()) {
			String name = TinyscriptModelUtil.propertyKey(method.getName());
			if ("constructor".equals(name))
				error("An interface lists methods, not the constructor", method, TsPackage.Literals.INTERFACE_METHOD__NAME);
			else if (name != null && !names.add(name))
				error("Duplicate method '" + name + "' in interface " + declaration.getId().getName(),
						method, TsPackage.Literals.INTERFACE_METHOD__NAME);
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
		if (!isAssignment(assignment) || assignment.getLeft() instanceof Identifier || assignment.getLeft() instanceof Pattern
				|| isAssignable(assignment.getLeft()))
			return;
		// [a, b] = [b, a]; { x, y } = point;
		if (assignment.getLeft() instanceof ArrayInitializer || assignment.getLeft() instanceof ObjectInitializer)
			error("Patterns can only declare variables (const [a, b] = pair;), not assign to existing ones: "
					+ "assign one by one, with a temporary variable to swap", TsPackage.Literals.BINARY_EXPRESSION__LEFT);
		else
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
			error(cannotAssign(assignment.getLeft()), TsPackage.Literals.BINARY_EXPRESSION__LEFT);
	}

	@Check
	public void checkNoAssignmentToConstant(ExpressionStatement statement) {
		if (Operators.increment(statement) != null && isConstant(statement.getExpr()))
			error(cannotAssign(statement.getExpr()), TsPackage.Literals.EXPRESSION_STATEMENT__EXPR);
	}

	@Check
	public void checkNoAssignmentToConstant(ForStatement forStatement) {
		if (forStatement.getRef() != null && isConstant(forStatement.getRef()))
			error(cannotAssign(forStatement.getRef()), TsPackage.Literals.FOR_STATEMENT__REF);
	}

	private static String cannotAssign(Expression reference) {
		String kind = constantKind(reference);
		String message = "Cannot assign to the " + kind + " " + name(reference);
		return kind.equals("function") ? message + " (to replace a function, declare it with let " + name(reference) + " = …)" : message;
	}

	// "builtin", "class", "interface", "function" or "constant", for the message
	private static String constantKind(Expression reference) {
		if (isBuiltin(((Reference) reference).getId()))
			return "builtin";
		EObject declaration = ((Reference) reference).getId().eContainer();
		return declaration instanceof ClassDeclaration ? "class" : declaration instanceof InterfaceDeclaration ? "interface" : isFunctionName(((Reference) reference).getId()) ? "function" : "constant";
	}

	// The name of a function declaration (not one of its parameters)
	private static boolean isFunctionName(Identifier id) {
		return id.eContainer() instanceof FunctionDeclaration declaration && declaration.getId() == id;
	}

	private static boolean isConstantDeclaration(String kind) {
		return "const".equals(kind);
	}

	// Whether expression refers to a variable declared with const (also in a pattern or a for loop)
	private static boolean isConstant(Expression expression) {
		if (!(expression instanceof Reference reference) || reference.getId() == null)
			return false;
		// The name of a class or a builtin (Math, print, Infinity) cannot be assigned either
		if (isBuiltin(reference.getId()) || reference.getId().eContainer() instanceof ClassDeclaration || reference.getId().eContainer() instanceof InterfaceDeclaration
				|| isFunctionName(reference.getId()))
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

	// A name of the builtins (Math, print), not one of the script or of a REPL session
	private static boolean isBuiltin(Identifier id) {
		return id.eResource() != null && BUILTINS.equals(id.eResource().getURI().toString());
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
	 * Too many arguments for a function or class whose parameters are known before the run: a
	 * function declaration, a const with a function, new of a class of the same script. Like the
	 * builtins, more arguments than parameters are an error; fewer are fine (undefined or the
	 * default). Methods (p.len(1, 2)) cannot be checked: the type of p is known only at run time.
	 */
	@Check
	public void checkNumberOfArguments(CallOrPropertyAccess call) {
		List<Expression> arguments;
		Expression callee;
		boolean constructor = call.getExpr() instanceof NewExpression;
		if (call.getSuffix() instanceof CallSuffix suffix) {
			arguments = suffix.getArguments();
			callee = constructor ? ((NewExpression) call.getExpr()).getExpr() : call.getExpr();
		}
		else
			return;
		if (!(callee instanceof Reference reference) || reference.isThis() || reference.getId() == null
				|| reference.getId().eIsProxy() || arguments.stream().anyMatch(a -> a instanceof SpreadElement))
			return;
		Identifier id = reference.getId();
		// A builtin function (parseInt, range, print): its signature from the runtime
		if (isBuiltin(id)) {
			if (!constructor)
				checkBuiltinArguments(id.getName(), signatures().function(id.getName()), arguments);
			return;
		}
		FunctionDefinition function = knownFunction(id, constructor);
		int max;
		if (function != null) {
			if (function.getRest() != null)
				return;
			max = function.getParams().size();
		}
		else if (constructor && id.eContainer() instanceof ClassDeclaration)
			max = 0; // a class without a constructor
		else
			return;
		if (arguments.size() > max) {
			String name = (constructor ? "new " : "") + id.getName();
			error(name + " takes " + (max == 0 ? "no arguments" : max == 1 ? "1 argument" : max + " arguments")
					+ ", not " + arguments.size(), null);
		}
	}

	// The function that a name stands for, if it cannot change: a function declaration, a const
	// with a function expression or arrow function, or (after new) the constructor of a class
	private static FunctionDefinition knownFunction(Identifier id, boolean constructor) {
		EObject container = id.eContainer();
		if (container instanceof FunctionDeclaration declaration && declaration.getId() == id)
			return declaration;
		if (container instanceof ClassDeclaration declaration && declaration.getId() == id) {
			if (!constructor)
				return null;
			return declaration.getMethods().stream()
					.filter(m -> "constructor".equals(TinyscriptModelUtil.propertyKey(m.getName())))
					.map(ClassMethod::getFunction).findFirst().orElse(null);
		}
		if (container instanceof BinaryExpression declaration && declaration.getLeft() == id
				&& declaration.getRight() instanceof FunctionDefinition function && !(function instanceof FunctionDeclaration)
				&& declaration.eContainer() instanceof VariableStatement statement && isConstantDeclaration(statement.getKind()))
			return function;
		return null;
	}

	/**
	 * In a class, this.area = … must not hide the method area(): the next this.area() would fail
	 * far from the cause.
	 */
	@Check
	public void checkFieldDoesNotHideAMethod(BinaryExpression assignment) {
		if (!isAssignment(assignment) || !(assignment.getLeft() instanceof CallOrPropertyAccess access)
				|| !(access.getExpr() instanceof Reference reference) || !reference.isThis()
				|| !(access.getSuffix() instanceof CallOrPropertyAccessSuffix suffix) || suffix.getCall() != null
				|| !(suffix.getProperty() instanceof DotPropertyAccessSuffix dot))
			return;
		String name = TinyscriptModelUtil.propertyKey(dot.getKey());
		ClassDeclaration declaration = enclosingClass(assignment);
		if (name == null || declaration == null || name.equals("constructor"))
			return;
		for (ClassMethod method : declaration.getMethods()) {
			if (name.equals(TinyscriptModelUtil.propertyKey(method.getName())))
				error("this." + name + " hides the method " + name + " of " + declaration.getId().getName()
						+ ": give the field another name", TsPackage.Literals.BINARY_EXPRESSION__LEFT);
		}
	}

	// The class whose method contains the element, where this is its instance: arrow functions
	// keep this, other functions have their own
	private static ClassDeclaration enclosingClass(EObject element) {
		for (EObject container = element.eContainer(); container != null; container = container.eContainer()) {
			if (container instanceof FunctionDefinition && !(container instanceof ArrowFunction))
				return container.eContainer() instanceof ClassMethod method
						&& method.eContainer() instanceof ClassDeclaration declaration ? declaration : null;
		}
		return null;
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
	 * Defaults only at the end: function f(a, b = 1, c = 2), not f(a = 1, b), where a caller
	 * would have to pass undefined to get the default (Javascript allows it).
	 */
	@Check
	public void checkParameterDefaultsAtTheEnd(FunctionDefinition function) {
		Identifier withDefault = null;
		for (Expression param : function.getParams()) {
			boolean hasDefault = param instanceof Identifier name && name.getDefaultValue() != null;
			if (hasDefault && withDefault == null)
				withDefault = (Identifier) param;
			else if (!hasDefault && withDefault != null) {
				error("A parameter without a default cannot follow one with a default (" + withDefault.getName()
						+ "): put the parameters with defaults last", param, null);
				return;
			}
		}
	}

	/**
	 * A default can use the parameters before it, f(a, b = a), not itself or a later one
	 * (f(a = b, b)); in Javascript that is a ReferenceError when the call runs.
	 */
	@Check
	public void checkParameterDefaultUsesOnlyEarlierParameters(Reference reference) {
		Identifier parameter = TinyscriptModelUtil.enclosingParameterDefault(reference);
		if (parameter == null || reference.getId() == null || reference.getId().eIsProxy())
			return;
		FunctionDefinition function = (FunctionDefinition) parameter.eContainer();
		List<Identifier> names = TinyscriptModelUtil.parameterIdentifiers(function);
		int index = names.indexOf(reference.getId());
		if (index >= 0 && index >= names.indexOf(parameter) || reference.getId() == function.getRest())
			error(reference.getId().getName() + " is used before its declaration: the default of "
					+ parameter.getName() + " can only use the parameters before it", null);
	}

	/**
	 * Destructuring patterns: each name only once (let [a, b] = pair; for (const [key, value] : map)).
	 */
	@Check
	public void checkPattern(Pattern pattern) {
		Set<String> names = new HashSet<String>();
		for (Identifier name : TinyscriptModelUtil.patternIdentifiers(pattern)) {
			if (!names.add(name.getName()))
				error("Duplicate identifier", name, TsPackage.Literals.IDENTIFIER__NAME);
		}
	}

	/**
	 * Operators whose operands can never fit, by the syntax alone (Kinds, no type inference): a
	 * literal or the result of an operator of the wrong kind. "a" - 1, [1] + 1, 1 < "2", x -= "a"
	 * fail whenever they run, in both backends, so they are errors before the run.
	 */
	@Check
	public void checkOperandKinds(BinaryExpression expression) {
		BinaryOperator op = expression.getOp();
		BinaryOperator base = Operators.baseOf(op);
		if (base != null) {
			// x -= "a": the right operand of a compound assignment (x itself is a variable)
			if (needsNumbers(base))
				checkNumber(base, expression.getRight());
			return;
		}
		String left = Kinds.kind(expression.getLeft());
		String right = Kinds.kind(expression.getRight());
		if (needsNumbers(op)) {
			if (!checkNumber(op, expression.getLeft()))
				checkNumber(op, expression.getRight());
		}
		else if (op == BinaryOperator.ADD && left != null && right != null && !left.equals(Kinds.STRING) && !right.equals(Kinds.STRING)
				&& !(left.equals(Kinds.NUMBER) && right.equals(Kinds.NUMBER)) && !(left.equals(Kinds.ARRAY) && right.equals(Kinds.ARRAY))) {
			if (left.equals(Kinds.ARRAY) || right.equals(Kinds.ARRAY))
				error("'+' can only add an array to another array or to a string, but "
						+ (left.equals(Kinds.ARRAY) ? operand(expression.getRight(), right) : operand(expression.getLeft(), left)), null);
			else
				error("'+' adds two numbers, two arrays, or a string and a value, but " + operand(expression.getLeft(), left)
						+ " and " + operand(expression.getRight(), right), null);
		}
		else if (isComparison(op)) {
			boolean leftWrong = left != null && !left.equals(Kinds.NUMBER) && !left.equals(Kinds.STRING);
			boolean rightWrong = right != null && !right.equals(Kinds.NUMBER) && !right.equals(Kinds.STRING);
			String compares = "'" + op.getLiteral() + "' compares two numbers or two strings, but ";
			if (leftWrong)
				error(compares + operand(expression.getLeft(), left), null);
			else if (rightWrong)
				error(compares + operand(expression.getRight(), right), null);
			else if (left != null && right != null && !left.equals(right))
				error(compares + operand(expression.getLeft(), left) + " and " + operand(expression.getRight(), right), null);
		}
	}

	@Check
	public void checkOperandKinds(Unary unary) {
		if (unary.getOp() == UnaryOperator.NEGATE) {
			String kind = Kinds.kind(unary.getExpr());
			if (kind != null && kind != Kinds.NUMBER)
				error("'-' needs a number, but " + operand(unary.getExpr(), kind), null);
		}
		if (unary.getOp() == UnaryOperator.NOT)
			checkCondition(unary.getExpr());
	}

	// - * / % and the bitwise operators
	private static boolean needsNumbers(BinaryOperator op) {
		return switch (op) {
			case SUBTRACT, MULTIPLY, DIVIDE, REMAINDER -> true;
			default -> Operators.isBitwise(op);
		};
	}

	private static boolean isComparison(BinaryOperator op) {
		return op == BinaryOperator.LESS || op == BinaryOperator.LESS_OR_EQUAL || op == BinaryOperator.GREATER
				|| op == BinaryOperator.GREATER_OR_EQUAL;
	}

	// An operand of - * / % or a bitwise operator that is not a number; true if reported
	private boolean checkNumber(BinaryOperator op, Expression operand) {
		String kind = Kinds.kind(operand);
		if (kind == null || kind == Kinds.NUMBER)
			return false;
		error("'" + op.getLiteral() + "' needs " + (Operators.isBitwise(op) ? "integers" : "two numbers") + ", but "
				+ operand(operand, kind), operand, null);
		return true;
	}

	// "\"a\" is a string", for messages
	private static String operand(Expression expression, String kind) {
		return TinyscriptModelUtil.sourceText(expression) + " is " + kind;
	}

	/**
	 * A condition (if, while, ?:, !, && / ||, assert) is a boolean, null / undefined or an object:
	 * a number or a string is a TypeError whenever it runs (if (1), while (n - 1)), and = instead
	 * of == (if (x = 2)) is a mistake; to assign in a condition, write extra parentheses.
	 */
	private void checkCondition(Expression condition) {
		if (condition instanceof BinaryExpression assignment && assignment.getOp() == BinaryOperator.ASSIGN) {
			error("'=' assigns a value: to compare, write == (or, to assign here, put the assignment in parentheses)", condition, null);
			return;
		}
		String kind = Kinds.kind(condition);
		if (kind == Kinds.NUMBER || kind == Kinds.STRING) {
			Expression inner = condition;
			while (inner instanceof GroupingExpression grouping)
				inner = grouping.getExpr();
			String text = TinyscriptModelUtil.sourceText(inner);
			error("The condition " + text + " is " + kind + ", not a boolean: compare it, e.g. "
					+ (kind == Kinds.NUMBER ? text + " > 0" : text + " != \"\""), condition, null);
		}
	}

	@Check
	public void checkCondition(IfStatement statement) {
		checkCondition(statement.getCond());
	}

	@Check
	public void checkCondition(WhileStatement statement) {
		checkCondition(statement.getCond());
	}

	@Check
	public void checkCondition(ConditionalExpression expression) {
		checkCondition(expression.getCond());
	}

	@Check
	public void checkCondition(AssertStatement statement) {
		checkCondition(statement.getCond());
	}

	/**
	 * x == NaN is always false (NaN is not equal to itself, as in Javascript), x != NaN always
	 * true; typeof x == "numbr" is always false. Warnings: they run, but are almost always a
	 * mistake.
	 */
	@Check
	public void checkComparisonsThatAreAlwaysTheSame(BinaryExpression expression) {
		if (expression.getOp() != BinaryOperator.EQUALS && expression.getOp() != BinaryOperator.NOT_EQUALS)
			return;
		boolean equals = expression.getOp() == BinaryOperator.EQUALS;
		Expression other = isNaN(expression.getLeft()) ? expression.getRight() : isNaN(expression.getRight()) ? expression.getLeft() : null;
		if (other != null) {
			String text = TinyscriptModelUtil.sourceText(other);
			warning(TinyscriptModelUtil.sourceText(expression) + " is always " + !equals + " (NaN is not equal to itself): use "
					+ (equals ? "" : "!") + "isNaN(" + text + ")", null);
			return;
		}
		String typeName = typeofComparedWith(expression.getLeft(), expression.getRight());
		if (typeName == null)
			typeName = typeofComparedWith(expression.getRight(), expression.getLeft());
		if (typeName != null && !TYPEOF_RESULTS.contains(typeName)) {
			String suggestion = TinyscriptLinkingDiagnosticMessageProvider.suggestion(typeName, TYPEOF_RESULTS);
			warning("typeof never gives \"" + typeName + "\", so this is always " + !equals
					+ (suggestion != null ? " (did you mean \"" + suggestion + "\"?)" : ""), null);
		}
	}

	private static final List<String> TYPEOF_RESULTS = List.of("undefined", "object", "boolean", "number", "string", "function");

	private static boolean isNaN(Expression expression) {
		return expression instanceof Reference reference && !reference.isThis() && reference.getId() != null
				&& !reference.getId().eIsProxy() && "NaN".equals(reference.getId().getName()) && isBuiltin(reference.getId());
	}

	// The string that typeof x is compared with, or null
	private static String typeofComparedWith(Expression typeofSide, Expression other) {
		while (typeofSide instanceof GroupingExpression grouping)
			typeofSide = grouping.getExpr();
		return typeofSide instanceof Unary unary && unary.getOp() == UnaryOperator.TYPEOF && other instanceof StringLiteral literal
				? literal.getValue() : null;
	}

	/**
	 * A class of the script called without new (P() instead of new P()), and new of what is no
	 * constructor where it is known before the run: an arrow function in a const (new f()), a
	 * builtin function that is no constructor (new Math.sqrt(4), new print(), new Number(5)).
	 */
	@Check
	public void checkConstructorCalls(CallOrPropertyAccess call) {
		boolean constructor = call.getExpr() instanceof NewExpression;
		// new Math.sqrt(4) is new Math with the suffix .sqrt(4): a method of a builtin object
		if (constructor && call.getSuffix() instanceof CallOrPropertyAccessSuffix methodCall && methodCall.getCall() != null
				&& methodCall.getProperty() instanceof DotPropertyAccessSuffix method && method.getKey() != null && method.getKey().getName() != null
				&& ((NewExpression) call.getExpr()).getExpr() instanceof Reference object && !object.isThis() && object.getId() != null
				&& !object.getId().eIsProxy() && isBuiltin(object.getId())
				&& signatures().global(object.getId().getName()) instanceof BuiltinSignatures.Members methods
				&& methods.signature(method.getKey().getName()) instanceof BuiltinSignatures.Signature methodSignature
				&& !methodSignature.constructor() && !isAddedInScript(call, object.getId().getName(), method.getKey().getName())) {
			error("'new' needs a class or a constructor function, not the builtin function " + method.getKey().getName(), null);
			return;
		}
		Expression callee;
		if (call.getSuffix() instanceof CallSuffix)
			callee = constructor ? ((NewExpression) call.getExpr()).getExpr() : call.getExpr();
		else
			return;
		if (callee instanceof Reference reference && !reference.isThis() && reference.getId() != null && !reference.getId().eIsProxy()) {
			Identifier id = reference.getId();
			if (!constructor && id.eContainer() instanceof ClassDeclaration declaration && declaration.getId() == id)
				error(id.getName() + " is a class: use new " + id.getName() + "(…)", null);
			else if (constructor && knownFunction(id, false) instanceof ArrowFunction)
				error("'new' needs a class or a constructor function, not an arrow function", null);
			else if (constructor && isBuiltin(id) && signatures().function(id.getName()) instanceof BuiltinSignatures.Signature signature
					&& !signature.constructor())
				error("'new' needs a class or a constructor function, not the builtin function " + id.getName(), null);
		}
	}

	/**
	 * throw takes an Error object: a literal (throw "oops", throw 42, throw { message: … }) is a
	 * TypeError whenever it runs.
	 */
	@Check
	public void checkThrowTakesAnError(ThrowStatement statement) {
		Expression thrown = statement.getExpr();
		while (thrown instanceof GroupingExpression grouping)
			thrown = grouping.getExpr();
		String kind = Kinds.kind(thrown);
		if (thrown instanceof ObjectInitializer || (kind != null && kind != Kinds.OBJECT))
			error("throw needs an Error object, e.g. throw new Error(\"...\"), not " + (kind != null ? kind : Kinds.OBJECT),
					TsPackage.Literals.THROW_STATEMENT__EXPR);
	}

	/**
	 * const [a, b] = [1]; : an array pattern needs at least as many elements as it has names, so
	 * an array literal that is too short fails whenever it runs.
	 */
	@Check
	public void checkArrayPatternLength(BinaryExpression assignment) {
		if (assignment.getOp() != BinaryOperator.ASSIGN || !(assignment.getLeft() instanceof ArrayPattern pattern))
			return;
		Expression value = assignment.getRight();
		while (value instanceof GroupingExpression grouping)
			value = grouping.getExpr();
		if (!(value instanceof ArrayInitializer array) || array.getValues().stream().anyMatch(element -> element instanceof SpreadElement))
			return;
		int needed = pattern.getNames().size();
		if (array.getValues().size() < needed)
			error(TinyscriptModelUtil.sourceText(pattern) + " needs an array with at least " + needed + (needed == 1 ? " element" : " elements")
					+ ", not " + array.getValues().size(), TsPackage.Literals.BINARY_EXPRESSION__RIGHT);
	}

	/**
	 * { a: 1, a: 2 } is allowed (the last one wins), but almost always a mistake: a warning.
	 */
	@Check
	public void checkDuplicateKeys(ObjectInitializer object) {
		Set<String> keys = new HashSet<String>();
		for (PropertyAssignment property : object.getPropertyassignments()) {
			String key = property.getShorthand() != null && property.getShorthand().getId() != null
					? property.getShorthand().getId().getName()
					: property.getKey() != null ? TinyscriptModelUtil.propertyKey(property.getKey()) : null;
			if (key != null && !keys.add(key))
				warning("Duplicate key '" + key + "': only the last one counts", property, null);
		}
	}

	/**
	 * Statements after return, break, continue or throw in the same block never run (function
	 * declarations, which are hoisted, excepted): a warning.
	 */
	@Check
	public void checkUnreachableCode(Block block) {
		List<Statement> statements = block.getStatements();
		for (int i = 0; i < statements.size() - 1; i++) {
			Statement statement = statements.get(i);
			String jump = statement instanceof ReturnStatement ? "return" : statement instanceof BreakStatement ? "break"
					: statement instanceof ContinueStatement ? "continue" : statement instanceof ThrowStatement ? "throw" : null;
			if (jump == null)
				continue;
			for (int j = i + 1; j < statements.size(); j++) {
				if (!(statements.get(j) instanceof FunctionDeclaration)) {
					warning("This code never runs: it comes after " + jump, statements.get(j), null);
					break;
				}
			}
			return;
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
		else {
			checkCondition(expression.getLeft());
			checkCondition(expression.getRight());
		}
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
