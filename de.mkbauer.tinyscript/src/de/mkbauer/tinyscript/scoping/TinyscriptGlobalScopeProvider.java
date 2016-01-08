package de.mkbauer.tinyscript.scoping;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.ResourceSetReferencingResourceSet;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.ResourceSetGlobalScopeProvider;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.TsFactory;

public class TinyscriptGlobalScopeProvider extends
		ResourceSetGlobalScopeProvider {
	
	private static final String builtinsFile = "builtins.ts";
	
	@Override
	protected IScope getScope(Resource resource, boolean ignoreCase, EClass type,
			Predicate<IEObjectDescription> filter) {
		IScope parent = IScope.NULLSCOPE;
		if (resource == null || resource.getResourceSet() == null)
			return parent;
		final ResourceSet resourceSet = resource.getResourceSet();
		addResourceForBuiltins(resourceSet);
		if (resourceSet instanceof ResourceSetReferencingResourceSet) {
			ResourceSetReferencingResourceSet set = (ResourceSetReferencingResourceSet) resourceSet;
			Iterable<ResourceSet> referencedSets = Lists.reverse(set.getReferencedResourceSets());
			for (ResourceSet referencedSet : referencedSets) {
				parent = createScopeWithQualifiedNames(parent, resource, filter, referencedSet, type, ignoreCase);
			}
		}
		IScope result = createScopeWithQualifiedNames(parent, resource, filter, resourceSet, type, ignoreCase);
		return result;
	}
	
	private void addResourceForBuiltins(ResourceSet resourceSet) {		
		URI builtinsUri = URI.createURI(builtinsFile);
		Resource builtinsResource = resourceSet.getResource(builtinsUri, false);
		if (builtinsResource == null) {
			builtinsResource = resourceSet.createResource(builtinsUri);
			try {
				InputStream in = this.getClass().getClassLoader()
                        .getResourceAsStream(builtinsFile);
				builtinsResource.load(in, null);
			}
			catch (IOException e) {
				throw new TinyscriptRuntimeException("Could not load '" + builtinsFile + "'.");
			}
		}
	}

}
