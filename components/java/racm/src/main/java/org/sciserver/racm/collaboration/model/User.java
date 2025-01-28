package org.sciserver.racm.collaboration.model;

import java.util.Optional;

import edu.jhu.user.SciserverEntity;

public class User {
	private final long id;
	private final String username;
	private final Optional<String> fullname;
	private final Optional<String> affiliation;

	public User(SciserverEntity entity) {
		if (!(entity instanceof edu.jhu.user.User)) {
			throw new IllegalStateException("Expected " + entity.toString() + " to be a user");
		}
		edu.jhu.user.User input = (edu.jhu.user.User) entity;
		this.id = entity.getId();
		this.username = input.getUsername();
		if (input.getParty() == null) {
			this.fullname = Optional.empty();
			this.affiliation = Optional.empty();
		} else {
			this.fullname = Optional.ofNullable(input.getParty().getFullName());
			this.affiliation = Optional.ofNullable(input.getParty().getAffiliation());
		}
	}

	public long getId() {
		return id;
	}
	public String getUsername() {
		return username;
	}
	public String getFullname() {
		return fullname.orElse(null);
	}
	public String getAffiliation() {
		return affiliation.orElse(null);
	}
}
