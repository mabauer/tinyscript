package de.mkbauer.tinyscript.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.IContainer;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider;

import com.google.inject.Inject;

public class TinyscriptIndex {

	  @Inject
	  private ResourceDescriptionsProvider rdp;
	  
	  @Inject
	  private IContainer.Manager cm;
	  
	  public IResourceDescription getResourceDescription(EObject object) {
		  IResourceDescriptions index = rdp.getResourceDescriptions(object.eResource());
		  return index.getResourceDescription(object.eResource().getURI());
	  }
	  
	  public Iterable<IEObjectDescription> getExportedEObjectDescriptions(EObject object) {
		  return getResourceDescription(object).getExportedObjects();
	  }

	
}
