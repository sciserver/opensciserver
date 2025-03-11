package org.sciserver.racm.resourcecontext.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ActionModel {
	private final String name;
	private final ActionCategory category;

	@JsonCreator
	public ActionModel(
			@JsonProperty("name") String name,
			@JsonProperty("category") ActionCategory category) {
		this.name = name;
		this.category = category;
	}

	public String getName() {
		return name;
	}

	public ActionCategory getCategory() {
		return category;
	}

	public enum ActionCategory {
		C, R, U, D, G, X;
	}
}
