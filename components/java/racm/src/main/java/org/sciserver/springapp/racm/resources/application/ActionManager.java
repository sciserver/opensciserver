package org.sciserver.springapp.racm.resources.application;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.cctree.model.ActionModel;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.jhu.rac.Action;
import edu.jhu.rac.ActionCategory;
import edu.jhu.rac.ResourceType;

@Service
public class ActionManager {
	private final ResourceTypeManager rtm;
	private final VOURPContext vourpContext;

	@Autowired
	public ActionManager(ResourceTypeManager rtm, VOURPContext vourpContext) {
		this.rtm = rtm;
		this.vourpContext = vourpContext;
	}

	public ActionModel editAction(ActionModel actionModel) {
		if (actionModel.getId() == null || actionModel.getId() == 0)
			return createAction(actionModel);
		else
			return updateAction(actionModel);
	}

	private ActionModel createAction(ActionModel actionModel) {
		TransientObjectManager tom = vourpContext.newTOM();
		try {
			ResourceType rt = rtm.queryResourceType(actionModel.getResourceTypeId(), tom);
			if (rt == null)
				throw new IllegalStateException("Cannot create new Action when parent ResourceType cannot be found");

			validate(actionModel, rt, null);
			Action action = new Action(rt);
			fill(action, actionModel);

			tom.persist();
			actionModel = RACMModelFactory.newActionModel(action);
			actionModel.setValid(true);
		} catch (Exception e) {
			actionModel.setException(e);
			actionModel.setValid(false);
		}
		return actionModel;
	}

	private ActionModel updateAction(ActionModel actionModel) {
		TransientObjectManager tom = vourpContext.newTOM();
		Action action = null;
		try {
			action = queryAction(actionModel.getId(), tom);
			if (action == null)
				throw new IllegalArgumentException(String.format("Cannot change non-existent Action with ID = '%s'", actionModel.getId()));

			ResourceType rt = action.getContainer();
			validate(actionModel,rt, action);
			fill(action, actionModel);
			tom.persist();

			actionModel = RACMModelFactory.newActionModel(action);
			actionModel.setValid(true);
		} catch (Exception e) {
			actionModel = RACMModelFactory.newActionModel(action);
			actionModel.setException(e);
			actionModel.setValid(false);
		}
		return actionModel;
	}

	public void deleteAction(long id) throws VOURPException{
		TransientObjectManager tom = vourpContext.newTOM();

		Action a = tom.find(Action.class, id);
		ResourceType rt = a.getContainer();
		rt.getAction().remove(a);
		tom.persist();
	}

	public Action queryAction(long actionId, TransientObjectManager tom) {
		if (tom != null) {
				return tom.find(Action.class, actionId);
		}
		return null;
	}
	private boolean existAction(ResourceType rt, String name) {
		for (Action action : rt.getAction()) {
			if (action.getName().equals(name))
				return true;
		}
		return false;
	}

	private void validate(ActionModel am, ResourceType rt, Action action) {
		String name = am.getName();
		if (null == name || "".equals(name.trim())) {
			throw new IllegalArgumentException("An Action MUST have a name");
		}
		name = name.trim();
		if ((action == null || !name.equals(action.getName())) && existAction(rt, name)) {
			throw new IllegalStateException(
					String.format(
						"An Action with name '%s' already exists",	name));
		}
	}

	/**
	 * precondition: name is valid (i.e. not null for example).
	 * @param action
	 * @param actionModel
	 */
	private void fill(Action action, ActionModel actionModel){
		action.setName(actionModel.getName().trim());
		action.setDescription(actionModel.getDescription());
		action.setCategory(ActionCategory.fromValue(actionModel.getCategory()));
	}
}
