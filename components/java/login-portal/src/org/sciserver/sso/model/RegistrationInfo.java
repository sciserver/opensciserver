/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.sso.model;

public class RegistrationInfo {
	private String username;
	private String password;
	private String confirmPassword;
	private String email;
	private String validationCode;
	private String keycloakUserId;
	private String keycloakUsername;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getConfirmPassword() {
		return confirmPassword;
	}
	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getValidationCode() {
		return validationCode;
	}
	public void setValidationCode(String validationCode) {
		this.validationCode = validationCode;
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
