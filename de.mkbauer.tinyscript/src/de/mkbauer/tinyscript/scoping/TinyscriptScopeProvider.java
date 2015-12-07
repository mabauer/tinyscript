package de.mkbauer.tinyscript.scoping;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.Scopes;
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider;
import org.eclipse.xtext.scoping.impl.AbstractScopeProvider;

import de.mkbauer.tinyscript.ts.TsPackage;
import de.mkbauer.tinyscript.ts.Variable;
import de.mkbauer.tinyscript.ts.VariableOrMember;

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
		if(context instanceof VariableOrMember
				&& reference == TsPackage.Literals.VARIABLE_OR_MEMBER__VAR){
			// Collect a list of candidates by going through the model
			// EcoreUtil2 provides useful functionality to do that
			// For example searching for all elements within the root Object's tree
			EObject rootElement = EcoreUtil2.getRootContainer(context);
			List<Variable> candidates = EcoreUtil2.getAllContentsOfType(rootElement, Variable.class);
			// Scopes.scopeFor creates IEObjectDescriptions and puts them into an IScope instance
			IScope scope = Scopes.scopeFor(candidates);
			return scope;
		}
		return IScope.NULLSCOPE;
	}



}

