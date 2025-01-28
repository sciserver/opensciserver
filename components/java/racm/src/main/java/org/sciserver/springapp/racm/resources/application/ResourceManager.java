package org.sciserver.springapp.racm.resources.application;

import java.util.List;

import javax.persistence.Query;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.rctree.model.ResourceMVCModel;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.jhu.rac.Resource;
import edu.jhu.rac.ResourceContext;
import edu.jhu.rac.ResourceType;

@Service
public class ResourceManager {
	private final VOURPContext vourpContext;

	@Autowired
	public ResourceManager(VOURPContext vourpContext) {
		this.vourpContext = vourpContext;
	}

	public ResourceMVCModel editResource(UserProfile up, ResourceMVCModel resourceModel) {
		if (resourceModel.getId() == null)
			return createResource(up, resourceModel);
		else
			return updateResource(up, resourceModel);
	}

	private ResourceMVCModel createResource(UserProfile up,  ResourceMVCModel resourceModel) {
		TransientObjectManager tom = up.getTom();
		List<ResourceType> rts=null;
		Resource r = null;
		try {
			rts = RACMModelFactory.queryResourceTypes(resourceModel, tom);

			ResourceContext rc = ResourceContextManager.queryResourceContext(resourceModel.getContainerId(), tom);
			if (rc == null)
				throw new IllegalStateException("Cannot create new Resource when parent ResourceContext cannot be found");

			validate(resourceModel, null);
			r = RACMUtil.newResource(rc);
			fill(r, resourceModel, rts);

			RACMUtil.assignAllPrivileges(r, up.getUser());

			tom.persist();
			resourceModel = RACMModelFactory.newResourceModel(r, rts);
			resourceModel.setValid(true);
		} catch (Exception e) {
			resourceModel.setException(e);
			resourceModel.setValid(false);
		}
		return resourceModel;

	}

	/**
	 * SImple code to update metadata of a resource.<br/>
	 * @param resourceUUID
	 * @param name
	 * @param description
	 * @param tom
	 */
	public void editResourceMetadata(String resourceUUID, String name, String description, TransientObjectManager tom) throws VOURPException{
		if(name == null && description == null)
			return;
		Query q = tom.createQuery("select r from Resource r where r.uuid=:uuid").setParameter("uuid", resourceUUID);
		Resource r = tom.queryOne(q,Resource.class);
		if(r !=null)
			r.setName(name);
		if(description != null)
			r.setDescription(description);
		tom.persist();
	}
	
	private ResourceMVCModel updateResource(UserProfile up, ResourceMVCModel resourceModel) {
		TransientObjectManager tom = up.getTom();
		List<ResourceType> rts = null;
		Resource r = null;
		try {
			rts = RACMModelFactory.queryResourceTypes(resourceModel, tom);
			r = queryResource(resourceModel.getId(), tom);
			if (r == null)
				throw new IllegalArgumentException(String.format("Cannot change non-existent Resource with ID = '%s'",
						resourceModel.getId()));

			validate(resourceModel, r);
			fill(r, resourceModel, rts);

			tom.persist();

			//deleting action requires additional step compared to deleting resource type or role or context class.
			//since deleting a referenced object is database operation in vo-urp generated code.
			//Therefore, query the database to get the updated resource type instance and
			//re-createResourceModel object.
			r = queryResource(resourceModel.getId(), tom);
			resourceModel = RACMModelFactory.newResourceModel(r, rts);
		} catch (Exception e) {
			if (r == null)
				throw new IllegalArgumentException("Trying to update non-existent resource type");
			resourceModel = RACMModelFactory.newResourceModel(r, rts);
			resourceModel.setException(e);
		}
		return resourceModel;
	}

	public void deleteResource(long id) throws VOURPException {
		TransientObjectManager tom = vourpContext.newTOM();

		Resource r = tom.find(Resource.class, id);
		ResourceContext rc = r.getContainer();
		rc.getResource().remove(r);
		tom.persist();
	}

	public ResourceMVCModel getModel(TransientObjectManager tom, long id) {
		Resource r = queryResource(id, vourpContext.newTOM());

		if (r == null)
			return null;
		return RACMModelFactory.newResourceModel(r, tom);
	}

	private Resource queryResource(long rtId, TransientObjectManager tom) {
		if (tom != null) {
			return tom.find(Resource.class, rtId);
		}
		return null;
	}

	private void fill(Resource r, ResourceMVCModel rm, List<ResourceType> rts) {
		r.setPublisherDID(rm.getPublisherDID().trim());
		r.setName(rm.getName());
		r.setDescription(rm.getDescription());
		String rcName = rm.getResourceTypeModel().getName();
		for (ResourceType rt: rts) {
			if (rcName.equals(rt.getName())){
				r.setResourceType(rt);
				break;
			}
		}

		if (null == r.getResourceType())
			throw new IllegalArgumentException("Referred ResourceType " + rcName + " is not found in the database.");
	}

	private void validate(ResourceMVCModel rm, Resource r) {
		String trimeduuid = (null==rm.getUuid()) ? null:rm.getUuid().trim();
		String uuid = (null==trimeduuid || "".equals(trimeduuid))? null:trimeduuid;

		if (null != r){
			if (r.isPurelyTransient() && uuid !=null)
				throw new IllegalArgumentException("Resource's uuid must be set by the system.");

			if (!r.isPurelyTransient() && uuid ==null)
				throw new IllegalArgumentException("Resource must have uuid.");
		}
		if (rm.getResourceTypeModel() == null)
			throw new IllegalArgumentException("A Resource MUST have a resource type");
	}

}
