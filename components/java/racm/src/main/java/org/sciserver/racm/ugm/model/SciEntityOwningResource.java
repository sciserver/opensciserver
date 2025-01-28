package org.sciserver.racm.ugm.model;

import org.sciserver.racm.resourcecontext.model.AssociatedSciserverEntityModel;
import org.sciserver.racm.resourcecontext.model.RegisteredResourceModel;

public class SciEntityOwningResource {

	private RegisteredResourceModel resource;
	private AssociatedSciserverEntityModel association; 
	
	public SciEntityOwningResource() {}
	public SciEntityOwningResource(RegisteredResourceModel _resource, AssociatedSciserverEntityModel _association) {
		if(_resource == null || _association == null)
			throw new IllegalArgumentException("SciEntityOwningResource must have non null attributes");
		this.resource = _resource;
		this.association = _association;
	}

	public RegisteredResourceModel getResource() {
		return resource;
	}

	public AssociatedSciserverEntityModel getAssociation() {
		return association;
	}
	public void setResource(RegisteredResourceModel resource) {
		this.resource = resource;
	}
	public void setAssociation(AssociatedSciserverEntityModel association) {
		this.association = association;
	}
}
