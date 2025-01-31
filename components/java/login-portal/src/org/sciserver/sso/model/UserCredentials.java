package org.sciserver.sso.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserCredentials {
	@JsonProperty("user_id")
	String userId;
	
	@JsonProperty("username")
	String userName;
	
	String email;
	
	String password;

	@JsonProperty("keycloak_user_id")
	String keycloakUserId;
	
	@JsonProperty("keycloak_username")
	String keycloakUsername;
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getKeycloakUserId() {
		return keycloakUserId;
	}
	
	public void setKeycloakUserId(String keycloakUserId) {
		this.keycloakUserId = keycloakUserId;
	}

	public String getKeycloakUsername() {
		return keycloakUsername;
	}
	
	public void setKeycloakUsername(String keycloakUsername) {
		this.keycloakUsername = keycloakUsername;
	}
}
