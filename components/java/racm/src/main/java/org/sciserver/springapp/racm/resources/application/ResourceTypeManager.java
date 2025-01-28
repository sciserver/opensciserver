package org.sciserver.springapp.racm.resources.application;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.cctree.model.ResourceTypeModel;
import org.sciserver.springapp.racm.utils.RACMNames;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.jhu.rac.Action;
import edu.jhu.rac.ActionCategory;
import edu.jhu.rac.ContextClass;
import edu.jhu.rac.ResourceType;

@Component
public class ResourceTypeManager {
	private VOURPContext vourpContext;

	@Autowired
	public ResourceTypeManager(VOURPContext vourpContext) {
		this.vourpContext = vourpContext;
	}

	public ResourceTypeModel editResourceType(ResourceTypeModel resourceTypeModel) {
		if (resourceTypeModel.getId() == null)
			return createResourceType(resourceTypeModel);
		else
			return updateResourceType(resourceTypeModel);
	}

	private ResourceTypeModel createResourceType(ResourceTypeModel resourceTypeModel) {
		TransientObjectManager tom = vourpContext.newTOM();
		ResourceType rt = null;
		try {
			ContextClass cc = ContextClassManager.queryContextClass(resourceTypeModel.getContextClassId(), tom);
			if (cc == null)
				throw new IllegalStateException("Cannot create new ResourceType when parent ContextClass cannot be found");

			validate(resourceTypeModel, cc, null);
			rt = new ResourceType(cc);
			fill(rt, resourceTypeModel);

			Action a = new Action(rt);
			a.setName(RACMNames.A_GRANT);
			a.setCategory(ActionCategory.G);
			a.setDescription("Grant");

			tom.persist();
			resourceTypeModel = RACMModelFactory.newResourceTypeModel(rt);
			resourceTypeModel.setValid(true);
		} catch (Exception e) {
			resourceTypeModel.setException(e);
			resourceTypeModel.setValid(false);
		}
		return resourceTypeModel;

	}

	private ResourceTypeModel updateResourceType(ResourceTypeModel rtm) {
		return updateResourceType(rtm, null, null);
	}

	public ResourceTypeModel updateResourceType(ResourceTypeModel resourceTypeModel,
			Long roleToBeDeleted, Long actionToBeDeleted) {
		TransientObjectManager tom = vourpContext.newTOM();
		ResourceType rt = null;
		try {
			rt = queryResourceType(resourceTypeModel.getId(), tom);
			if (rt == null)
				throw new IllegalArgumentException(String.format("Cannot change non-existent ResourceType with ID = '%s'",
						resourceTypeModel.getId()));

			ContextClass cc = rt.getContainer();
			validate(resourceTypeModel, cc, rt);
			fill(rt, resourceTypeModel);

			if(roleToBeDeleted != null && roleToBeDeleted > 0) {
				rt.getRole()
					.removeIf(role -> role.getId().equals(roleToBeDeleted));
			}

			if(actionToBeDeleted != null && actionToBeDeleted > 0) {
				rt.getAction()
					.removeIf(action -> action.getId().equals(actionToBeDeleted));
			}

			tom.persist();
			//deleting action requires additional step compared to deleting resource type or role or context class.
			//since deleting a referenced object is database operation in vo-urp generated code.
			//Therefore, query the database to get the updated resource type instance and
			//re-createResourceTypeModel object.
			rt = queryResourceType(resourceTypeModel.getId(), vourpContext.newTOM());
			resourceTypeModel = RACMModelFactory.newResourceTypeModel(rt);
			resourceTypeModel.setValid(true);
		} catch (Exception e) {
			resourceTypeModel = RACMModelFactory.newResourceTypeModel(rt);
			resourceTypeModel.setException(e);
			resourceTypeModel.setValid(false);
		}
		return resourceTypeModel;

	}

	public void deleteResourceType(long id) throws VOURPException{
		TransientObjectManager tom = vourpContext.newTOM();

		ResourceType rt = tom.find(ResourceType.class, id);
		ContextClass cc = rt.getContainer();
		cc.getResourceType().remove(rt);
		tom.persist();

	}

	public ResourceTypeModel getModel(long id) {
		ResourceType rt = queryResourceType(id, vourpContext.newTOM());
		if (rt == null)
			return null;
		return RACMModelFactory.newResourceTypeModel(rt);
	}

	public ResourceType queryResourceType(long rtId, TransientObjectManager tom) {
		if (tom != null) {
			return tom.find(ResourceType.class, rtId);
		}
		return null;
	}

	private void fill(ResourceType rt, ResourceTypeModel rtm){
		rt.setName(rtm.getName().trim());
		rt.setDescription(rtm.getDescription());
	}

	private boolean existResourceType(ContextClass cc, String name) {
		for (ResourceType resourceType : cc.getResourceType()) {
			if (resourceType.getName().equals(name))
				return true;
		}
		return false;
	}

	private void validate(ResourceTypeModel rtm, ContextClass cc, ResourceType rt) {
		String name = rtm.getName();
		if (name == null || "".equals(name.trim())) {
			throw new IllegalArgumentException("A ResourceType MUST have a name");
		}
		name = name.trim();
		if ((rt == null || !name.equals(rt.getName())) && existResourceType(cc, name)) {
			throw new IllegalArgumentException(
					String.format(
						"A ResourceType with name '%s' already exists",	name));
		}
	}

}
