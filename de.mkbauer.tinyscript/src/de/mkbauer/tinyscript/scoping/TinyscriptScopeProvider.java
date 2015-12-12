package de.mkbauer.tinyscript.scoping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.Scopes;
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider;
import org.eclipse.xtext.scoping.impl.AbstractScopeProvider;

import de.mkbauer.tinyscript.ts.Function;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.Reference;
import de.mkbauer.tinyscript.ts.TsPackage;

/**
 * This class contains custom scoping description.
 * 
 * see : http://www.eclipse.org/Xtext/documentation.html#scoping
 * on how and when to use it 
 *
 */
public class TinyscriptScopeProvider extends AbstractScopeProvider {

	public IScope getScope(EObject context, EReference reference) {
		// We want to define the Scope for the Element's superElement cross-reference
		if(context instanceof Reference
				&& reference == TsPackage.Literals.REFERENCE__ID){
			// Collect a list of candidates by going through the model
			// EcoreUtil2 provides useful functionality to do that
			// For example searching for all elements within the root Object's tree
			EObject rootElement = EcoreUtil2.getRootContainer(context);
			List<EObject> candidates = new ArrayList<EObject>();
			candidates.addAll(EcoreUtil2.getAllContentsOfType(rootElement, Identifier.class));
			// Scopes.scopeFor creates IEObjectDescriptions and puts them into an IScope instance
			IScope scope = Scopes.scopeFor(candidates);
			return scope;
		}
		return IScope.NULLSCOPE;
	}



}

