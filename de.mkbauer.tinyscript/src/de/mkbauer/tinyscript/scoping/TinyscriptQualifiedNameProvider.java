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


public class TinyscriptQualifiedNameProvider extends IQualifiedNameProvider.AbstractImpl {

	@Inject
	private IQualifiedNameConverter converter = new IQualifiedNameConverter.DefaultImpl();
	
	@Inject
	private IResourceScopeCache cache = IResourceScopeCache.NullImpl.INSTANCE;
	
	@Inject 
	private TinyscriptNameResolver resolver;
		
	@Override
	public QualifiedName getFullyQualifiedName(EObject object) {
		return cache.get(object, object.eResource(), new Provider<QualifiedName>() {

			public QualifiedName get() {
				List<String> components = new ArrayList<String>();
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
