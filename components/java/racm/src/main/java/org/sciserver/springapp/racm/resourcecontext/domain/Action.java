package org.sciserver.springapp.racm.resourcecontext.domain;

public class Action {
	private final String name;
	private final String category;
	public String name() {
		return name;
	}
	public String category() {
		return category;
	}
	public Action(String name, String category) {
		this.name = name;
		this.category = category;
	}
}
