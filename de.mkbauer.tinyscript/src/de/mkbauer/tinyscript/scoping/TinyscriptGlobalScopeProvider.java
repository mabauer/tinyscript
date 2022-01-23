package de.mkbauer.tinyscript.scoping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.ResourceSetReferencingResourceSet;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.ResourceSetGlobalScopeProvider;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import de.mkbauer.tinyscript.ts.Identifier;
import de.mkbauer.tinyscript.ts.TsFactory;

public class TinyscriptGlobalScopeProvider extends
		ResourceSetGlobalScopeProvider {
	
	private static final String BUILTINSFILE = "__builtins.ts";
	
	@Override
	protected IScope getScope(Resource resource, boolean ignoreCase, EClass type,
			Predicate<IEObjectDescription> filter) {
		IScope parent = IScope.NULLSCOPE;
		if (resource == null || resource.getResourceSet() == null)
			return parent;
		final ResourceSet resourceSet = resource.getResourceSet();
		// Usually TinyscriptEngine properly defines builtins and adds them to a resource
		// for cross-referencing. In case this is not there (e.g. Unit tests)
		// we'll create a basic one here.
		addFallBackResourceForBuiltins(resourceSet);
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
	
	private static Identifier createId(String name) {
		TsFactory factory = TsFactory.eINSTANCE;
		Identifier id = factory.createIdentifier();
		id.setName(name);
		return id;
	}
	
	private List<Identifier> createBuiltInIds() {
		List<String> builtins = new ArrayList<String>(List.of("Object", "Function", "Array", "String", "Math", "fs", "System", "print")); 
		List<Identifier> builtinIds = new ArrayList<Identifier>();
		builtinIds = builtins.stream()
				.map(s -> TinyscriptGlobalScopeProvider.createId(s))
				.collect(Collectors.toList()); 
		return builtinIds;
	}
	
	private void addFallBackResourceForBuiltins(ResourceSet resourceSet) {		
		URI builtinsUri = URI.createURI(BUILTINSFILE);
		Resource builtinsResource = resourceSet.getResource(builtinsUri, false);
		if (builtinsResource == null) {
			builtinsResource = resourceSet.createResource(builtinsUri);
			builtinsResource.getContents().addAll(createBuiltInIds());
		} 
	}

}
