package org.sciserver.springapp.racm.login;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Auth {
	@JsonProperty("username")
	String userName;
	
	String password;
	
	@JsonProperty("trust_id")
	String trustId;
	
	public String getUser() {
		return userName;
	}
	public void setUser(String user) {
		this.userName = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getTrust() {
		return trustId;
	}
	public void setTrust(String trust) {
		this.trustId = trust;
	}

}
