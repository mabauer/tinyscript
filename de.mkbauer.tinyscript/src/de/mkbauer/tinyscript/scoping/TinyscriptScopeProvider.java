package de.mkbauer.tinyscript.scoping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.Scopes;
import org.eclipse.xtext.scoping.impl.AbstractScopeProvider;
import org.eclipse.xtext.scoping.impl.SimpleLocalScopeProvider;
import org.eclipse.xtext.xbase.typesystem.internal.ExpressionScope.Scope;

import com.google.common.base.Predicate;

import de.mkbauer.tinyscript.TinyscriptModelUtil;
import de.mkbauer.tinyscript.ts.Block;
import de.mkbauer.tinyscript.ts.ForEachStatement;
import de.mkbauer.tinyscript.ts.Function;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.NumericForStatement;
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
			return createBlockScope(context, true);
		}
		// For contentassist, references sometimes need to be resolved with other contexts.
		if (reference == TsPackage.Literals.REFERENCE__ID) {
			if (context instanceof Tinyscript)
				context = ((Tinyscript) context).getGlobal();
			return createBlockScope(context, false);
		}
		return Scope.NULLSCOPE; // super.getScope(context, reference);
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
			// TODO: Provide a better GlobalScopeProvider instead of the default: TypesAwareDefault....; create a replacement by overriding getScope there.
			// Check https://www.eclipse.org/forums/index.php/t/1067727/
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
		if (container instanceof Function) {
			Function function = (Function) container;
			List<Identifier> params = function.getParams();
			ids.addAll(params);
			EObject parent = TinyscriptModelUtil.containingBlock(function);
			return Scopes.scopeFor(ids, createBlockScope(parent, false));
		}
		// Is the block part of a foreach statement? Add the (optional) variable declaration to the scope (if present)
		if (container instanceof ForEachStatement) {
			ForEachStatement foreach = (ForEachStatement) container;
			Identifier varId = foreach.getId(); 
			if (varId != null) {
				ids.add(varId);
			}
		}
		// Is the block part of a numerical for statement? Add the (optional) variable declaration to the scope (if present)
		if (container instanceof NumericForStatement) {
			NumericForStatement numericalfor = (NumericForStatement) container;
			Identifier varId = numericalfor.getId(); 
			if (varId != null) {
				ids.add(varId);
			}
		}
		EObject parent = container; 
		return Scopes.scopeFor(ids, createBlockScope(parent, true));
	}
	
}

