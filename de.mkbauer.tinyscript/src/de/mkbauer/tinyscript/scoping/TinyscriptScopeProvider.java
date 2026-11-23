package de.mkbauer.tinyscript.scoping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.Scopes;
import org.eclipse.xtext.scoping.impl.SimpleLocalScopeProvider;

import com.google.common.base.Predicate;

import de.mkbauer.tinyscript.TinyscriptModelUtil;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.CatchClause;
import de.mkbauer.tinyscript.ts.ForStatement;
import de.mkbauer.tinyscript.ts.FunctionDefinition;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.Reference;
import de.mkbauer.tinyscript.ts.Tinyscript;
import de.mkbauer.tinyscript.ts.TsPackage;

/**
 * Scoping rules for Tinyscript.
 * 
 * see : http://www.eclipse.org/Xtext/documentation.html#scoping
 * on how and when to use it 
 *
 */
public class TinyscriptScopeProvider extends SimpleLocalScopeProvider {

	public IScope getScope(EObject context, EReference reference) {
		// We want to define the Scope for the Reference's Identifier cross-reference
		if (context instanceof Reference
				&& reference == TsPackage.Literals.REFERENCE__ID) {
			Identifier parameter = TinyscriptModelUtil.enclosingParameterDefault(context);
			if (parameter != null)
				return parameterScope((FunctionDefinition) parameter.eContainer());
			return createBlockScope(context, true);
		}
		// For content assist, references sometimes need to be resolved with other contexts.
		if (reference == TsPackage.Literals.REFERENCE__ID) {
			if (context instanceof Tinyscript)
				context = ((Tinyscript) context).getGlobal();
			return createBlockScope(context, false);
		}
		return IScope.NULLSCOPE; // super.getScope(context, reference);
	}

	/**
	 * The scope of a parameter's default: all parameters of the function (the validator reports
	 * a default that uses its own or a later one), then what the function sees.
	 */
	private IScope parameterScope(FunctionDefinition function) {
		List<Identifier> ids = new ArrayList<Identifier>(TinyscriptModelUtil.parameterIdentifiers(function));
		if (function.getRest() != null)
			ids.add(function.getRest());
		return Scopes.scopeFor(ids, outerScope(function));
	}

	// What a function sees around it: its block, or the parameters of the function in whose
	// default it is written, f(a, g = () => a)
	private IScope outerScope(FunctionDefinition function) {
		Identifier parameter = TinyscriptModelUtil.enclosingParameterDefault(function);
		if (parameter != null)
			return parameterScope((FunctionDefinition) parameter.eContainer());
		return createBlockScope(TinyscriptModelUtil.containingBlock(function), false);
	}

	public IScope createScopeFromAllIdentifers(Reference reference) {
		// Collect a list of candidates by going through the model
		// EcoreUtil2 provides useful functionality to do that
		// For example searching for all elements within the root Object's tree
		EObject rootElement = EcoreUtil2.getRootContainer(reference);
		List<EObject> candidates = new ArrayList<EObject>();
		candidates.addAll(EcoreUtil2.getAllContentsOfType(rootElement, Identifier.class));
		// Scopes.scopeFor creates IEObjectDescriptions and puts them into an IScope instance
		IScope scope = Scopes.scopeFor(candidates);
		return scope;
	}
	
	/*
	 * Creates a scope for an element with all identifiers declared in a block.
	 * @param elem	the element
	 * @param before if 'true', only considers declaration until the statement that containing 'elem' 
	 */
	public IScope createBlockScope(EObject elem, boolean before) {
		Block block = TinyscriptModelUtil.containingBlock(elem);
		if (block == null)
			// Outside of all blocks: the global scope, i.e. the builtins and shared resources
			// (see TinyscriptGlobalScopeProvider); never the names of other files
			return getGlobalScope(elem.eResource(), TsPackage.Literals.REFERENCE__ID, new Predicate<IEObjectDescription>() {

				@Override
				public boolean apply(IEObjectDescription description) {
					return (description.getEObjectOrProxy().eResource() != elem.eResource());
				}
				
			});
			// return Scope.NULLSCOPE;
		List<Identifier> ids = null;
		if (before)
			ids = TinyscriptModelUtil.declaredVariablesInBlockBefore(block, elem);
		else
			ids = TinyscriptModelUtil.declaredVariablesInBlock(block);
		
		// Hack: Add identifiers of function declarations to the scope
		List<Identifier> funcdecls = TinyscriptModelUtil.functionDeclarationsInBlock(block).stream()
				.map(f->f.getId())
				.collect(Collectors.toList());
		ids.addAll(funcdecls);
		
		// Does the block implement a function? Add formal parameters to the scope 
		// and collect all identifiers of the parent block to the parent scope
		EObject container = block.eContainer();
		if (container instanceof FunctionDefinition) {
			FunctionDefinition function = (FunctionDefinition) container;
			ids.addAll(TinyscriptModelUtil.parameterIdentifiers(function));
			Identifier rest = function.getRest();
			if (rest != null)
				ids.add(rest);
			return Scopes.scopeFor(ids, outerScope(function));
		}
		// Is the block part of a for statement? Add the (optional) variable declaration to the scope (if present)
		if (container instanceof ForStatement) {
			ForStatement foreach = (ForStatement) container;
			Identifier varId = foreach.getId(); 
			if (varId != null) {
				ids.add(varId);
			}
			if (foreach.getPattern() != null) {
				ids.addAll(foreach.getPattern().getNames());
			}
		}
		// catch (e) { ... }: the error is a variable of the catch block
		if (container instanceof CatchClause handler && handler.getParam() != null) {
			ids.add(handler.getParam());
		}
		// The top level also sees the imported names
		if (container instanceof Tinyscript) {
			ids.addAll(TinyscriptModelUtil.importedIdentifiers((Tinyscript) container));
		}
		EObject parent = container; 
		return Scopes.scopeFor(ids, createBlockScope(parent, true));
	}
	
}

