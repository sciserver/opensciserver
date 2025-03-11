package org.sciserver.racm.ugm.model;

import edu.jhu.user.Party;
import edu.jhu.user.User;

public class UserInfo {
	private Long id;
	private String username;
	private String fullname;
	private String affiliation;

	public UserInfo() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String partyName) {
		this.fullname = partyName;
	}

	public String getAffiliation() {
		return affiliation;
	}

	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}
}
