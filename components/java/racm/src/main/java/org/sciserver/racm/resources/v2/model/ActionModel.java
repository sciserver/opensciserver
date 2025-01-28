package org.sciserver.racm.resources.v2.model;

import edu.jhu.rac.ActionCategory;

public class ActionModel {
	private final String name;
	private final String description;
	private final ActionCategory category;
	public ActionModel(String name, String description, ActionCategory category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}
	public String getName() {
		return name;
	}
	public String getDescription() {
		return description;
	}
	public ActionCategory getCategory() {
		return category;
	}
}
