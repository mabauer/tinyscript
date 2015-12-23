package de.mkbauer.tinyscript.scoping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.util.IResourceScopeCache;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.mkbauer.tinyscript.ts.FunctionDeclaration;
import de.mkbauer.tinyscript.ts.Identifier;


public class TinyscriptQualifiedNameProvider extends IQualifiedNameProvider.AbstractImpl {

	@Inject
	private IQualifiedNameConverter converter = new IQualifiedNameConverter.DefaultImpl();
	
	@Inject
	private IResourceScopeCache cache = IResourceScopeCache.NullImpl.INSTANCE;
	
	@Inject 
	private TinyscriptNameResolver resolver;
	
	protected QualifiedName _getFullyQualifiedName(EObject object) {
		if (object instanceof Identifier) {
			Identifier identifier = (Identifier) object;
			EObject parent = object.eContainer();
			if (parent instanceof FunctionDeclaration) {
				FunctionDeclaration function = (FunctionDeclaration) parent;
				if ( identifier == function.getId() ) {
					return getFullyQualifiedName(function).skipLast(1).append(identifier.getName());
				}
			}
		}
		return null;
	}
		
	@Override
	public QualifiedName getFullyQualifiedName(EObject object) {
		return cache.get(object, object.eResource(), new Provider<QualifiedName>() {

			public QualifiedName get() {
				List<String> components = new ArrayList<String>();
				
				// Check, if we can directly compute the name
				QualifiedName fqn = _getFullyQualifiedName(object);
				if (fqn != null)
					return fqn;
				// Fall back to the resolver
				String name = resolver.getName(object);
				if (name == null)
					return null;
				components.add(0, name);
				for (EObject elem = object.eContainer(); elem != null; elem=elem.eContainer()) {
					name = resolver.getName(elem);
					if (name != null) {
						components.add(0, name);
					}
				}
				return converter.toQualifiedName(String.join(".", components));	
			}
			
		});

	}
	
}
