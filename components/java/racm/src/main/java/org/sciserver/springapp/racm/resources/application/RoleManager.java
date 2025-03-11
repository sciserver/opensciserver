package org.sciserver.springapp.racm.resources.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.cctree.model.ActionModel;
import org.sciserver.racm.cctree.model.RoleModel;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.jhu.rac.Action;
import edu.jhu.rac.ResourceType;
import edu.jhu.rac.Role;
import edu.jhu.rac.RoleAction;

@Service
public class RoleManager {
	private final ResourceTypeManager rtm;
	private final VOURPContext vourpContext;

	@Autowired
	public RoleManager(ResourceTypeManager rtm, VOURPContext vourpContext) {
		this.rtm = rtm;
		this.vourpContext = vourpContext;
	}

	public org.sciserver.racm.cctree.model.RoleModel editRole(RoleModel roleModel) {
		if (roleModel.getId() == null)
			return createRole(roleModel);
		else
			return updateRole(roleModel);
	}

	public void deleteRole(long id) throws VOURPException{
		TransientObjectManager tom = vourpContext.newTOM();

		Role r = tom.find(Role.class, id);
		ResourceType rt = r.getContainer();
		rt.getRole().remove(r);
		tom.persist();

	}

	private RoleModel createRole(RoleModel roleModel) {
		TransientObjectManager tom = vourpContext.newTOM();
		ResourceType rt = null;
		try {
			rt = rtm.queryResourceType(roleModel.getResourceTypeId(), tom);
			if (rt == null)
				throw new IllegalStateException("Cannot create new Role when parent ResourceType cannot be found");

			validate(roleModel, rt, null);
			Role role = new Role(rt);
			fill(role, roleModel);
			buildRoleActions(roleModel, role);

			tom.persist();
			roleModel = RACMModelFactory.newRoleModel(role);
			roleModel.setValid(true);
		} catch (Exception e) {
			if(rt != null)
  			roleModel = RACMModelFactory.newRoleModel(rt);
			else
				throw new IllegalArgumentException("Illegal argument: try to post a new role for a non-existent resource type");
			roleModel.setException(e);
			roleModel.setValid(false);
		}
		return roleModel;
	}

	private RoleModel updateRole(RoleModel roleModel) {
		TransientObjectManager tom = vourpContext.newTOM();
		Role role = null;
		try {
			role = tom.find(Role.class, roleModel.getId());
			if (role == null)
				throw new IllegalArgumentException(String.format("Cannot change non-existent Role with ID = '%s'", roleModel.getId()));

			ResourceType rt = rtm.queryResourceType(roleModel.getResourceTypeId(), tom);

			validate(roleModel, rt, role);
			fill(role, roleModel);
			buildRoleActions(roleModel, role);

			tom.persist();
			roleModel = RACMModelFactory.newRoleModel(role);
		} catch (Exception e) {
			if(role == null)
				throw new IllegalArgumentException("Trying to update non-existent role");
			roleModel = RACMModelFactory.newRoleModel(role);
			roleModel.setException(e);
		}
		return roleModel;
	}
	/**
	 * Build RoleAction collection of Role.
	 * From RoleModel object update Role's roleAction Collection.
	 *
	 * @param roleModel
	 * @param role
	 * @throws Exception
	 */
	private void buildRoleActions(RoleModel roleModel, Role role) {
		//step 1. build map of assignedActions from RoleModel
		Map<Long, ActionModel> assActions = new HashMap<>();
		if (roleModel.getAssignedActions() != null) {
			for (ActionModel actionModel : roleModel.getAssignedActions())
				assActions.put(actionModel.getId(), actionModel);
		}

		//step 2. Walk through RoleAction collection of Role domain object.
		//2.1 for each roleAction object find if it exists in assActions.
		//2.2 if not remove it from RoleAction collection.
		//2.3 otherwise, remove it from assActions.
		//so the end of loop results in removing all actions not in assActions from RoleAction collection
		//and assActions with roleActions to be added to RoleAction Collection in step 3 below.
		List<RoleAction> actions = role.getAction();
		if (actions != null) {
			int l = actions.size() - 1;
			for (int i = l; i >= 0; i--) {
				RoleAction ra = actions.get(i);
				if (!assActions.containsKey(ra.getAction().getId()))
					actions.remove(ra);
				else
					assActions.remove(ra.getAction().getId());
			}
		}

		//step 3 From the Action collection 'allActions' of ResourceTypr of role object, find each action A in assActions.
		//if A is not found in allActions, A is not a valid action and throws exception.
		//otherwise add A to the RoleAction collection of role.
		ResourceType rt = role.getContainer();
		Map<Long, Action> allActions = new HashMap<>();
		if (rt.getAction() != null) {
			for (Action a : rt.getAction()) {
				allActions.put(a.getId(), a);
			}
			for (Long k : assActions.keySet()) {
				Action a = allActions.get(k);
				if (a == null) {
					throw new IllegalArgumentException("Action with non-existent ID assigned to Role.");
				}
				RoleAction ra = new RoleAction(role);
				ra.setAction(a);
			}
		}

	}

	public RoleModel getModel(long id) {
		Role rt = vourpContext.newTOM().find(Role.class, id);
		if (rt == null)
			return null;
		return RACMModelFactory.newRoleModel(rt);
	}

	private boolean existRole(ResourceType rt, String name) {
		for (Role role : rt.getRole()) {
			if (role.getName().equals(name))
				return true;
		}
		return false;
	}

	private void validate(RoleModel roleModel, ResourceType rt, Role role) {
		String name=roleModel.getName();
		if (null == name || "".equals(name.trim())) {
			throw new IllegalArgumentException("A Role MUST have a name");
		}
		name = name.trim();
		if((role == null || !name.equals(role.getName())) && existRole(rt, name)) {
			throw new IllegalArgumentException(String.format("A Role with name '%s' already exists",
					name));
		}
	}

	private void fill(Role role, RoleModel roleModel){
		role.setName(roleModel.getName().trim());
		role.setDescription(roleModel.getDescription());
	}

}
