package org.sciserver.racm.resources.v2.model;

import java.util.List;

public interface ResourceModel {
	long getEntityId();
	public String getResourceUUID();
	String getName();
	String getDescription();
	List<ActionModel> getAllowedActions();
	List<ActionModel> getPossibleActions();
	String getType();
}
