package org.sciserver.springapp.racm.login;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Trustee {
	@JsonProperty("username")
	String userName;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
}
