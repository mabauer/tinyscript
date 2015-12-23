package de.mkbauer.tinyscript.scoping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.IContainer;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider;

import com.google.common.base.Predicate;
import com.google.inject.Inject;

import de.mkbauer.tinyscript.ts.TsPackage;

public class TinyscriptIndex {

	  @Inject
	  private ResourceDescriptionsProvider rdp;
	  
	  @Inject
	  private IContainer.Manager cm;
	  
	  public IResourceDescriptions getIndex(EObject object) {
		  return rdp.getResourceDescriptions(object.eResource());
	  }
	  
	  public IResourceDescription getResourceDescription(EObject object) {
		  IResourceDescriptions index = getIndex(object);
		  return index.getResourceDescription(object.eResource().getURI());
	  }
	  
	  public Iterable<IEObjectDescription> getExportedEObjectDescriptions(EObject object) {
		  return getResourceDescription(object).getExportedObjects();
	  }
	  
	  public List<IEObjectDescription> getVisibleEObjectDescriptions(EObject object, EClass type, Predicate<IEObjectDescription> predicate) {
		  List<IEObjectDescription> result = new ArrayList<IEObjectDescription>();
		  List<IContainer> containers = cm.getVisibleContainers(getResourceDescription(object), getIndex(object));
		  for (IContainer container : containers) {
			  for (IEObjectDescription obj_descr : container.getExportedObjectsByType(type)) {
				  if (predicate.apply(obj_descr))
					  result.add(obj_descr);
			  }
		  }
		  return result;
	  }
	  
	  public List<IEObjectDescription> getVisibleIdentifiersDescriptions(EObject object, Predicate<IEObjectDescription> predicate) {
		  List<IEObjectDescription> result = getVisibleEObjectDescriptions(object, TsPackage.eINSTANCE.getIdentifier(), 
				  predicate);
		  return result;
	  }

	
}
